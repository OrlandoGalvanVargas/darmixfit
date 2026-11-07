package com.darmix.darmixfit.ui.splash

import android.app.Activity
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.darmix.darmixfit.ui.theme.DisplayFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

const val SPLASH_DURATION_MILLIS = 1800

private val Background = Color(0xFF0A0C08)
private val Volt = Color(0xFFC6FF3D)
private val VoltBright = Color(0xFFDBFF7A)
private val VoltCore = Color(0xFFF2FFC9)
private val VoltDeep = Color(0xFF7FAF12)
private val SegmentOff = Color(0xFF1A1E16)
private val SegmentEdge = Color(0xFF262B21)
private val Hairline = Color(0xFF2A2F26)
private val HairlineStrong = Color(0xFF3A4035)
private val CoreTop = Color(0xFF1C2312)
private val CoreBottom = Color(0xFF0B0D09)
private val TextPrimary = Color(0xFFF4F6F1)
private val TextSecondary = Color(0xFF9AA095)
private val TextTertiary = Color(0xFF6B7266)

private const val SEGMENTS = 24
private const val TICKS = 72
private const val REACTOR_UNITS = 300f
private val TagBarShape = CutCornerShape(2.dp)

private fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun easeOut(x: Float): Float = 1f - (1f - x).pow(3)

@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }

    DarkSystemBars()

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_DURATION_MILLIS, easing = LinearEasing))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(300.dp)) {
                drawReactor(progress.value)
            }

            Spacer(Modifier.height(10.dp))

            Wordmark(progress = { progress.value })

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.graphicsLayer {
                    val p = easeOut(phase(progress.value, 0.8f, 0.95f))
                    alpha = p
                    scaleX = 1.12f - 0.12f * p
                }
            ) {
                TagBar()
                Text(
                    text = "SIGUE ESFORZÁNDOTE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 5.sp
                    ),
                    color = TextSecondary
                )
                TagBar()
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .graphicsLayer { alpha = phase(progress.value, 0.86f, 1f) }
        ) {
            val footer = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.6.sp
            )
            Text(text = "POR ", style = footer, color = TextTertiary)
            Text(text = "DARMIXISTA", style = footer, color = TextSecondary)
        }
    }
}

@Composable
private fun DarkSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                previousStatus?.let { controller.isAppearanceLightStatusBars = it }
                previousNavigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

@Composable
private fun TagBar() {
    Box(
        modifier = Modifier
            .width(18.dp)
            .height(4.dp)
            .background(HairlineStrong, TagBarShape)
    )
}

@Composable
private fun Wordmark(progress: () -> Float) {
    val wordStyle = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 60.sp,
        lineHeight = 60.sp,
        letterSpacing = 1.sp
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .drawBehind { drawSlit(progress()) },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.drawWithContent {
                val open = easeOut(phase(progress(), 0.67f, 0.82f))
                val inset = size.height / 2f * (1f - open)
                clipRect(top = inset, bottom = size.height - inset) {
                    this@drawWithContent.drawContent()
                }
            }
        ) {
            Text(text = "DARMIX", style = wordStyle, color = TextPrimary)
            FitText(style = wordStyle, progress = progress)
        }
    }
}

@Composable
private fun FitText(style: TextStyle, progress: () -> Float) {
    val p = phase(progress(), 0.74f, 0.86f)
    Text(
        text = "FIT",
        style = style.copy(
            shadow = Shadow(color = Volt.copy(alpha = 0.5f * p), blurRadius = 36f)
        ),
        color = lerp(TextPrimary, Volt, p)
    )
}

private fun DrawScope.drawSlit(t: Float) {
    val grow = easeOut(phase(t, 0.56f, 0.67f))
    if (t < 0.56f || grow <= 0f) return
    val open = easeOut(phase(t, 0.67f, 0.82f))
    val fullWidth = 238.dp.toPx()
    val width = fullWidth * grow
    val left = (size.width - width) / 2f
    val center = size.height / 2f
    val travel = 38.dp.toPx() * open
    val thickness = 2.dp.toPx()
    val topAlpha = 1f - phase(t, 0.82f, 0.95f)
    drawGlowLine(left, width, center - travel, thickness, topAlpha)
    drawGlowLine(left, width, center + travel, thickness, 1f)
}

private fun DrawScope.drawGlowLine(left: Float, width: Float, y: Float, thickness: Float, alpha: Float) {
    if (alpha <= 0f) return
    drawRect(
        color = Volt.copy(alpha = 0.18f * alpha),
        topLeft = Offset(left - 4.dp.toPx(), y - thickness * 3f),
        size = Size(width + 8.dp.toPx(), thickness * 6f)
    )
    drawRect(
        color = Volt.copy(alpha = alpha),
        topLeft = Offset(left, y - thickness / 2f),
        size = Size(width, thickness)
    )
}

