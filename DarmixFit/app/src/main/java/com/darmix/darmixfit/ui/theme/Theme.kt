package com.darmix.darmixfit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private fun DarmixColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = if (isDark) volt else DarmixPalette.VoltDeep,
        onPrimary = if (isDark) onVolt else Color.White,
        primaryContainer = voltSoft,
        onPrimaryContainer = voltText,
        inversePrimary = if (isDark) DarmixPalette.VoltDeep else volt,
        secondary = tide,
        onSecondary = onTide,
        secondaryContainer = tideSoft,
        onSecondaryContainer = tideText,
        tertiary = leaf,
        onTertiary = DarmixPalette.Ink950,
        tertiaryContainer = leafSoft,
        onTertiaryContainer = leafText,
        background = canvas,
        onBackground = textPrimary,
        surface = canvas,
        onSurface = textPrimary,
        surfaceVariant = cardRaised,
        onSurfaceVariant = textSecondary,
        surfaceTint = canvas,
        surfaceDim = canvasDeep,
        surfaceBright = cardHigh,
        surfaceContainerLowest = canvasDeep,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = cardRaised,
        surfaceContainerHighest = cardHigh,
        inverseSurface = textPrimary,
        inverseOnSurface = canvas,
        outline = hairlineStrong,
        outlineVariant = hairline,
        error = ember,
        onError = onEmber,
        errorContainer = emberSoft,
        onErrorContainer = emberText,
        scrim = scrim
    )
}

private val DarkScheme = DarkDarmixColors.toColorScheme()
private val LightScheme = LightDarmixColors.toColorScheme()

private val DarmixMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DarmixFitTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val darmixColors = if (dark) DarkDarmixColors else LightDarmixColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalDarmixColors provides darmixColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = Typography,
            shapes = DarmixMaterialShapes,
            content = content
        )
    }
}

object DarmixTheme {
    val colors: DarmixColors
        @Composable @ReadOnlyComposable
        get() = LocalDarmixColors.current
}
