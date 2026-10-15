package com.darmix.darmixfit.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Immutable
data class DarmixColors(
    val isDark: Boolean,
    val canvas: Color,
    val canvasDeep: Color,
    val card: Color,
    val cardRaised: Color,
    val cardHigh: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val volt: Color,
    val onVolt: Color,
    val voltSoft: Color,
    val voltText: Color,
    val gold: Color,
    val onGold: Color,
    val goldSoft: Color,
    val goldText: Color,
    val tide: Color,
    val onTide: Color,
    val tideSoft: Color,
    val tideText: Color,
    val ember: Color,
    val onEmber: Color,
    val emberSoft: Color,
    val emberText: Color,
    val leaf: Color,
    val leafSoft: Color,
    val leafText: Color,
    val scrim: Color
) {
    val voltGlow: Brush
        get() = Brush.radialGradient(
            listOf(volt.copy(alpha = if (isDark) 0.22f else 0.30f), Color.Transparent)
        )

    val heroSheen: Brush
        get() = Brush.linearGradient(
            listOf(cardRaised, card, canvasDeep)
        )

    val voltRamp: Brush
        get() = Brush.horizontalGradient(listOf(volt, if (isDark) DarmixPalette.VoltBright else DarmixPalette.VoltDeep))

    val fireRamp: Brush
        get() = Brush.horizontalGradient(listOf(gold, ember))
}

val DarkDarmixColors = DarmixColors(
    isDark = true,
    canvas = DarmixPalette.Ink900,
    canvasDeep = DarmixPalette.Ink950,
    card = DarmixPalette.Ink850,
    cardRaised = DarmixPalette.Ink800,
    cardHigh = DarmixPalette.Ink750,
    hairline = DarmixPalette.Ink700,
    hairlineStrong = DarmixPalette.Ink600,
    textPrimary = DarmixPalette.Ink50,
    textSecondary = DarmixPalette.Ink300,
    textTertiary = DarmixPalette.Ink400,
    volt = DarmixPalette.Volt,
    onVolt = DarmixPalette.VoltInk,
    voltSoft = DarmixPalette.VoltShadow,
    voltText = DarmixPalette.Volt,
    gold = DarmixPalette.Gold,
    onGold = DarmixPalette.GoldInk,
    goldSoft = DarmixPalette.GoldShadow,
    goldText = DarmixPalette.Gold,
    tide = DarmixPalette.Tide,
    onTide = DarmixPalette.TideInk,
    tideSoft = DarmixPalette.TideShadow,
    tideText = DarmixPalette.Tide,
    ember = DarmixPalette.Ember,
    onEmber = DarmixPalette.EmberInk,
    emberSoft = DarmixPalette.EmberShadow,
    emberText = DarmixPalette.Ember,
    leaf = DarmixPalette.Leaf,
    leafSoft = DarmixPalette.LeafShadow,
    leafText = DarmixPalette.Leaf,
    scrim = DarmixPalette.Scrim
)

val LightDarmixColors = DarmixColors(
    isDark = false,
    canvas = DarmixPalette.Chalk,
    canvasDeep = DarmixPalette.ChalkElevated,
    card = DarmixPalette.ChalkSurface,
    cardRaised = DarmixPalette.ChalkElevated,
    cardHigh = DarmixPalette.ChalkHigh,
    hairline = DarmixPalette.ChalkBorder,
    hairlineStrong = DarmixPalette.ChalkBorderStrong,
    textPrimary = DarmixPalette.Graphite,
    textSecondary = DarmixPalette.GraphiteSoft,
    textTertiary = DarmixPalette.GraphiteMuted,
    volt = DarmixPalette.Volt,
    onVolt = DarmixPalette.VoltInk,
    voltSoft = DarmixPalette.VoltMist,
    voltText = DarmixPalette.VoltDeep,
    gold = DarmixPalette.Gold,
    onGold = DarmixPalette.GoldInk,
    goldSoft = DarmixPalette.GoldMist,
    goldText = DarmixPalette.GoldDeep,
    tide = DarmixPalette.Tide,
    onTide = DarmixPalette.TideInk,
    tideSoft = DarmixPalette.TideMist,
    tideText = DarmixPalette.TideDeep,
    ember = DarmixPalette.Ember,
    onEmber = DarmixPalette.EmberInk,
    emberSoft = DarmixPalette.EmberMist,
    emberText = DarmixPalette.EmberDeep,
    leaf = DarmixPalette.Leaf,
    leafSoft = DarmixPalette.LeafMist,
    leafText = DarmixPalette.LeafDeep,
    scrim = DarmixPalette.Scrim
)

val LocalDarmixColors = staticCompositionLocalOf { DarkDarmixColors }

object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
    val screen = 20.dp
}

object DarmixShapes {
    val Chip = RoundedCornerShape(10.dp)
    val Control = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(30.dp)
    val Sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val Pill = RoundedCornerShape(50)
    val Plate = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
    val Tag = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
}

object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val Quick = 140
    const val Standard = 260
    const val Slow = 420

    fun <T> snappy() = spring<T>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    fun <T> gentle() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    fun <T> enter() = tween<T>(Standard, easing = Decelerate)
    fun <T> exit() = tween<T>(Quick, easing = Accelerate)
    val slide = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
}
