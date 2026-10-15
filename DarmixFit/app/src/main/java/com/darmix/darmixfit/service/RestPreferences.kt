package com.darmix.darmixfit.service

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RestAutoState(
    val enabled: Boolean = false,
    val seconds: Int = RestTimer.DEFAULT_SECONDS
)

class RestPreferences private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<RestAutoState> = _state.asStateFlow()

    fun update(enabled: Boolean, seconds: Int) {
        val next = RestAutoState(enabled, seconds.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS))
        if (next == _state.value) return
        prefs.edit {
            putBoolean(K_ENABLED, next.enabled)
            putInt(K_SECONDS, next.seconds)
        }
        _state.value = next
    }

    private fun load() = RestAutoState(
        enabled = prefs.getBoolean(K_ENABLED, false),
        seconds = prefs.getInt(K_SECONDS, RestTimer.DEFAULT_SECONDS)
            .coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS)
    )

    companion object {
        private const val PREFS = "darmixfit_rest"
        private const val K_ENABLED = "auto_enabled"
        private const val K_SECONDS = "auto_seconds"

        @Volatile
        private var instance: RestPreferences? = null

        fun get(context: Context): RestPreferences =
            instance ?: synchronized(this) {
                instance ?: RestPreferences(context.applicationContext).also { instance = it }
            }
    }
}
