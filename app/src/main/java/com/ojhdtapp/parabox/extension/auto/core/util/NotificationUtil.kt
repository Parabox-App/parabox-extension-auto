package com.ojhdtapp.parabox.extension.auto.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import com.ojhdtapp.parabox.extension.auto.MainActivity
import com.ojhdtapp.parabox.extension.auto.R

object NotificationUtil {
    const val SERVICE_STATE_CHANNEL_ID = "service_state"
    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1

    private fun createNotificationChannel(
        context: Context,
        channelName: String,
        channelDescription: String
    ) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            SERVICE_STATE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = channelDescription
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun startForegroundService(context: Service) {
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        createNotificationChannel(
            context,
            context.getString(R.string.service_status),
            context.getString(R.string.service_status)
        )
        val notification: Notification =
            Notification.Builder(context, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android_black_24dp)
                .setContentTitle(context.getString(R.string.foreground_service_notification_title))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build()
        context.startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }

    fun updateForegroundServiceNotification(context: Service, title: String, text: String? = null) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification: Notification =
            Notification.Builder(context, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android_black_24dp)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .build()
        notificationManager.notify(
            FOREGROUND_SERVICE_NOTIFICATION_ID,
            notification
        )
    }

    fun stopForegroundService(context: Service) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(FOREGROUND_SERVICE_NOTIFICATION_ID)
        context.stopForeground(true)
    }
}