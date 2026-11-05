package com.darmix.darmixfit.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.darmix.darmixfit.ui.theme.BodyFamily
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
internal fun ReactorCore(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.reactor
    val progress = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f
    val drive = ReactorDrive.of(completed, total)
    val motion = remember { ReactorMotion() }
    val feedback = rememberSpinFeedback()
    val sensors = rememberLiquidSensors()
    val frame = remember { mutableLongStateOf(0L) }
    val currentDrive by rememberUpdatedState(drive)
    val currentProgress by rememberUpdatedState(progress)
    val dark = DarmixTheme.colors.isDark
    val painter = remember(colors, dark) { ReactorPainter(colors, dark) }

    LaunchedEffect(motion, feedback) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceIn(0f, MAX_FRAME_SECONDS)
                last = now
                motion.step(dt, currentDrive, currentProgress, sensors, feedback)
                frame.longValue = now
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.pointerInput(motion, feedback) { spinGesture(motion, feedback) },
        contentAlignment = Alignment.Center
    ) {
        val sizePx = constraints.maxWidth
        val body = remember(sizePx, colors) { buildReactorBody(sizePx, colors) }
        Canvas(modifier = Modifier.fillMaxSize()) {
            frame.longValue
            with(painter) { drawReactor(motion, currentDrive, currentProgress, body) }
        }
        val density = LocalDensity.current
        val numberSize = with(density) { (maxWidth.toPx() * NUMBER_RATIO).toSp() }
        val labelSize = with(density) { (maxWidth.toPx() * LABEL_RATIO).toSp() }
        val shadowNumber = with(density) { Shadow(Color.Black.copy(alpha = 0.85f), Offset(0f, 2.dp.toPx()), 12.dp.toPx()) }
        val shadowLabel = with(density) { Shadow(Color.Black.copy(alpha = 0.95f), Offset(0f, 1.dp.toPx()), 8.dp.toPx()) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = completed.coerceAtLeast(0).toString().padStart(2, '0'),
                style = TextStyle(
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = numberSize,
                    lineHeight = numberSize,
                    color = colors.number,
                    shadow = shadowNumber,
                    fontFeatureSettings = "tnum",
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
                )
            )
            Text(
                text = if (total > 0 && completed >= total) "COMPLETO" else "DE $total SERIES",
                style = TextStyle(
                    fontFamily = BodyFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = labelSize,
                    letterSpacing = 0.12.em,
                    color = colors.number,
                    shadow = shadowLabel
                )
            )
        }
    }
}

private suspend fun PointerInputScope.spinGesture(motion: ReactorMotion, feedback: ReactorSpinFeedback) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val unit = size.width / ReactorGeometry.VIEWBOX
        val center = Offset(size.width / 2f, size.height / 2f)
        val distance = hypot(down.position.x - center.x, down.position.y - center.y) / unit
        if (distance < ReactorGeometry.GRAB_INNER || distance > ReactorGeometry.GRAB_OUTER) return@awaitEachGesture
        down.consume()
        motion.beginDrag()
        var previous = angleOf(down.position, center)
        var lastMove = down.uptimeMillis
        var releasedAt = lastMove
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                releasedAt = change.uptimeMillis
                change.consume()
                break
            }
            val current = angleOf(change.position, center)
            var delta = current - previous
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            if (delta != 0f) {
                val dt = (change.uptimeMillis - lastMove).coerceAtLeast(1L) / 1000f
                motion.drag(delta, dt, feedback)
                lastMove = change.uptimeMillis
                previous = current
            }
            change.consume()
        }
        motion.endDrag(stale = releasedAt - lastMove > STALE_RELEASE_MS)
    }
}

private fun angleOf(position: Offset, center: Offset): Float =
    Math.toDegrees(atan2(position.y - center.y, position.x - center.x).toDouble()).toFloat()

private const val MAX_FRAME_SECONDS = 0.05f
private const val STALE_RELEASE_MS = 90L
private const val NUMBER_RATIO = 0.1675f
private const val LABEL_RATIO = 0.0382f
