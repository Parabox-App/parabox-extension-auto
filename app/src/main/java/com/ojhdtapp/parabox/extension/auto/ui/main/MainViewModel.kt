package com.ojhdtapp.parabox.extension.auto.ui.main

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.extension.auto.core.util.DataStoreKeys
import com.ojhdtapp.parabox.extension.auto.core.util.dataStore
import com.ojhdtapp.parabox.extension.auto.domain.util.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {
    // UiEvent
    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()
    fun emitToUiEventFlow(event: UiEvent) {
        viewModelScope.launch {
            _uiEventFlow.emit(event)
        }
    }

    // MainApp Installation
    private val _isMainAppInstalled = MutableStateFlow(false)
    val isMainAppInstalled get() = _isMainAppInstalled.asStateFlow()
    fun setMainAppInstalled(isInstalled: Boolean) {
        viewModelScope.launch {
            _isMainAppInstalled.emit(isInstalled)
        }
    }

    // Service Status
    private val _serviceStatusStateFlow = MutableStateFlow<ServiceStatus>(ServiceStatus.Stop)
    val serviceStatusStateFlow = _serviceStatusStateFlow.asStateFlow()
    fun updateServiceStatusStateFlow(value: ServiceStatus) {
        viewModelScope.launch {
            _serviceStatusStateFlow.emit(value)
        }
    }

    // Auto Login Switch
    val autoLoginSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.AUTO_LOGIN] ?: false
        }

    fun setAutoLoginSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.AUTO_LOGIN] = value
            }
        }
    }

    //Foreground Service
    val foregroundServiceSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.FOREGROUND_SERVICE] ?: true
        }

    fun setForegroundServiceSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.FOREGROUND_SERVICE] = value
            }
        }
    }

    private val _refreshingKey = mutableStateOf<Int>(0)
    val refreshingKey : State<Int> = _refreshingKey
    fun refreshKey(){
        _refreshingKey.value = _refreshingKey.value + 1
    }

    val appVersion = com.ojhdtapp.parabox.extension.auto.BuildConfig.VERSION_NAME
}

sealed interface UiEvent{
    data class ShowSnackbar(val message: String): UiEvent
}