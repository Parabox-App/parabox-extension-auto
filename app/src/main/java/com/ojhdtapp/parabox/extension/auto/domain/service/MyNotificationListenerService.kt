package com.ojhdtapp.parabox.extension.auto.domain.service

import android.app.Notification
import android.app.PendingIntent
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

    fun sendReply(sbn: StatusBarNotification?, content: String): Boolean {
        return sbn?.notification?.let {
            val conversation = Notification.CarExtender(it).unreadConversation
            conversation?.let {
                val reply = it.replyPendingIntent
                val remoteInput = it.remoteInput
                val intent = Intent().apply {
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    putExtra(remoteInput.resultKey, content)
                }
                try {
                    reply.send(this, 0, intent)
                    true
                } catch (e: PendingIntent.CanceledException) {
                    e.printStackTrace()
                    null
                }
            }
        } ?: false
    }


    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == "com.ojhdtapp.parabox.extension.auto.core.ConnService") NotificationListenerServiceBinder()
        else super.onBind(intent)
    }

    inner class NotificationListenerServiceBinder : Binder() {
        fun getService(): MyNotificationListenerService = this@MyNotificationListenerService
    }

    fun setMessageListener(listener: NotificationMessageListener) {
        notificationMessageListener = listener
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName in listOf<String>("com.ojhdtapp.parabox")) return
        sbn?.let {
            notificationMessageListener?.receiveSbn(it)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerConnected() {
        notificationMessageListener?.onStateChange(ParaboxKey.STATE_RUNNING, "监听服务正常运行")
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, MyNotificationListenerService::class.java))
        notificationMessageListener?.onStateChange(ParaboxKey.STATE_LOADING, "监听服务停止，正在尝试重新绑定")
    }
}