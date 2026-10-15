package com.darmix.darmixfit.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.darmix.darmixfit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SessionNotifier {

    const val CHANNEL_ID = "workout_session"
    const val ACCENT = 0xFFC6FF3D.toInt()

    private const val NOTIF_ID = 1002
    private const val PREFS = "darmixfit_session"
    private const val K_HIDDEN = "hidden"
    private const val REQ_COMPLETE = 5001
    private const val REQ_HIDE = 5002
    private const val REQ_OPEN = 5003
    private const val TIMEOUT_MS = 3 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var appVisible: Boolean = false
        private set

    fun onAppVisible(context: Context) {
        appVisible = true
        prefs(context).edit { putBoolean(K_HIDDEN, false) }
        cancel(context)
    }

    fun onAppHidden(context: Context) {
        appVisible = false
        refresh(context)
    }

    fun hide(context: Context) {
        prefs(context).edit { putBoolean(K_HIDDEN, true) }
        cancel(context)
    }

    fun refresh(context: Context, headline: String? = null) {
        val app = context.applicationContext
        scope.launch { refreshNow(app, headline) }
    }

    suspend fun refreshNow(context: Context, headline: String? = null) {
        if (!canShow(context)) return
        if (TimerStore.state.value.active) return
        val snapshot = runCatching { WorkoutSession.snapshot(context) }.getOrNull() ?: return
        val step = snapshot.step
        if (!snapshot.started || step == null) {
            cancel(context)
            return
        }
        post(context, snapshot, step, headline)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    fun postComplete(context: Context, snapshot: SessionSnapshot) {
        if (!canShow(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle("¡Día conquistado!")
            .setContentText("${snapshot.totalSets} series completadas. Buen trabajo.")
            .setSubText("${snapshot.totalSets}/${snapshot.totalSets} series")
            .setContentIntent(openIntent(context))
            .setAutoCancel(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setTimeoutAfter(TIMEOUT_MS)
            .build()
        notify(context, notification)
    }

    private fun post(context: Context, snapshot: SessionSnapshot, step: SessionStep, headline: String?) {
        ensureChannel(context)
        val unit = if (step.isSuperset) "Ronda" else "Serie"
        val position = "$unit ${step.setNumber} de ${step.totalSets}"
        val title = headline?.let { "$it · $position" } ?: position
        val details = buildString {
            append(step.reps)
            step.nextName?.let { append("\nDespués: ").append(it) }
        }

        val complete = PendingIntent.getBroadcast(
            context,
            REQ_COMPLETE,
            Intent(context, SessionActionReceiver::class.java)
                .setAction(SessionActionReceiver.ACTION_COMPLETE)
                .putExtra(SessionActionReceiver.EXTRA_IDS, step.ids)
                .putExtra(SessionActionReceiver.EXTRA_SET, step.setNumber),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hide = PendingIntent.getBroadcast(
            context,
            REQ_HIDE,
            Intent(context, SessionActionReceiver::class.java)
                .setAction(SessionActionReceiver.ACTION_HIDE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_action_check,
            "Completar ${unit.lowercase()} ${step.setNumber}",
            complete
        )
            .setAuthenticationRequired(false)
            .setShowsUserInterface(false)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(step.title)
            .setSubText("${snapshot.doneSets}/${snapshot.totalSets} series")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("${step.title}\n$details")
            )
            .setProgress(snapshot.totalSets, snapshot.doneSets, false)
            .addAction(completeAction)
            .addAction(0, "Ocultar", hide)
            .setContentIntent(openIntent(context))
            .setDeleteIntent(hide)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setTimeoutAfter(TIMEOUT_MS)
            .build()
        notify(context, notification)
    }

    fun openIntent(context: Context): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            PendingIntent.getActivity(
                context,
                REQ_OPEN,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sesión de entrenamiento",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Serie actual en la pantalla de bloqueo para completarla sin desbloquear"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun canShow(context: Context): Boolean {
        if (appVisible) return false
        if (prefs(context).getBoolean(K_HIDDEN, false)) return false
        return notificationsAllowed(context)
    }

    private fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun notify(context: Context, notification: android.app.Notification) {
        if (!notificationsAllowed(context)) return
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
