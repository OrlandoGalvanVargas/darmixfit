package com.darmix.darmixfit.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ToastShape = RoundedCornerShape(24.dp)
private const val DISMISS_FRACTION = 0.32f

data class DarmixToastVisuals(
    override val message: String,
    val type: MessageType = MessageType.INFO,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

suspend fun SnackbarHostState.showToast(message: UiMessage) {
    showSnackbar(DarmixToastVisuals(message = message.text, type = message.type))
}

suspend fun SnackbarHostState.showToast(text: String, type: MessageType = MessageType.INFO) {
    showSnackbar(DarmixToastVisuals(message = text, type = type))
}

@Composable
fun DarmixToastHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val current = hostState.currentSnackbarData
    var last by remember { mutableStateOf<SnackbarData?>(null) }
    LaunchedEffect(current) {
        if (current != null) last = current
    }
    AnimatedVisibility(
        visible = current != null,
        enter = slideInVertically(spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } +
            fadeIn(tween(Motion.Quick)) +
            scaleIn(spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.86f),
        exit = slideOutVertically(tween(Motion.Standard, easing = Motion.Accelerate)) { -it * 2 } +
            fadeOut(tween(Motion.Standard)),
        modifier = modifier
    ) {
        val data = current ?: last
        if (data != null) {
            key(data) { DarmixToast(data = data) }
        }
    }
}

@Composable
private fun DarmixToast(data: SnackbarData) {
    val colors = DarmixTheme.colors
    val visuals = data.visuals
    val type = (visuals as? DarmixToastVisuals)?.type ?: MessageType.INFO
    val (accent, label) = when (type) {
        MessageType.SUCCESS -> colors.voltText to "Listo"
        MessageType.ERROR -> colors.emberText to "Ojo"
        MessageType.INFO -> colors.tideText to "Aviso"
    }
    val onAccent = if (accent.luminance() > 0.5f) DarmixPalette.Ink950 else Color.White
    val durationMs = when (visuals.duration) {
        SnackbarDuration.Short -> 3400
        SnackbarDuration.Long -> 6000
        SnackbarDuration.Indefinite -> 10000
    }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val timer = remember { Animatable(1f) }
    val glyph = remember { Animatable(0f) }
    val dragX = remember { Animatable(0f) }
    var width by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(if (type == MessageType.ERROR) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
        glyph.animateTo(1f, tween(480, delayMillis = 140, easing = Motion.Emphasized))
    }
    LaunchedEffect(Unit) {
        timer.animateTo(0f, tween(durationMs, easing = LinearEasing))
        data.dismiss()
    }

    fun resume() {
        scope.launch { dragX.animateTo(0f, Motion.snappy()) }
        scope.launch {
            timer.animateTo(0f, tween((timer.value * durationMs).toInt().coerceAtLeast(300), easing = LinearEasing))
            data.dismiss()
        }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.screen)
            .fillMaxWidth()
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationX = dragX.value
                rotationZ = dragX.value / 70f
                alpha = 1f - (abs(dragX.value) / size.width.coerceAtLeast(1f)).coerceIn(0f, 0.7f)
            }
            .shadow(20.dp, ToastShape, ambientColor = accent, spotColor = accent)
            .clip(ToastShape)
            .background(colors.cardRaised)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = if (colors.isDark) 0.22f else 0.16f), Color.Transparent),
                        center = Offset(30.dp.toPx(), size.height / 2f),
                        radius = size.height * 1.4f
                    ),
                    radius = size.height * 1.4f,
                    center = Offset(30.dp.toPx(), size.height / 2f)
                )
                val bar = 3.dp.toPx()
                drawRect(
                    color = accent,
                    topLeft = Offset(0f, size.height - bar),
                    size = Size(size.width * timer.value, bar)
                )
            }
            .border(1.dp, accent.copy(alpha = 0.35f), ToastShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { scope.launch { timer.stop() } },
                    onDragEnd = {
                        if (abs(dragX.value) > width * DISMISS_FRACTION) {
                            scope.launch {
                                dragX.animateTo(if (dragX.value > 0) width * 1.2f else -width * 1.2f, tween(Motion.Quick))
                                data.dismiss()
                            }
                        } else {
                            resume()
                        }
                    },
                    onDragCancel = { resume() },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch { dragX.snapTo(dragX.value + amount) }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { data.dismiss() }
            .padding(start = 10.dp, end = Spacing.lg, top = 10.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            ToastGlyph(type = type, progress = { glyph.value }, color = onAccent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Overline(text = label, color = accent)
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun ToastGlyph(type: MessageType, progress: () -> Float, color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val p = progress()
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (type) {
            MessageType.SUCCESS -> {
                val path = Path().apply {
                    moveTo(w * 0.14f, h * 0.54f)
                    lineTo(w * 0.40f, h * 0.80f)
                    lineTo(w * 0.88f, h * 0.24f)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * p, partial, true)
                drawPath(partial, color, style = stroke)
            }

            MessageType.ERROR -> {
                val first = (p * 2f).coerceIn(0f, 1f)
                val second = (p * 2f - 1f).coerceIn(0f, 1f)
                val a = Offset(w * 0.2f, h * 0.2f)
                val b = Offset(w * 0.8f, h * 0.8f)
                val c = Offset(w * 0.8f, h * 0.2f)
                val d = Offset(w * 0.2f, h * 0.8f)
                if (first > 0f) drawLine(color, a, a + (b - a) * first, stroke.width, StrokeCap.Round)
                if (second > 0f) drawLine(color, c, c + (d - c) * second, stroke.width, StrokeCap.Round)
            }

            MessageType.INFO -> {
                drawCircle(color, radius = 1.8.dp.toPx() * p, center = Offset(w / 2f, h * 0.18f))
                val top = Offset(w / 2f, h * 0.42f)
                if (p > 0f) drawLine(color, top, top + Offset(0f, h * 0.44f * p), stroke.width, StrokeCap.Round)
            }
        }
    }
}
