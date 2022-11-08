package com.ojhdtapp.parabox.extension.auto.domain.service

interface NotificationMessageListener {
    fun onStateChange(state: Int, message: String?)
}