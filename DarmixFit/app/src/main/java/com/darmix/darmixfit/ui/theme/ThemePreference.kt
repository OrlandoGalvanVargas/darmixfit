package com.darmix.darmixfit.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Preferencia de tema persistida en SharedPreferences.
 * Expone un StateFlow para que Compose lo observe.
 */
class ThemePreference(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(newMode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, newMode.name).apply()
        _mode.value = newMode
    }

    /** Cicla SYSTEM → LIGHT → DARK → SYSTEM. */
    fun cycle() {
        val next = when (_mode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setMode(next)
    }

    private fun readMode(): ThemeMode {
        val name = prefs.getString(KEY_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val PREFS_NAME = "fitflow_theme"
        const val KEY_MODE = "theme_mode"
    }
}

/**
 * CompositionLocal opcional. Si lo prefieres, puedes pasar el `ThemePreference`
 * y el `ThemeMode` como parámetros explícitos a `FitFlowNavHost`. Yo lo uso aquí
 * para no tener que ensuciar todas las firmas con un parámetro más.
 */
val LocalThemeToggle = staticCompositionLocalOf<() -> Unit> {
    error("LocalThemeToggle no ha sido provisto")
}