private fun DrawScope.drawReactor(t: Float) {
    val u = size.minDimension / REACTOR_UNITS
    val c = center

    val glowIn = phase(t, 0.48f, 0.62f)
    val glowSettle = phase(t, 0.62f, 0.9f)
    val glowAlpha = if (t < 0.62f) easeOut(glowIn) else 1f - 0.25f * glowSettle
    val glowScale = if (t < 0.62f) 0.7f + 0.38f * easeOut(glowIn) else 1.08f - 0.08f * glowSettle
    if (glowAlpha > 0f) {
        val r = 210f * u * glowScale
        drawCircle(
            brush = Brush.radialGradient(
                0f to Volt.copy(alpha = 0.16f * glowAlpha),
                0.45f to Volt.copy(alpha = 0.05f * glowAlpha),
                0.7f to Color.Transparent,
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )
    }

    val bracketP = easeOut(phase(t, 0f, 0.3f))
    if (bracketP > 0f) {
        val half = 136f * u * (1.4f - 0.4f * bracketP)
        val arm = 22f * u
        val stroke = 2f * u
        val color = Volt.copy(alpha = bracketP)
        for (sx in listOf(-1f, 1f)) {
            for (sy in listOf(-1f, 1f)) {
                val corner = Offset(c.x + sx * half, c.y + sy * half)
                drawLine(color, corner, Offset(corner.x - sx * arm, corner.y), stroke, StrokeCap.Square)
                drawLine(color, corner, Offset(corner.x, corner.y - sy * arm), stroke, StrokeCap.Square)
            }
        }
    }

    val border = easeOut(phase(t, 0f, 0.4f))
    if (border > 0f) {
        drawArc(
            color = HairlineStrong,
            startAngle = -90f,
            sweepAngle = 360f * border,
            useCenter = false,
            topLeft = Offset(c.x - 128f * u, c.y - 128f * u),
            size = Size(256f * u, 256f * u),
            style = Stroke(width = 2f * u)
        )
        drawArc(
            color = Volt.copy(alpha = 0.22f),
            startAngle = -90f,
            sweepAngle = 360f * border,
            useCenter = false,
            topLeft = Offset(c.x - 132f * u, c.y - 132f * u),
            size = Size(264f * u, 264f * u),
            style = Stroke(width = 1f * u)
        )
    }

    val tickP = easeOut(phase(t, 0f, 0.42f))
    if (tickP > 0f) {
        rotate(degrees = -40f * (1f - tickP), pivot = c) {
            for (k in 0 until TICKS) {
                val angle = (k * 5f - 90f) * PI.toFloat() / 180f
                val major = k % 6 == 0
                val inner = 108f * u
                val outer = inner + (if (major) 9f else 4f) * u
                val color = if (k % 18 == 0) Volt else HairlineStrong
                drawLine(
                    color = color.copy(alpha = tickP),
                    start = Offset(c.x + inner * cos(angle), c.y + inner * sin(angle)),
                    end = Offset(c.x + outer * cos(angle), c.y + outer * sin(angle)),
                    strokeWidth = (if (major) 1.6f else 1f) * u
                )
            }
        }
    }

    drawCircle(Hairline, radius = 74f * u, center = c, style = Stroke(1f * u))
    drawCircle(Hairline, radius = 102f * u, center = c, style = Stroke(1f * u))

    val ringRadius = 90f * u
    val ringTopLeft = Offset(c.x - ringRadius, c.y - ringRadius)
    val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
    val litBrush = Brush.linearGradient(
        listOf(VoltBright, Volt),
        start = Offset(c.x - ringRadius, c.y - ringRadius),
        end = Offset(c.x + ringRadius, c.y + ringRadius)
    )
    for (i in 0 until SEGMENTS) {
        val start = i * 15f + 2f - 90f
        val sweep = 11f
        drawArc(SegmentEdge, start - 0.3f, sweep + 0.6f, false, ringTopLeft, ringSize, style = Stroke(width = 17f * u))
        drawArc(SegmentOff, start, sweep, false, ringTopLeft, ringSize, style = Stroke(width = 15f * u))
        val lit = easeOut(phase(t, 0.12f + i * 0.0145f, 0.17f + i * 0.0145f))
        if (lit > 0f) {
            drawArc(Volt.copy(alpha = 0.22f * lit), start - 1f, sweep + 2f, false, ringTopLeft, ringSize, style = Stroke(width = 24f * u))
            drawArc(litBrush, start, sweep, false, ringTopLeft, ringSize, alpha = lit, style = Stroke(width = 16f * u))
        }
    }

    if (t >= 0.5f) {
        val shock = phase(t, 0.5f, 0.8f)
        val alpha = if (t < 0.52f) 0.9f * phase(t, 0.5f, 0.52f) else 0.9f * (1f - shock)
        if (alpha > 0f) {
            drawCircle(
                color = Volt.copy(alpha = alpha),
                radius = 90f * u * (1f + 0.9f * easeOut(shock)),
                center = c,
                style = Stroke(width = 2f * u)
            )
        }
    }

    drawCircle(Hairline, radius = 36f * u, center = c)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(CoreTop, CoreBottom),
            center = Offset(c.x, c.y - 7f * u),
            radius = 35f * u
        ),
        radius = 35f * u,
        center = c
    )

    val coreIn = phase(t, 0.47f, 0.55f)
    if (coreIn > 0f) {
        val coreScale = if (t < 0.55f) 0.2f + 1.15f * easeOut(coreIn) else 1.35f - 0.35f * easeOut(phase(t, 0.55f, 0.7f))
        withTransform({ scale(coreScale, coreScale, c) }) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to VoltCore,
                    0.55f to Volt,
                    1f to VoltDeep,
                    center = c,
                    radius = 13f * u
                ),
                radius = 13f * u,
                center = c,
                alpha = coreIn
            )
        }
    }

    val flash = when {
        t < 0.5f -> 0f
        t < 0.53f -> phase(t, 0.5f, 0.53f)
        else -> 1f - phase(t, 0.53f, 0.72f)
    }
    if (flash > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFE6FFAA).copy(alpha = 0.9f * flash),
                0.45f to Volt.copy(alpha = 0.25f * flash),
                0.7f to Color.Transparent,
                center = c,
                radius = 60f * u
            ),
            radius = 60f * u,
            center = c
        )
    }
}
