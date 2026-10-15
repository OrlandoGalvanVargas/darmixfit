package com.darmix.darmixfit.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.darmix.darmixfit.ui.theme.Motion
import kotlinx.coroutines.delay

private const val CELEBRATION_MILLIS = 1500L

@Composable
fun SetupGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var status by remember { mutableStateOf(SetupChecker.check(context)) }
    var setupShown by rememberSaveable { mutableStateOf(!status.allOk) }
    var skipped by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var autostartFallback by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = SetupChecker.check(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        status = SetupChecker.check(context)
    }

    LaunchedEffect(status.allOk) {
        if (status.allOk && setupShown && !finished) {
            delay(CELEBRATION_MILLIS)
            finished = true
        }
    }

    val showApp = skipped || (status.allOk && (!setupShown || finished))

    Crossfade(targetState = showApp, animationSpec = Motion.enter(), label = "setup-gate") { app ->
        if (app) {
            content()
        } else {
            SetupScreen(
                status = status,
                onNotifications = {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (
                            !SetupChecker.notificationsAsked(context) ||
                                    (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission))
                            )
                    if (canAsk) {
                        SetupChecker.markNotificationsAsked(context)
                        notificationLauncher.launch(permission)
                    } else {
                        SettingsIntents.openNotificationSettings(context)
                    }
                },
                onExactAlarms = { SettingsIntents.requestExactAlarms(context) },
                onBattery = { SettingsIntents.requestIgnoreBatteryOptimizations(context) },
                autostartFallback = autostartFallback,
                onOpenAutostart = { autostartFallback = !SettingsIntents.openXiaomiAutostart(context) },
                onConfirmXiaomi = {
                    SetupChecker.confirmXiaomi(context)
                    status = SetupChecker.check(context)
                },
                onSkip = { skipped = true }
            )
        }
    }
}
