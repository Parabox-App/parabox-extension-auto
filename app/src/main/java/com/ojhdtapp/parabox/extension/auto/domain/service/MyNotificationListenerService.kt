package com.ojhdtapp.parabox.extension.auto.domain.service

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ojhdtapp.parabox.extension.auto.core.util.dataStore
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    private var notificationMessageListener: NotificationMessageListener? = null

//    override fun onBind(intent: Intent?): IBinder? {
//        super.onBind(intent)
//        return NotificationListenerServiceBinder()
//    }

    inner class NotificationListenerServiceBinder : Binder(){
        fun getService(): MyNotificationListenerService = this@MyNotificationListenerService
    }

    fun setMessageListener(listener: NotificationMessageListener){
        notificationMessageListener = listener
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d("parabox", "onNotificationPosted")
        sbn?.notification?.extras?.let {
            val title = it.getString(Notification.EXTRA_TITLE)
            val content = it.getString(Notification.EXTRA_TEXT)
            Log.d("parabox", "title: $title, content: $content")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerConnected() {
        Log.d("parabox", "onListenerConnected")
        notificationMessageListener?.onStateChange(ParaboxKey.STATE_RUNNING, "监听服务正常运行")
    }

    override fun onListenerDisconnected() {
        Log.d("parabox", "onListenerDisconnected")
        requestRebind(ComponentName(this, MyNotificationListenerService::class.java))
        notificationMessageListener?.onStateChange(ParaboxKey.STATE_PAUSE, "监听服务已停止，尝试重新绑定")
    }
}