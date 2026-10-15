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
import com.darmix.darmixfit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var countdownJob: Job? = null
    private var upNextJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmSound: AlarmSound? = null
    private var beeping = false
    private var upNext: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_CANCEL) {
            TimerStore.cancel()
            RestTimer.cancelAlarm(this)
            finish()
            SessionNotifier.refresh(this)
            return START_NOT_STICKY
        }

        if (action == ACTION_EXTEND) {
            RestTimer.extend(this)
            return START_NOT_STICKY
        }

        val state = TimerStore.state.value
        startInForeground(buildNotification(state, finished = action == ACTION_FIRE))

        if (action == ACTION_FIRE) fireNow() else beginCountdown(state)
        return START_NOT_STICKY
    }

    private fun beginCountdown(state: TimerState) {
        if (beeping) {
            alarmSound?.release()
            alarmSound = null
            beeping = false
        }
        if (!state.active) {
            finish()
            return
        }
        acquireWakeLock(state.endAtElapsed - SystemClock.elapsedRealtime() + 15_000L)
        loadUpNext()
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (true) {
                if (!TimerStore.state.value.active) return@launch
                val remaining = state.endAtElapsed - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                delay(minOf(remaining, 500L))
            }
            fireNow()
        }
    }

    private fun loadUpNext() {
        upNextJob?.cancel()
        upNextJob = scope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching { WorkoutSession.snapshot(this@TimerService) }.getOrNull()
            }
            val step = snapshot?.step ?: return@launch
            val unit = if (step.isSuperset) "Ronda" else "Serie"
            val label = "Sigue: $unit ${step.setNumber} de ${step.totalSets} · ${step.title}"
            if (label == upNext) return@launch
            upNext = label
            val current = TimerStore.state.value
            if (!beeping && current.active) {
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIF_ID, buildNotification(current, finished = false))
            }
        }
    }

    private fun fireNow() {
        if (beeping) return
        if (!TimerStore.tryComplete()) {
            finish()
            return
        }
        countdownJob?.cancel()
        RestTimer.cancelAlarm(this)
        beeping = true
        acquireWakeLock(SOUND_MS + 10_000L)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(TimerState(), finished = true))
        SessionNotifier.refresh(this, "Descanso terminado")
        alarmSound = AlarmSound(this).also { it.play(SOUND_MS) { finish() } }
    }

    private fun finish() {
        countdownJob?.cancel()
        upNextJob?.cancel()
        alarmSound?.release()
        alarmSound = null
        beeping = false
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
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(SessionNotifier.ACCENT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        SessionNotifier.openIntent(this)?.let { builder.setContentIntent(it) }

        if (finished || !state.active) {
            builder
                .setContentTitle("¡Descanso terminado!")
                .setContentText("A por la siguiente serie")
        } else {
            val remaining = (state.endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            builder
                .setContentTitle("Descanso en curso")
                .setContentText(upNext ?: "Respira, ya casi vuelves")
                .setSubText("${state.totalSeconds} s")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + remaining)
                .addAction(
                    0,
                    "+${RestTimer.EXTEND_SECONDS} s",
                    PendingIntent.getService(
                        this, 2, intent(this, ACTION_EXTEND), PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .addAction(
                    0,
                    "Saltar",
                    PendingIntent.getService(
                        this, 1, intent(this, ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE
                    )
                )
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador de descanso",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Cuenta regresiva del descanso entre series"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
        const val ACTION_EXTEND = "com.darmix.darmixfit.action.TIMER_EXTEND"

        private const val TAG = "TimerService"
        private const val CHANNEL_ID = "rest_countdown"
        private const val LEGACY_CHANNEL_ID = "rest_timer"
        private const val NOTIF_ID = 1001
        private const val SOUND_MS = 3_000L

        fun intent(context: Context, action: String): Intent =
            Intent(context, TimerService::class.java).setAction(action)
    }
}
