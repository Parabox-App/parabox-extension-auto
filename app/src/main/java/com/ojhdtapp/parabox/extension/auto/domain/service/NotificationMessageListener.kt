package com.ojhdtapp.parabox.extension.auto.domain.service

interface NotificationMessageListener {
    fun onStateChange(state: Int, message: String?)
    fun receiveSbn(sbn: android.service.notification.StatusBarNotification)
}