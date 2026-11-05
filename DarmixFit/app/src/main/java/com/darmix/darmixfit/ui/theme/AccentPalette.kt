package com.darmix.darmixfit.ui.theme

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

enum class AccentPalette(internal val hue: Float?, internal val lightBoost: Float) {
    VOLT(null, 0f),
    PLASMA(192f, 0f),
    MAGMA(30f, 0f),
    ION(272f, 0.25f)
}

@Immutable
data class AccentTones(
    val main: Color,
    val bright: Color,
    val deep: Color,
    val ink: Color,
    val shadow: Color,
    val mist: Color
)

private const val BASE_HUE = 78f
private const val MIN_HUE = 45f
private const val MAX_HUE = 135f
private const val MIN_SATURATION = 0.04f

fun Color.toAccent(accent: AccentPalette): Color {
    val target = accent.hue ?: return this
    val r = red
    val g = green
    val b = blue
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val delta = high - low
    if (delta <= 0f) return this
    val lightness = (high + low) / 2f
    val saturation = if (lightness > 0.5f) delta / (2f - high - low) else delta / (high + low)
    val hue = when (high) {
        r -> ((g - b) / delta + if (g < b) 6f else 0f) * 60f
        g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }
    if (saturation < MIN_SATURATION || hue < MIN_HUE || hue > MAX_HUE) return this
    val shifted = ((hue - BASE_HUE + target) % 360f + 360f) % 360f
    val boosted = if (saturation > 0.35f && lightness > 0.3f) {
        min(0.97f, lightness + accent.lightBoost * (1f - lightness))
    } else {
        lightness
    }
    return Color.hsl(shifted, saturation.coerceIn(0f, 1f), boosted.coerceIn(0f, 1f), alpha)
}

private val ToneCache: Map<AccentPalette, AccentTones> = AccentPalette.entries.associateWith { accent ->
    AccentTones(
        main = DarmixPalette.Volt.toAccent(accent),
        bright = DarmixPalette.VoltBright.toAccent(accent),
        deep = DarmixPalette.VoltDeep.toAccent(accent),
        ink = DarmixPalette.VoltInk.toAccent(accent),
        shadow = DarmixPalette.VoltShadow.toAccent(accent),
        mist = DarmixPalette.VoltMist.toAccent(accent)
    )
}

val AccentPalette.tones: AccentTones
    get() = ToneCache.getValue(this)

fun DarmixColors.withAccent(accent: AccentPalette): DarmixColors {
    if (accent == AccentPalette.VOLT) return this
    val t = accent.tones
    return copy(
        volt = t.main,
        onVolt = t.ink,
        voltSoft = if (isDark) t.shadow else t.mist,
        voltText = if (isDark) t.main else t.deep,
        voltBright = t.bright,
        voltDeep = t.deep
    )
}

val LocalAccentPalette = staticCompositionLocalOf { AccentPalette.VOLT }

class AccentPreference(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _palette = MutableStateFlow(read())
    val palette: StateFlow<AccentPalette> = _palette.asStateFlow()

    fun set(accent: AccentPalette) {
        if (_palette.value == accent) return
        prefs.edit { putString(KEY_PALETTE, accent.name) }
        _palette.value = accent
    }

    private fun read(): AccentPalette {
        val name = prefs.getString(KEY_PALETTE, null) ?: return AccentPalette.VOLT
        return runCatching { AccentPalette.valueOf(name) }.getOrDefault(AccentPalette.VOLT)
    }

    private companion object {
        const val PREFS_NAME = "darmixfit_accent"
        const val KEY_PALETTE = "accent_palette"
    }
}
