package com.darmix.darmixfit.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/** Punto de entrada para iniciar y cancelar el temporizador de descanso. */
object RestTimer {
    const val MIN_SECONDS = 5
    const val MAX_SECONDS = 300
    const val DEFAULT_SECONDS = 30

    private const val TAG = "RestTimer"
    private const val ALARM_REQUEST_CODE = 4001
    private const val ACTION_ALARM = "com.darmix.darmixfit.action.REST_TIMER_ALARM"

    fun start(context: Context, seconds: Int) {
        val app = context.applicationContext
        val state = TimerStore.begin(seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
        scheduleAlarm(app, state.endAtElapsed)
        try {
            ContextCompat.startForegroundService(
                app,
                TimerService.intent(app, TimerService.ACTION_START)
            )
        } catch (e: Exception) {
            // Si el servicio no arranca, la alarma exacta sigue siendo el respaldo.
            Log.w(TAG, "No se pudo iniciar el servicio", e)
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        TimerStore.cancel()
        cancelAlarm(app)
        app.stopService(Intent(app, TimerService::class.java))
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(context: Context, endAtElapsed: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endAtElapsed,
                alarmIntent(context)
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso de alarma exacta; solo funcionará el servicio", e)
        }
    }

    internal fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, TimerAlarmReceiver::class.java).setAction(ACTION_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}