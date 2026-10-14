package com.darmix.darmixfit.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Splash animado. Se muestra tras el splash nativo y hace fade-in de:
 *  · Anillo lima que se dibuja progresivamente
 *  · Monograma "F" que aparece con fade + scale
 *  · Wordmark "FitFlow" retardado
 */
@Composable
fun SplashOverlay(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Progreso global 0..1 (controla el fade general de la capa)
    val reveal = remember { Animatable(0f) }
    // Progreso del anillo 0..1
    val ringProgress = remember { Animatable(0f) }
    // Escala del monograma 0.7 → 1.0
    val markScale = remember { Animatable(0.7f) }
    // Opacidad del wordmark
    val wordmarkAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        ringProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        markScale.animateTo(1f, tween(600, delayMillis = 350, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        wordmarkAlpha.animateTo(1f, tween(400, delayMillis = 700))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .graphicsLayer { alpha = reveal.value },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                // ── Anillo + F (mismo lenguaje que el icono de la app) ──
                Canvas(modifier = Modifier.size(160.dp)) {
                    val c = center
                    val r = size.minDimension / 2f
                    val stroke = 3.dp.toPx()

                    // Anillo lima que se dibuja
                    drawArc(
                        color = primary,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.value,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            c.x - r + stroke / 2,
                            c.y - r + stroke / 2,
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            (r - stroke / 2) * 2,
                            (r - stroke / 2) * 2,
                        ),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                // Monograma F (escala + fade)
                Canvas(
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = markScale.value
                            scaleY = markScale.value
                            alpha = markScale.value.coerceIn(0f, 1f)
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val stroke = 12.dp.toPx()
                    val pad = stroke / 2

                    // Tallo + brazo superior (una sola pasada)
                    val topStem = Path().apply {
                        moveTo(pad, h - pad)
                        lineTo(pad, pad)
                        lineTo(w - pad, pad)
                    }
                    drawPath(
                        path = topStem,
                        color = primary,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    // Brazo medio
                    val midArm = Path().apply {
                        moveTo(pad, h / 2f)
                        lineTo(w * 0.72f, h / 2f)
                    }
                    drawPath(
                        path = midArm,
                        color = primary,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Wordmark
            Text(
                text = "FitFlow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { alpha = wordmarkAlpha.value },
            )
        }

        // Micro-caption abajo del todo
        Text(
            text = "ENTRENA CON PROPÓSITO",
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = wordmarkAlpha.value * 0.9f }
                .padding(bottom = 48.dp),
        )
    }
}