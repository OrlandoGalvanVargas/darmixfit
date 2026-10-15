package com.darmix.darmixfit.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

object RestTimer {
    const val MIN_SECONDS = 5
    const val MAX_SECONDS = 300
    const val DEFAULT_SECONDS = 30
    const val EXTEND_SECONDS = 15

    private const val TAG = "RestTimer"
    private const val ALARM_REQUEST_CODE = 4001
    private const val ACTION_ALARM = "com.darmix.darmixfit.action.REST_TIMER_ALARM"

    fun start(context: Context, seconds: Int) {
        val app = context.applicationContext
        DarmixChime.prewarm()
        SessionNotifier.cancel(app)
        val state = TimerStore.begin(seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
        scheduleAlarm(app, state.endAtElapsed)
        startService(app)
    }

    fun extend(context: Context, seconds: Int = EXTEND_SECONDS) {
        val app = context.applicationContext
        val state = TimerStore.extend(seconds) ?: return
        scheduleAlarm(app, state.endAtElapsed)
        startService(app)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        TimerStore.cancel()
        cancelAlarm(app)
        app.stopService(Intent(app, TimerService::class.java))
        SessionNotifier.refresh(app)
    }

    private fun startService(context: Context) {
        try {
            ContextCompat.startForegroundService(
                context,
                TimerService.intent(context, TimerService.ACTION_START)
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar el servicio", e)
        }
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
