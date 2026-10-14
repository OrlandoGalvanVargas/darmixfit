package com.darmix.darmixfit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LimeAccent,
    onPrimary = Color(0xFF10120A),
    primaryContainer = LimeAccentContainerDark,
    onPrimaryContainer = LimeAccent,

    secondary = AmberAccent,
    onSecondary = Color(0xFF201400),
    secondaryContainer = AmberAccentContainerDark,
    onSecondaryContainer = AmberAccent,

    tertiary = SuccessGreen,
    onTertiary = Color(0xFF04210F),

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDarkElevated,
    surfaceContainerHighest = SurfaceDarkHigh,

    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder,

    error = DangerRed,
    onError = Color(0xFF2B0000),
)

private val LightColorScheme = lightColorScheme(
    primary = LimeAccentDim,
    onPrimary = Color.White,
    primaryContainer = LimeAccentContainerLight,
    onPrimaryContainer = Color(0xFF223200),

    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberAccentContainerLight,
    onSecondaryContainer = Color(0xFF3A2900),

    tertiary = SuccessGreen,
    onTertiary = Color.White,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLightElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLightElevated,
    surfaceContainerHighest = SurfaceLightElevated,

    outline = SurfaceLightBorder,
    outlineVariant = SurfaceLightBorder,

    error = DangerRed,
    onError = Color.White,
)

private val FitFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FitFLowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // El modo efectivo depende del themeMode + sistema.
    val effectiveDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        effectiveDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FitFlowShapes,
        content = content
    )
}