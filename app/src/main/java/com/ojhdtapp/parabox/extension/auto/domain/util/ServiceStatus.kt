package com.ojhdtapp.parabox.extension.auto.domain.util

sealed class ServiceStatus(open val message: String) {
    data class Loading(override val message: String) : ServiceStatus(message)
    data class Pause(override val message: String) : ServiceStatus(message)
    data class Error(override val message: String) : ServiceStatus(message)
    data class Running(override val message: String) : ServiceStatus(message)
    object Stop : ServiceStatus("")
}