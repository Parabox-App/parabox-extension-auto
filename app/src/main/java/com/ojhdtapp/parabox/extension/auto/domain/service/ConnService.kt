package com.ojhdtapp.parabox.extension.auto.domain.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.service.notification.NotificationListenerService
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
import com.ojhdtapp.parabox.extension.auto.domain.util.CustomKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnService : ParaboxService() {
    companion object {
        var connectionType = 0
    }

    var notificationListenerService: MyNotificationListenerService? = null
    lateinit var serviceConnection: ServiceConnection

    private fun receiveTestMessage(msg: Message, metadata: ParaboxMetadata) {
        // TODO 11 : Receive Message
        val content = (msg.obj as Bundle).getString("content") ?: "No content"
        val profile = Profile(
            name = "anonymous",
            avatar = "https://gravatar.loli.net/avatar/d41d8cd98f00b204e9800998ecf8427e?d=mp&v=1.5.1",
            id = 1L
        )
        receiveMessage(
            ReceiveMessageDto(
                contents = listOf(PlainText(text = content)),
                profile = profile,
                subjectProfile = profile,
                timestamp = System.currentTimeMillis(),
                messageId = null,
                pluginConnection = PluginConnection(
                    connectionType = connectionType,
                    sendTargetType = SendTargetType.USER,
                    id = 1L
                )
            ),
            onResult = {
                // TODO 7 : Call sendCommandResponse when the job is done
                if (it is ParaboxResult.Success) {
                    sendCommandResponse(
                        isSuccess = true,
                        metadata = metadata,
                        extra = Bundle().apply {
                            putString(
                                "message",
                                "Message received at ${System.currentTimeMillis()}"
                            )
                        }
                    )
                } else {
                    sendCommandResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = (it as ParaboxResult.Fail).errorCode
                    )
                }
            }
        )
    }

    // TODO 9 : Call sendNotification function with NOTIFICATION_SHOW_TEST_MESSAGE_SNACKBAR
    private fun showTestMessageSnackbar(message: String) {
        sendNotification(CustomKey.NOTIFICATION_SHOW_TEST_MESSAGE_SNACKBAR, Bundle().apply {
            putString("message", message)
        })
    }

    private fun enableListener(){
        packageManager.setComponentEnabledSetting(
            ComponentName(baseContext, MyNotificationListenerService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    private fun disableListener(){
        packageManager.setComponentEnabledSetting(
            ComponentName(baseContext, MyNotificationListenerService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when (msg.what) {
            // TODO 6: Handle custom command
            CustomKey.COMMAND_RECEIVE_TEST_MESSAGE -> {
                receiveTestMessage(msg, metadata)
            }
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
        return true
    }

    override fun onRefreshMessage() {

    }

    override suspend fun onSendMessage(dto: SendMessageDto): Boolean {
        val contentString = dto.contents.getContentString()
        showTestMessageSnackbar(contentString)
        return true
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

            val intent = Intent(this@ConnService, MyNotificationListenerService::class.java)
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    notificationListenerService = (service as MyNotificationListenerService.NotificationListenerServiceBinder).getService().also {
                        it.setMessageListener(object: NotificationMessageListener{
                            override fun onStateChange(state: Int, message: String?) {
                                updateServiceState(state, message)
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
//            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
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