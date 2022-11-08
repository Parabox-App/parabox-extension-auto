package com.ojhdtapp.parabox.extension.auto

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxActivity
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.parabox.extension.auto.domain.service.ConnService
import com.ojhdtapp.parabox.extension.auto.domain.service.MyNotificationListenerService
import com.ojhdtapp.parabox.extension.auto.domain.util.CustomKey
import com.ojhdtapp.parabox.extension.auto.domain.util.ServiceStatus
import com.ojhdtapp.parabox.extension.auto.ui.main.MainScreen
import com.ojhdtapp.parabox.extension.auto.ui.main.MainViewModel
import com.ojhdtapp.parabox.extension.auto.ui.main.UiEvent
import com.ojhdtapp.parabox.extension.auto.ui.theme.ParaboxExtensionExampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ParaboxActivity<ConnService>(ConnService::class.java) {
    private val viewModel: MainViewModel by viewModels<MainViewModel>()

    private fun checkMainAppInstallation() {
        val pkg = "com.ojhdtapp.parabox"
        var res = false
        try {
            packageManager.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            res = true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        viewModel.setMainAppInstalled(res)
    }

    fun launchMainApp() {
        val pkg = "com.ojhdtapp.parabox"
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            startActivity(it.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun receiveTestMessage() {
        // TODO 5 : Call sendCommand function with COMMAND_RECEIVE_TEST_MESSAGE
        sendCommand(command = CustomKey.COMMAND_RECEIVE_TEST_MESSAGE,
            extra = Bundle().apply {
                putString("content", "Message sent at ${System.currentTimeMillis()}")
            },
            timeoutMillis = 3000,
            onResult = {
                if (it is ParaboxResult.Success) {
                    val obj = it.obj
                    val message = obj.getString("message")

                    Toast.makeText(
                        this,
                        getString(R.string.command_success_toast, message),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val errorCode = (it as ParaboxResult.Fail).errorCode

                    Toast.makeText(
                        this,
                        getString(R.string.command_failed_toast, errorCode.toString()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showTestMessageSnackbar(message: String) {
        viewModel.emitToUiEventFlow(
            UiEvent.ShowSnackbar(message)
        )
    }

    internal fun isNotificationServiceEnable(): Boolean {
        val mNotificationManager =
            getSystemService(NotificationListenerService.NOTIFICATION_SERVICE) as NotificationManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val status = mNotificationManager?.isNotificationListenerAccessGranted(
                ComponentName(
                    baseContext,
                    MyNotificationListenerService::class.java
                )
            )
            return status == true
        } else {
            try {
                val listeners =
                    Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                return !(listeners == null || !listeners.contains(BuildConfig.APPLICATION_ID))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    fun isAndroidAutoInstalled() : Boolean{
        val pkg = "com.google.android.projection.gearhead"
        var res = false
        try {
            packageManager.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            res = true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return res
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when (msg.what) {
            // TODO 10: Handle custom notification
            CustomKey.NOTIFICATION_SHOW_TEST_MESSAGE_SNACKBAR -> {
                (msg.obj as Bundle).getString("message")?.also {
                    showTestMessageSnackbar(it)
                }
            }
        }
    }

    override fun onParaboxServiceConnected() {
        getState()
    }

    override fun onParaboxServiceDisconnected() {

    }

    override fun onParaboxServiceStateChanged(state: Int, message: String?) {
        val serviceState = when (state) {
            ParaboxKey.STATE_ERROR -> ServiceStatus.Error(
                message ?: getString(R.string.status_error)
            )

            ParaboxKey.STATE_LOADING -> ServiceStatus.Loading(
                message ?: getString(R.string.status_loading)
            )

            ParaboxKey.STATE_PAUSE -> ServiceStatus.Pause(
                message ?: getString(R.string.status_pause)
            )

            ParaboxKey.STATE_STOP -> ServiceStatus.Stop
            ParaboxKey.STATE_RUNNING -> ServiceStatus.Running(
                message ?: getString(R.string.status_running)
            )

            else -> ServiceStatus.Stop
        }
        viewModel.updateServiceStatusStateFlow(serviceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive Navigation Bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Check MainApp Installation
        checkMainAppInstallation()
        setContent {
            ParaboxExtensionExampleTheme {
                // Set Icons Color on Immersive Navigation Bar
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }
                MainScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshKey()
        bindParaboxService()
    }

    override fun onStop() {
        super.onStop()
        unbindParaboxService()
    }
}