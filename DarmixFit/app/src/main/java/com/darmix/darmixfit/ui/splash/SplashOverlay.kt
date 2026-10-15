package com.darmix.darmixfit.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

const val SPLASH_DURATION_MILLIS = 1750

private val Background = DarmixPalette.Ink900
private val Steel = Color(0xFFC9CCC4)
private val SteelShade = Color(0xFF8D9188)
private val PlateColors = listOf(DarmixPalette.Volt, DarmixPalette.Ink50, DarmixPalette.Tide)
private val PlateHeights = listOf(112f, 88f, 64f)
private val PlateWidths = listOf(15f, 12f, 10f)

private fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun easeOut(x: Float): Float = 1f - (1f - x).pow(3)

private fun backOut(x: Float): Float {
    val c1 = 1.9f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            delay((SPLASH_DURATION_MILLIS * 0.62f).toLong())
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        progress.animateTo(1f, tween(SPLASH_DURATION_MILLIS, easing = LinearEasing))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .drawBehind {
                val t = progress.value
                val glow = easeOut(phase(t, 0.35f, 0.8f))
                if (glow > 0f) {
                    val center = Offset(size.width / 2f, size.height * 0.42f)
                    val radius = size.width * (0.45f + 0.25f * glow)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(DarmixPalette.Volt.copy(alpha = 0.16f * glow), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
                val stripes = 0.035f * easeOut(phase(t, 0f, 0.3f))
                val step = 22.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = stripes),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                drawBarbell(progress.value)
            }

            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Box(modifier = Modifier.clipToBounds()) {
                    Text(
                        text = "DARMIX",
                        style = MaterialTheme.typography.displayLarge,
                        color = DarmixPalette.Ink50,
                        modifier = Modifier.graphicsLayer {
                            val p = easeOut(phase(progress.value, 0.58f, 0.78f))
                            translationY = (1f - p) * size.height
                            alpha = p
                        }
                    )
                }
                Text(
                    text = "FIT",
                    style = MaterialTheme.typography.displayLarge,
                    color = DarmixPalette.Volt,
                    modifier = Modifier.graphicsLayer {
                        val p = phase(progress.value, 0.7f, 0.84f)
                        val s = 1.8f - 0.8f * backOut(p)
                        scaleX = s
                        scaleY = s
                        alpha = p
                        rotationZ = -8f * (1f - p)
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = easeOut(phase(progress.value, 0.82f, 0.96f))
                }
            ) {
                Text(
                    text = "SUPÉRATE CADA DÍA",
                    style = DarmixTextStyles.Overline,
                    color = DarmixPalette.Ink300
                )
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(3.dp)
                        .drawBehind {
                            val p = easeOut(phase(progress.value, 0.86f, 1f))
                            val w = size.width * p
                            drawRoundRect(
                                color = DarmixPalette.Volt,
                                topLeft = Offset((size.width - w) / 2f, 0f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(size.height / 2f)
                            )
                        }
                )
            }
        }

        Text(
            text = "by DARMIXISTA",
            style = DarmixTextStyles.Overline,
            color = DarmixPalette.Ink400,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = phase(progress.value, 0.88f, 1f) }
        )
    }
}

private fun DrawScope.drawBarbell(t: Float) {
    val cx = size.width / 2f
    val restY = size.height * 0.62f

    val lift = phase(t, 0.5f, 0.74f)
    val liftEase = backOut(lift)
    val cy = restY - 26.dp.toPx() * liftEase
    val flex = 12.dp.toPx() * sin(PI.toFloat() * lift) * (1f - lift * 0.35f)

    val halfBar = size.width * 0.40f
    val grow = easeOut(phase(t, 0f, 0.18f))
    val half = halfBar * grow
    if (half <= 0f) return

    val endY = cy + flex
    val bar = Path().apply {
        moveTo(cx - half, endY)
        quadraticTo(cx, cy - flex * 0.6f, cx + half, endY)
    }
    drawPath(bar, SteelShade, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
    drawPath(bar, Steel, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

    val knurl = 1.5.dp.toPx()
    for (i in -3..3) {
        if (i == 0) continue
        val kx = cx + i * 9.dp.toPx()
        drawLine(SteelShade, Offset(kx, cy + flex * 0.2f - 3.dp.toPx()), Offset(kx, cy + flex * 0.2f + 3.dp.toPx()), knurl)
    }

    val sleeveStart = halfBar * 0.56f
    val collarAlpha = phase(t, 0.12f, 0.2f)
    if (collarAlpha > 0f) {
        for (side in listOf(-1f, 1f)) {
            val x = cx + side * sleeveStart
            drawRoundRect(
                color = Steel.copy(alpha = collarAlpha),
                topLeft = Offset(x - 4.dp.toPx(), endY - 13.dp.toPx()),
                size = Size(8.dp.toPx(), 26.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }

    var offset = sleeveStart + 6.dp.toPx()
    PlateHeights.forEachIndexed { index, heightDp ->
        val start = 0.16f + index * 0.08f
        val p = phase(t, start, start + 0.16f)
        val w = PlateWidths[index].dp.toPx()
        val h = heightDp.dp.toPx()
        val target = offset + w / 2f
        if (p > 0f) {
            val travel = size.width * 0.7f
            val eased = backOut(p)
            for (side in listOf(-1f, 1f)) {
                val x = cx + side * (target + travel * (1f - eased))
                val color = PlateColors[index]
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - w / 2f, endY - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.22f),
                    topLeft = Offset(x - w / 2f + w * 0.62f, endY - h / 2f + 3.dp.toPx()),
                    size = Size(w * 0.22f, h - 6.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
        offset += w + 3.dp.toPx()
    }

    val clamp = phase(t, 0.44f, 0.52f)
    if (clamp > 0f) {
        for (side in listOf(-1f, 1f)) {
            val x = cx + side * (offset + 3.dp.toPx())
            drawRoundRect(
                color = Steel.copy(alpha = clamp),
                topLeft = Offset(x - 3.dp.toPx(), endY - 9.dp.toPx() * clamp),
                size = Size(6.dp.toPx(), 18.dp.toPx() * clamp),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }

    val tipStart = cx + halfBar
    if (grow >= 1f) {
        for (side in listOf(-1f, 1f)) {
            drawCircle(Steel, radius = 3.5.dp.toPx(), center = Offset(cx + side * (tipStart - cx), endY))
        }
    }
}
