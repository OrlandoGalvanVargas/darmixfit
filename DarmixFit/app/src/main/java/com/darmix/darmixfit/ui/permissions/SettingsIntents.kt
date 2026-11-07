package com.darmix.darmixfit.ui.permissions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

object SettingsIntents {

    fun openNotificationSettings(context: Context) {
        val appNotifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        startFirstAvailable(context, appNotifications, appDetails(context))
    }

    fun requestExactAlarms(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val direct = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                "package:${context.packageName}".toUri()
            )
            val general = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startFirstAvailable(context, direct, general, appDetails(context))
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

    fun openVendorSettings(context: Context, vendor: Vendor): Boolean {
        val specific = vendorIntents(vendor)
        if (specific.isNotEmpty() && startFirstAvailable(context, *specific.toTypedArray())) return true
        startFirstAvailable(context, appDetails(context))
        return false
    }

    private fun vendorIntents(vendor: Vendor): List<Intent> = when (vendor) {
        Vendor.XIAOMI -> components(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )

        Vendor.HUAWEI -> components(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )

        Vendor.HONOR -> components(
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )

        Vendor.OPPO -> components(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity"
        )

        Vendor.ONEPLUS -> components(
            "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity"
        )

        Vendor.VIVO -> components(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        )

        Vendor.SAMSUNG -> components(
            "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
            "com.samsung.android.sm" to "com.samsung.android.sm.ui.battery.BatteryActivity"
        )

        Vendor.ASUS -> components(
            "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
            "com.asus.mobilemanager" to "com.asus.mobilemanager.entry.FunctionActivity"
        )

        Vendor.TRANSSION -> components(
            "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"
        )
    }

    private fun components(vararg targets: Pair<String, String>): List<Intent> =
        targets.map { (pkg, cls) -> Intent().setComponent(ComponentName(pkg, cls)) }

    private fun appDetails(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        )

    private fun startFirstAvailable(context: Context, vararg intents: Intent): Boolean {
        for (intent in intents) {
            try {
                if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
        return false
    }
}
