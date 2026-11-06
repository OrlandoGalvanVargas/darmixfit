package com.darmix.darmixfit.ui.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import com.darmix.darmixfit.ui.theme.AccentPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HapticPreference(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun set(value: Boolean) {
        if (_enabled.value == value) return
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        _enabled.value = value
    }

    private companion object {
        const val PREFS_NAME = "darmixfit_haptics"
        const val KEY_ENABLED = "haptics_enabled"
    }
}

@Immutable
class SettingsActions(
    val setDark: (Boolean) -> Unit,
    val setAccent: (AccentPalette) -> Unit,
    val setHaptics: (Boolean) -> Unit
)

val LocalSettingsActions = staticCompositionLocalOf<SettingsActions> {
    error("LocalSettingsActions no ha sido provisto")
}

val LocalHapticsEnabled = staticCompositionLocalOf { true }
