package com.darmix.darmixfit.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class ThemePreference(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacy()
    }

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(newMode: ThemeMode) {
        if (_mode.value == newMode) return
        prefs.edit { putString(KEY_MODE, newMode.name) }
        _mode.value = newMode
    }

    fun toggle(currentlyDark: Boolean) {
        setMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun cycle() {
        setMode(
            when (_mode.value) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
        )
    }

    private fun readMode(): ThemeMode {
        val name = prefs.getString(KEY_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private fun migrateLegacy() {
        if (prefs.contains(KEY_MODE)) return
        val legacy = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val legacyMode = legacy.getString(KEY_MODE, null) ?: return
        prefs.edit { putString(KEY_MODE, legacyMode) }
        legacy.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "darmixfit_theme"
        const val LEGACY_PREFS_NAME = "fitflow_theme"
        const val KEY_MODE = "theme_mode"
    }
}

val LocalThemeToggle = staticCompositionLocalOf<() -> Unit> {
    error("LocalThemeToggle no ha sido provisto")
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
