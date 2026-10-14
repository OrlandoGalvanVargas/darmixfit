package com.darmix.darmixfit.ui.permissions

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

data class SetupStatus(
    val notificationsOk: Boolean,
    val exactAlarmsOk: Boolean,
    val batteryOk: Boolean,
    val xiaomiRequired: Boolean,
    val xiaomiConfirmed: Boolean,
) {
    val xiaomiOk: Boolean get() = !xiaomiRequired || xiaomiConfirmed
    val allOk: Boolean get() = notificationsOk && exactAlarmsOk && batteryOk && xiaomiOk
}

object SetupChecker {
    private const val PREFS = "fitflow_setup"
    private const val KEY_XIAOMI_CONFIRMED = "xiaomi_confirmed"

    fun check(context: Context): SetupStatus {
        val notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val exactAlarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }

        val batteryOk = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

        val xiaomiRequired = isXiaomiFamily()
        val xiaomiConfirmed = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_XIAOMI_CONFIRMED, false)

        return SetupStatus(
            notificationsOk = notificationsOk,
            exactAlarmsOk = exactAlarmsOk,
            batteryOk = batteryOk,
            xiaomiRequired = xiaomiRequired,
            xiaomiConfirmed = xiaomiConfirmed,
        )
    }

    fun confirmXiaomi(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_XIAOMI_CONFIRMED, true)
        }
    }

    private fun isXiaomiFamily(): Boolean {
        val maker = Build.MANUFACTURER.lowercase()
        return maker == "xiaomi" || maker == "redmi" || maker == "poco"
    }
}