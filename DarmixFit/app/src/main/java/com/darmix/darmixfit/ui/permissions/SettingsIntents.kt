package com.darmix.darmixfit.ui.permissions

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

object SettingsIntents {

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        startFirstAvailable(context, intent, appDetails(context))
    }

    fun requestExactAlarms(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                "package:${context.packageName}".toUri()
            )
            startFirstAvailable(context, intent, appDetails(context))
        } else {
            startFirstAvailable(context, appDetails(context))
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
        val general = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startFirstAvailable(context, direct, general, appDetails(context))
    }

    fun openXiaomiAutostart(context: Context) {
        val autostart = Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )
        startFirstAvailable(context, autostart, appDetails(context))
    }

    fun openAppDetails(context: Context) {
        startFirstAvailable(context, appDetails(context))
    }

    private fun appDetails(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        )

    /** Intenta cada intent en orden hasta que uno se pueda abrir. */
    private fun startFirstAvailable(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // probamos el siguiente
            } catch (_: SecurityException) {
                // probamos el siguiente
            }
        }
    }
}