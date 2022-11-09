package com.ojhdtapp.parabox.extension.auto.domain.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxService
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.Profile
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.getContentString
import com.ojhdtapp.parabox.extension.auto.core.util.DataStoreKeys
import com.ojhdtapp.parabox.extension.auto.core.util.NotificationUtil
import com.ojhdtapp.parabox.extension.auto.core.util.dataStore
import com.ojhdtapp.parabox.extension.auto.data.AppDatabase
import com.ojhdtapp.parabox.extension.auto.domain.model.WxContact
import com.ojhdtapp.parabox.extension.auto.domain.util.CustomKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnService : ParaboxService() {
    companion object {
        var connectionType = 0
    }

    @Inject
    lateinit var database: AppDatabase

    var notificationListenerService: MyNotificationListenerService? = null
    lateinit var serviceConnection: ServiceConnection

    var wxSbnMap = mutableMapOf<Long, StatusBarNotification>()

    private fun receiveWXSbn(sbn: StatusBarNotification) {
        val time = sbn.postTime
        Log.d("parabox", "receiveWXSbn: $time")
        val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE, "")
        val content = sbn.notification.extras.getString(Notification.EXTRA_TEXT, "")
        val icon = sbn.notification.extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON)
        if (content.contains("撤回了一条消息")) return
        lifecycleScope.launch(Dispatchers.IO) {
            if (title.isNotBlank()) {
                var contactId: Long? = database.wxContactDao.queryByName(title)?.id
                if (contactId == null) {
                    contactId = database.wxContactDao.insert(WxContact(title))
                }
                contactId?.let { id ->
                    wxSbnMap.put(id, sbn)
                    val processedContent = content.replace("\\[\\d+条\\]".toRegex(), "")
                    val arr = processedContent.split(":".toRegex(), 2)
                    val isGroup = arr.size > 1
                    val profile = Profile(
                        name = if (isGroup) arr.first() else title,
                        avatar = null,
                        id = id
                    )
                    val subjectProfile = Profile(
                        name = title,
                        avatar = null,
                        id = id
                    )
                    receiveMessage(
                        ReceiveMessageDto(
                            contents = listOf(PlainText(arr.last())),
                            profile = profile,
                            subjectProfile = subjectProfile,
                            timestamp = time,
                            messageId = null,
                            pluginConnection = PluginConnection(
                                connectionType = connectionType,
                                sendTargetType = if (isGroup) SendTargetType.GROUP else SendTargetType.USER,
                                id = id
                            )
                        ),
                        onResult = {
                            Log.d("parabox", "receiveMessage result: $it")
                        }
                    )
                }
            }
        }
    }

    private fun enableListener() {
        packageManager.setComponentEnabledSetting(
            ComponentName(baseContext, MyNotificationListenerService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    private fun disableListener() {
        packageManager.setComponentEnabledSetting(
            ComponentName(baseContext, MyNotificationListenerService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when (msg.what) {

        }
    }

    override fun onMainAppLaunch() {
        // Auto Login
        if (getServiceState() == ParaboxKey.STATE_STOP) {
            lifecycleScope.launch {
                val isAutoLoginEnabled =
                    dataStore.data.first()[DataStoreKeys.AUTO_LOGIN] ?: false
                if (isAutoLoginEnabled) {
                    onStartParabox()
                }
            }
        }
    }

    override suspend fun onRecallMessage(messageId: Long): Boolean {
        return false
    }

    override fun onRefreshMessage() {

    }

    override suspend fun onSendMessage(dto: SendMessageDto): Boolean {
        val contentString = dto.contents.getContentString()
        return wxSbnMap.get(dto.pluginConnection.objectId)?.let {
            notificationListenerService?.sendReply(it, contentString)
            true
        } ?: false
    }

    override fun onStartParabox() {
        lifecycleScope.launch {
            // Foreground Service
            val isForegroundServiceEnabled =
                dataStore.data.first()[DataStoreKeys.FOREGROUND_SERVICE] ?: false
            if (isForegroundServiceEnabled) {
                NotificationUtil.startForegroundService(this@ConnService)
            }

            updateServiceState(ParaboxKey.STATE_LOADING, "尝试绑定监听服务")

            val intent = Intent(this@ConnService, MyNotificationListenerService::class.java).apply {
                action = "com.ojhdtapp.parabox.extension.auto.core.ConnService"
            }
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    notificationListenerService =
                        (service as MyNotificationListenerService.NotificationListenerServiceBinder).getService()
                            .also {
                                it.setMessageListener(object : NotificationMessageListener {
                                    override fun onStateChange(state: Int, message: String?) {
                                        updateServiceState(state, message)
                                    }

                                    override fun receiveSbn(sbn: StatusBarNotification) {
                                        when (sbn.packageName) {
                                            "com.tencent.mm" -> {
                                                receiveWXSbn(sbn)
                                            }
                                            else -> {}
                                        }
                                    }
                                })
                            }
                    enableListener()
                    updateServiceState(ParaboxKey.STATE_RUNNING, "绑定监听服务成功")
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    notificationListenerService = null
                    disableListener()
                    updateServiceState(ParaboxKey.STATE_ERROR, "绑定监听服务失败")
                }
            }
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStateUpdate(state: Int, message: String?) {

    }

    override fun onStopParabox() {
        NotificationUtil.stopForegroundService(this)
        unbindService(serviceConnection)
        notificationListenerService = null
        updateServiceState(ParaboxKey.STATE_STOP)
    }

    override fun onCreate() {
        connectionType = packageManager.getApplicationInfo(
            this@ConnService.packageName,
            PackageManager.GET_META_DATA
        ).metaData.getInt("connection_type")
        super.onCreate()
    }

}