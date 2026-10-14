package com.darmix.darmixfit.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun SetupGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(SetupChecker.check(context)) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }

    // Al volver de los ajustes del teléfono se vuelve a comprobar todo.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = SetupChecker.check(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        status = SetupChecker.check(context)
    }

    if (status.allOk) {
        content()
    } else {
        SetupScreen(
            status = status,
            onNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotifications) {
                    askedNotifications = true
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Ya se pidió antes (o el sistema bloqueó el diálogo): vamos a ajustes.
                    SettingsIntents.openNotificationSettings(context)
                }
            },
            onExactAlarms = { SettingsIntents.requestExactAlarms(context) },
            onBattery = { SettingsIntents.requestIgnoreBatteryOptimizations(context) },
            onOpenAutostart = { SettingsIntents.openXiaomiAutostart(context) },
            onOpenAppSettings = { SettingsIntents.openAppDetails(context) },
            onConfirmXiaomi = {
                SetupChecker.confirmXiaomi(context)
                status = SetupChecker.check(context)
            },
        )
    }
}