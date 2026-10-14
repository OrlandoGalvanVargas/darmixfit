package com.darmix.darmixfit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/** Se ejecuta cuando la alarma exacta se dispara; solo despierta al servicio. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            ContextCompat.startForegroundService(
                context,
                TimerService.intent(context, TimerService.ACTION_FIRE)
            )
        } catch (e: Exception) {
            Log.e("TimerAlarmReceiver", "No se pudo despertar al servicio", e)
        }
    }
}