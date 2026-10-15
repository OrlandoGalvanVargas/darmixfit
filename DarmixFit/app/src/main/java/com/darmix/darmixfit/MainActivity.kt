package com.darmix.darmixfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixfit.service.SessionNotifier
import com.darmix.darmixfit.ui.DarmixNavHost
import com.darmix.darmixfit.ui.permissions.SetupGate
import com.darmix.darmixfit.ui.splash.SPLASH_DURATION_MILLIS
import com.darmix.darmixfit.ui.splash.SplashOverlay
import com.darmix.darmixfit.ui.theme.DarmixFitTheme
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.LocalThemeMode
import com.darmix.darmixfit.ui.theme.LocalThemeToggle
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.ThemeMode
import com.darmix.darmixfit.ui.theme.ThemePreference
import com.darmix.darmixfit.ui.update.UpdatePrompt
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !contentReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePreference = ThemePreference(this)
        setContent {
            val themeMode by themePreference.mode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val toggleTheme: () -> Unit = remember(effectiveDark) { { themePreference.toggle(effectiveDark) } }

            DarmixFitTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalThemeToggle provides toggleTheme,
                    LocalThemeMode provides themeMode
                ) {
                    var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }

                    LaunchedEffect(Unit) {
                        contentReady = true
                        if (showSplash) {
                            delay(SPLASH_DURATION_MILLIS + SPLASH_HOLD_MILLIS)
                            showSplash = false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarmixTheme.colors.canvas)
                    ) {
                        Crossfade(
                            targetState = showSplash,
                            animationSpec = remember { Motion.enter<Float>() },
                            label = "splash"
                        ) { isSplash ->
                            if (isSplash) {
                                SplashOverlay()
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SetupGate {
                                        DarmixNavHost()
                                    }
                                    UpdatePrompt()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SessionNotifier.onAppVisible(this)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) SessionNotifier.onAppHidden(this)
    }

    private companion object {
        const val SPLASH_HOLD_MILLIS = 250L
    }
}
