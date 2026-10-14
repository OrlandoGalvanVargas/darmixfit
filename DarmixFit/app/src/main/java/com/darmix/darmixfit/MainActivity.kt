package com.darmix.darmixfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixfit.ui.FitFlowNavHost
import com.darmix.darmixfit.ui.permissions.SetupGate
import com.darmix.darmixfit.ui.splash.SplashOverlay
import com.darmix.darmixfit.ui.theme.FitFLowTheme
import com.darmix.darmixfit.ui.theme.LocalThemeToggle
import com.darmix.darmixfit.ui.theme.ThemePreference
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // 1. Splash nativo (Android 12+ y fallback). Evita el flash blanco.
        val splashScreen = installSplashScreen()

        // 2. Mientras el contenido aún no está listo, mantenemos el splash
        //    nativo. Cuando esté, lo soltamos y entra el splash animado.
        var contentReady = false
        splashScreen.setKeepOnScreenCondition { !contentReady }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePreference = ThemePreference(this)

        setContent {
            val themeMode by themePreference.mode.collectAsStateWithLifecycle()

            FitFLowTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalThemeToggle provides { themePreference.cycle() }
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        // Soltamos el splash nativo en cuanto Compose está vivo.
                        contentReady = true
                        // Splash animado dura ~1.1s.
                        delay(1100)
                        showSplash = false
                    }

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(400),
                        label = "splash-crossfade",
                    ) { isSplash ->
                        if (isSplash) {
                            SplashOverlay()
                        } else {
                            SetupGate {
                                FitFlowNavHost()
                            }
                        }
                    }
                }
            }
        }
    }
}