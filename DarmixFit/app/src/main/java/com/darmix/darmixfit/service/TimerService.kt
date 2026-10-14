package com.darmix.darmixfit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var countdownJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmSound: AlarmSound? = null
    private var beeping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_CANCEL) {
            TimerStore.cancel()
            RestTimer.cancelAlarm(this)
            finish()
            return START_NOT_STICKY
        }

        // Toda llamada a startForegroundService debe llamar a startForeground de inmediato.
        val state = TimerStore.state.value
        startInForeground(buildNotification(state, finished = action == ACTION_FIRE))

        if (action == ACTION_FIRE) fireNow() else beginCountdown(state)
        return START_NOT_STICKY
    }

    private fun beginCountdown(state: TimerState) {
        if (beeping) { // se inició un descanso nuevo mientras aún sonaba el anterior
            alarmSound?.release()
            alarmSound = null
            beeping = false
        }
        if (!state.active) {
            finish()
            return
        }
        acquireWakeLock(state.endAtElapsed - SystemClock.elapsedRealtime() + 15_000L)
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (true) {
                if (!TimerStore.state.value.active) return@launch // cancelado
                val remaining = state.endAtElapsed - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                delay(minOf(remaining, 500L))
            }
            fireNow()
        }
    }

    private fun fireNow() {
        if (beeping) return
        if (!TimerStore.tryComplete()) { // otro camino ya lo atendió
            finish()
            return
        }
        countdownJob?.cancel()
        RestTimer.cancelAlarm(this)
        beeping = true
        acquireWakeLock(SOUND_MS + 10_000L)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(TimerState(), finished = true))
        alarmSound = AlarmSound(this).also { it.play(SOUND_MS) { finish() } }
    }

    private fun finish() {
        countdownJob?.cancel()
        alarmSound?.release()
        alarmSound = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        alarmSound?.release()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo pasar a primer plano", e)
        }
    }

    private fun buildNotification(state: TimerState, finished: Boolean): Notification {
        ensureChannel()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Descanso")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setContentIntent(
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
            )
        }

        if (finished || !state.active) {
            builder.setContentText("¡Descanso terminado!")
        } else {
            val remaining = (state.endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            builder
                .setContentText("Tiempo restante")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + remaining)
                .addAction(
                    0,
                    "Cancelar",
                    PendingIntent.getService(
                        this, 1, intent(this, ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE
                    )
                )
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador de descanso",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cuenta regresiva del descanso entre series"
                setSound(null, null) // el sonido lo reproducimos nosotros por el canal de alarma
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "darmixfit:rest_timer")
            .apply {
                setReferenceCounted(false)
                acquire(timeoutMs.coerceAtLeast(1_000L))
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    companion object {
        const val ACTION_START = "com.darmix.darmixfit.action.TIMER_START"
        const val ACTION_FIRE = "com.darmix.darmixfit.action.TIMER_FIRE"
        const val ACTION_CANCEL = "com.darmix.darmixfit.action.TIMER_CANCEL"

        private const val TAG = "TimerService"
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIF_ID = 1001
        private const val SOUND_MS = 3_000L

        fun intent(context: Context, action: String): Intent =
            Intent(context, TimerService::class.java).setAction(action)
    }
}