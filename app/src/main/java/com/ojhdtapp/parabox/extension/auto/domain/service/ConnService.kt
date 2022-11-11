package com.ojhdtapp.parabox.extension.auto.domain.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Icon
import android.os.IBinder
import android.os.Message
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxService
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.Profile
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.getContentString
import com.ojhdtapp.parabox.extension.auto.core.util.DataStoreKeys
import com.ojhdtapp.parabox.extension.auto.core.util.FileUtil
import com.ojhdtapp.parabox.extension.auto.core.util.NotificationUtil
import com.ojhdtapp.parabox.extension.auto.core.util.dataStore
import com.ojhdtapp.parabox.extension.auto.data.AppDatabase
import com.ojhdtapp.parabox.extension.auto.domain.model.AppModel
import com.ojhdtapp.parabox.extension.auto.domain.model.WxContact
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
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
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        val content =
            sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        if (content.contains("撤回了一条消息")) return
        val icon = sbn.notification.extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON)
        val bitmap = icon?.loadDrawable(this)?.toBitmap()
        val wxIconBitmap = FileUtil.getAppIcon(baseContext, sbn.packageName)
        val wxIconUri = wxIconBitmap?.let {
            FileUtil.getUriFromBitmap(baseContext, it, "微信").apply {
                grantUriPermission(
                    "com.ojhdtapp.parabox",
                    this,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                grantUriPermission(
                    "com.ojhdtapp.parabox",
                    this,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val appModel: AppModel =
                database.appModelDao.queryByPackageName(sbn.packageName) ?: run {
                    val ai = try {
                        packageManager.getApplicationInfo(sbn.packageName, 0)
                    } catch (e: NameNotFoundException) {
                        null
                    }
                    val appName = ai?.let { packageManager.getApplicationLabel(it).toString() }
                    database.appModelDao.insert(
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                        )
                    ).let {
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                            id = it
                        )
                    }
                }
            if (appModel.disabled) return@launch
            val avatarUri = bitmap?.let {
                FileUtil.getUriFromBitmap(baseContext, it, title).apply {
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            if (title.isNotBlank()) {
                var contactId: Long? = database.wxContactDao.queryByName(title)?.id
                if (contactId == null) {
                    contactId = database.wxContactDao.insert(WxContact(title))
                }
                val id = contactId + 10000
                wxSbnMap.put(id, sbn)
                val processedContent = content.replace("\\[\\d+条\\]".toRegex(), "")
                val arr = processedContent.split(":".toRegex(), 2)
                val isGroup = arr.size > 1
                val profile = Profile(
                    name = if (isGroup) arr.first() else title,
                    avatar = null,
                    id = null,
                    avatarUri = null
                )
                val subjectProfile = Profile(
                    name = title,
                    avatar = null,
                    id = id,
                    avatarUri = avatarUri
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

    private fun receiveQQSbn(sbn: StatusBarNotification) {
        val time = sbn.postTime
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        val processedTitle = title.replace(" \\(\\d+条新消息\\)".toRegex(), "")
        val content =
            sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        if (content.contains("正在语音通话") || content.contains("等待大家加入")) return
        Log.d("parabox", "title:$title, processed: $processedTitle, content:$content")
        val icon = sbn.notification.extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON)
        val bitmap = icon?.loadDrawable(this)?.toBitmap()
        val qqIconBitmap = FileUtil.getAppIcon(baseContext, sbn.packageName)
        val qqIconUri = qqIconBitmap?.let {
            FileUtil.getUriFromBitmap(baseContext, it, "QQ").apply {
                grantUriPermission(
                    "com.ojhdtapp.parabox",
                    this,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                grantUriPermission(
                    "com.ojhdtapp.parabox",
                    this,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val appModel: AppModel =
                database.appModelDao.queryByPackageName(sbn.packageName) ?: run {
                    val ai = try {
                        packageManager.getApplicationInfo(sbn.packageName, 0)
                    } catch (e: NameNotFoundException) {
                        null
                    }
                    val appName = ai?.let { packageManager.getApplicationLabel(it).toString() }
                    database.appModelDao.insert(
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                        )
                    ).let {
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                            id = it
                        )
                    }
                }
            if (appModel.disabled) return@launch
            val avatarUri = bitmap?.let {
                FileUtil.getUriFromBitmap(baseContext, it, processedTitle).apply {
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            if (processedTitle.isNotBlank()) {
                var contactId: Long? = database.wxContactDao.queryByName(processedTitle)?.id
                if (contactId == null) {
                    contactId = database.wxContactDao.insert(WxContact(processedTitle))
                }
                val id = contactId + 10000
                val arr = content.split(":".toRegex(), 2)
                val isGroup = arr.size > 1
                val profile = Profile(
                    name = if (isGroup) arr.first() else processedTitle,
                    avatar = null,
                    id = null,
                    avatarUri = null
                )
                val subjectProfile = Profile(
                    name = processedTitle,
                    avatar = null,
                    id = id,
                    avatarUri = avatarUri
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

    private fun receiveConversationSbn(sbn: StatusBarNotification) {
        val time = sbn.postTime
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        val content =
            sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()
//        val smallIconId = sbn.notification.extras.getInt(Notification.EXTRA_SMALL_ICON, 0)
//        val bitmap: Bitmap? = if(smallIconId == 0) null else {
//            FileUtil.getSmallIcon(baseContext, sbn.packageName, smallIconId)
//        }
        lifecycleScope.launch(Dispatchers.IO) {
            val appModel: AppModel =
                database.appModelDao.queryByPackageName(sbn.packageName) ?: run {
                    val ai = try {
                        packageManager.getApplicationInfo(sbn.packageName, 0)
                    } catch (e: NameNotFoundException) {
                        null
                    }
                    val appName = ai?.let { packageManager.getApplicationLabel(it).toString() }
                    database.appModelDao.insert(
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                        )
                    ).let {
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                            id = it
                        )
                    }
                }
            if (appModel.disabled) return@launch
            val bitmap = FileUtil.getAppIcon(baseContext, sbn.packageName)
            val avatarUri = bitmap?.let {
                FileUtil.getUriFromBitmap(baseContext, it, appModel.appName).apply {
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            val profile = Profile(
                name = title,
                avatar = null,
                id = null,
                avatarUri = avatarUri
            )
            val subjectProfile = Profile(
                name = appModel.appName,
                avatar = null,
                id = appModel.id,
                avatarUri = avatarUri
            )
            receiveMessage(
                ReceiveMessageDto(
                    contents = listOf(PlainText(text = content)),
                    profile = profile,
                    subjectProfile = subjectProfile,
                    timestamp = time,
                    messageId = null,
                    pluginConnection = PluginConnection(
                        connectionType = connectionType,
                        sendTargetType = SendTargetType.GROUP,
                        id = appModel.id
                    )
                ),
                onResult = {
                    Log.d("parabox", "receiveMessage result: $it")
                }
            )
        }
    }

    private fun receiveOtherSbn(sbn: StatusBarNotification) {
        val time = sbn.postTime
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        val content =
            sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()
//        val smallIconId = sbn.notification.extras.getInt(Notification.EXTRA_SMALL_ICON, 0)
//        val bitmap: Bitmap? = if(smallIconId == 0) null else {
//            FileUtil.getSmallIcon(baseContext, sbn.packageName, smallIconId)
//        }
        lifecycleScope.launch(Dispatchers.IO) {
            val appModel: AppModel =
                database.appModelDao.queryByPackageName(sbn.packageName) ?: run {
                    val ai = try {
                        packageManager.getApplicationInfo(sbn.packageName, 0)
                    } catch (e: NameNotFoundException) {
                        null
                    }
                    val appName = ai?.let { packageManager.getApplicationLabel(it).toString() }
                    database.appModelDao.insert(
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                        )
                    ).let {
                        AppModel(
                            packageName = sbn.packageName,
                            appName = appName ?: "Unknown",
                            id = it
                        )
                    }
                }
            if (appModel.disabled) return@launch
            val bitmap = FileUtil.getAppIcon(baseContext, sbn.packageName)
            val avatarUri = bitmap?.let {
                FileUtil.getUriFromBitmap(baseContext, it, appModel.appName).apply {
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    grantUriPermission(
                        "com.ojhdtapp.parabox",
                        this,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            val profile = Profile(
                name = appModel.appName,
                avatar = null,
                id = appModel.id,
                avatarUri = avatarUri
            )
            receiveMessage(
                ReceiveMessageDto(
                    contents = listOf(PlainText(text = "$title\n$content")),
                    profile = profile,
                    subjectProfile = profile,
                    timestamp = time,
                    messageId = null,
                    pluginConnection = PluginConnection(
                        connectionType = connectionType,
                        sendTargetType = SendTargetType.USER,
                        id = appModel.id
                    )
                ),
                onResult = {
                    Log.d("parabox", "receiveMessage result: $it")
                }
            )
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
        return wxSbnMap.get(dto.pluginConnection.id)?.let {
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
                                            "com.tencent.mobileqq" -> {
                                                receiveQQSbn(sbn)
                                            }
                                            in listOf<String>(
                                                "com.google.android.apps.messaging",
                                                "com.android.mms",
                                                "com.google.android.gm",
                                                "com.google.android.youtube",
                                            ) -> {
                                                receiveConversationSbn(sbn)
                                            }
                                            else -> {
                                                receiveOtherSbn(sbn)
                                            }
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