package com.darmix.darmixfit.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

enum class AutostartState { ENABLED, DISABLED, UNKNOWN }

enum class Vendor { XIAOMI, HUAWEI, HONOR, OPPO, ONEPLUS, VIVO, SAMSUNG, ASUS, TRANSSION }

@Immutable
data class SetupStatus(
    val notificationsOk: Boolean,
    val notificationPermissionGranted: Boolean,
    val exactAlarmsOk: Boolean,
    val exactAlarmsRelevant: Boolean,
    val batteryOk: Boolean,
    val vendor: Vendor?,
    val vendorConfirmed: Boolean,
    val autostart: AutostartState = AutostartState.UNKNOWN
) {
    val vendorRequired: Boolean get() = vendor != null
    val autostartDetectable: Boolean get() = autostart != AutostartState.UNKNOWN
    val vendorOk: Boolean
        get() = when {
            vendor == null -> true
            autostart == AutostartState.ENABLED -> true
            autostart == AutostartState.DISABLED -> false
            else -> vendorConfirmed
        }
    val allOk: Boolean get() = notificationsOk && exactAlarmsOk && batteryOk && vendorOk
}

object SetupChecker {
    private const val PREFS = "darmixfit_setup"
    private const val LEGACY_PREFS = "fitflow_setup"
    private const val KEY_VENDOR_CONFIRMED = "xiaomi_confirmed"
    private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
    private const val OP_AUTO_START = 10008

    fun check(context: Context): SetupStatus {
        val prefs = prefs(context)
        val permissionGranted = notificationPermissionGranted(context)
        val notificationsOk = permissionGranted &&
                NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                noBlockedChannels(context)
        val exactAlarmsRelevant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val exactAlarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }.getOrDefault(true)
        } else {
            true
        }
        val batteryOk = runCatching {
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(true)

        val vendor = detectVendor()
        return SetupStatus(
            notificationsOk = notificationsOk,
            notificationPermissionGranted = permissionGranted,
            exactAlarmsOk = exactAlarmsOk,
            exactAlarmsRelevant = exactAlarmsRelevant,
            batteryOk = batteryOk,
            vendor = vendor,
            vendorConfirmed = prefs.getBoolean(KEY_VENDOR_CONFIRMED, false),
            autostart = if (vendor == Vendor.XIAOMI) xiaomiAutostart(context) else AutostartState.UNKNOWN
        )
    }

    fun confirmVendor(context: Context) {
        prefs(context).edit { putBoolean(KEY_VENDOR_CONFIRMED, true) }
    }

    fun notificationsAsked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS_ASKED, false)

    fun markNotificationsAsked(context: Context) {
        prefs(context).edit { putBoolean(KEY_NOTIFICATIONS_ASKED, true) }
    }

    private fun notificationPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun noBlockedChannels(context: Context): Boolean = runCatching {
        val manager = NotificationManagerCompat.from(context)
        val channelsOk = manager.notificationChannelsCompat.none { it.importance == NotificationManager.IMPORTANCE_NONE }
        val groupsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(NotificationManager::class.java).notificationChannelGroups.none { it.isBlocked }
        } else {
            true
        }
        channelsOk && groupsOk
    }.getOrDefault(true)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { current ->
            if (!current.contains(KEY_VENDOR_CONFIRMED)) {
                val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                if (legacy.getBoolean(KEY_VENDOR_CONFIRMED, false)) {
                    current.edit { putBoolean(KEY_VENDOR_CONFIRMED, true) }
                }
            }
        }

    private fun xiaomiAutostart(context: Context): AutostartState =
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

    private fun detectVendor(): Vendor? {
        val maker = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        fun any(vararg names: String) = names.any { it == maker || it == brand }
        return when {
            any("xiaomi", "redmi", "poco") -> Vendor.XIAOMI
            any("honor") -> Vendor.HONOR
            any("huawei") -> Vendor.HUAWEI
            any("oneplus") -> Vendor.ONEPLUS
            any("oppo", "realme") -> Vendor.OPPO
            any("vivo", "iqoo") -> Vendor.VIVO
            any("samsung") -> Vendor.SAMSUNG
            any("asus") -> Vendor.ASUS
            any("tecno", "infinix", "itel", "transsion") -> Vendor.TRANSSION
            else -> null
        }
    }
}
