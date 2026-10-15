package com.darmix.darmixfit.ui.permissions

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

enum class AutostartState { ENABLED, DISABLED, UNKNOWN }

@Immutable
data class SetupStatus(
    val notificationsOk: Boolean,
    val exactAlarmsOk: Boolean,
    val exactAlarmsRelevant: Boolean,
    val batteryOk: Boolean,
    val xiaomiRequired: Boolean,
    val xiaomiConfirmed: Boolean,
    val autostart: AutostartState = AutostartState.UNKNOWN
) {
    val autostartDetectable: Boolean get() = autostart != AutostartState.UNKNOWN
    val xiaomiOk: Boolean
        get() = when {
            !xiaomiRequired -> true
            autostart == AutostartState.ENABLED -> true
            autostart == AutostartState.DISABLED -> false
            else -> xiaomiConfirmed
        }
    val allOk: Boolean get() = notificationsOk && exactAlarmsOk && batteryOk && xiaomiOk
}

object SetupChecker {
    private const val PREFS = "darmixfit_setup"
    private const val LEGACY_PREFS = "fitflow_setup"
    private const val KEY_XIAOMI_CONFIRMED = "xiaomi_confirmed"
    private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
    private const val OP_AUTO_START = 10008

    fun check(context: Context): SetupStatus {
        val prefs = prefs(context)
        val notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exactAlarmsRelevant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val exactAlarmsOk = if (exactAlarmsRelevant) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
        val batteryOk = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

        val xiaomi = isXiaomiFamily()
        return SetupStatus(
            notificationsOk = notificationsOk,
            exactAlarmsOk = exactAlarmsOk,
            exactAlarmsRelevant = exactAlarmsRelevant,
            batteryOk = batteryOk,
            xiaomiRequired = xiaomi,
            xiaomiConfirmed = prefs.getBoolean(KEY_XIAOMI_CONFIRMED, false),
            autostart = if (xiaomi) autostartState(context) else AutostartState.UNKNOWN
        )
    }

    fun confirmXiaomi(context: Context) {
        prefs(context).edit { putBoolean(KEY_XIAOMI_CONFIRMED, true) }
    }

    fun notificationsAsked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS_ASKED, false)

    fun markNotificationsAsked(context: Context) {
        prefs(context).edit { putBoolean(KEY_NOTIFICATIONS_ASKED, true) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { current ->
            if (!current.contains(KEY_XIAOMI_CONFIRMED)) {
                val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                if (legacy.getBoolean(KEY_XIAOMI_CONFIRMED, false)) {
                    current.edit { putBoolean(KEY_XIAOMI_CONFIRMED, true) }
                }
            }
        }

    private fun autostartState(context: Context): AutostartState =
        try {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            when (method.invoke(ops, OP_AUTO_START, Process.myUid(), context.packageName) as Int) {
                AppOpsManager.MODE_ALLOWED -> AutostartState.ENABLED
                AppOpsManager.MODE_IGNORED -> AutostartState.DISABLED
                else -> AutostartState.UNKNOWN
            }
        } catch (_: Throwable) {
            AutostartState.UNKNOWN
        }

    private fun isXiaomiFamily(): Boolean {
        val maker = Build.MANUFACTURER.lowercase()
        return maker == "xiaomi" || maker == "redmi" || maker == "poco"
    }
}
