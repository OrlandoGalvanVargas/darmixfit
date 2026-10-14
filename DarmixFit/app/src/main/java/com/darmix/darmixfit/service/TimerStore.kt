package com.darmix.darmixfit.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerState(
    val active: Boolean = false,
    /** Momento de fin, medido con SystemClock.elapsedRealtime(). */
    val endAtElapsed: Long = 0L,
    val totalSeconds: Int = 0,
)

/**
 * Fuente única de verdad del temporizador. Se guarda en disco para que el servicio
 * y la alarma puedan leerla aunque el proceso haya sido reiniciado.
 */
object TimerStore {
    private const val PREFS = "fitflow_timer"
    private const val K_ACTIVE = "active"
    private const val K_END = "end_at_elapsed"
    private const val K_TOTAL = "total_seconds"
    private const val K_BOOT = "boot_count"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = load()
    }

    @Synchronized
    fun begin(seconds: Int): TimerState {
        val s = TimerState(
            active = true,
            endAtElapsed = SystemClock.elapsedRealtime() + seconds * 1000L,
            totalSeconds = seconds,
        )
        persist(s)
        return s
    }

    @Synchronized
    fun cancel() {
        if (_state.value.active) persist(TimerState())
    }

    /** Devuelve true solo para el primer llamador; así el sonido nunca se dispara dos veces. */
    @Synchronized
    fun tryComplete(): Boolean {
        if (!_state.value.active) return false
        persist(TimerState())
        return true
    }

    /** Descarta (en silencio) un temporizador que terminó hace mucho y nadie atendió. */
    @Synchronized
    fun expireIfStale(graceMs: Long = 15_000L) {
        val s = _state.value
        if (s.active && SystemClock.elapsedRealtime() > s.endAtElapsed + graceMs) {
            persist(TimerState())
        }
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, 0)

    private fun load(): TimerState {
        if (!prefs.getBoolean(K_ACTIVE, false)) return TimerState()
        val end = prefs.getLong(K_END, 0L)
        val total = prefs.getInt(K_TOTAL, 0)
        val sameBoot = prefs.getInt(K_BOOT, -1) == bootCount()
        val remaining = end - SystemClock.elapsedRealtime()
        // Si el teléfono se reinició, elapsedRealtime empezó de cero y el dato ya no sirve.
        if (!sameBoot || remaining > total * 1000L + 5_000L) {
            persist(TimerState())
            return TimerState()
        }
        return TimerState(active = true, endAtElapsed = end, totalSeconds = total)
    }

    private fun persist(s: TimerState) {
        prefs.edit {
            putBoolean(K_ACTIVE, s.active)
            putLong(K_END, s.endAtElapsed)
            putInt(K_TOTAL, s.totalSeconds)
            putInt(K_BOOT, bootCount())
        }
        _state.value = s
    }
}