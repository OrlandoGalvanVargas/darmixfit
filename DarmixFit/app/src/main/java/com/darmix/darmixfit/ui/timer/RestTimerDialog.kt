package com.darmix.darmixfit.ui.timer

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixfit.service.RestAutoState
import com.darmix.darmixfit.service.RestTimer
import com.darmix.darmixfit.service.TimerState
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.DarmixIconButton
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val QuickPresets = listOf(30, 60, 90, 120)
private const val STEP_SECONDS = 5
private const val FINAL_COUNTDOWN = 3

private data class RestPalette(val fill: Color, val onFill: Color, val soft: Color, val urgent: Color)

@Composable
private fun restPalette(colors: DarmixColors) = remember(colors) {
    RestPalette(
        fill = colors.tideText,
        onFill = if (colors.isDark) colors.onTide else Color.White,
        soft = colors.tideSoft,
        urgent = colors.voltText
    )
}

@Composable
fun RestTimerDialog(
    timer: TimerState,
    initialSeconds: Int,
    autoRest: RestAutoState,
    onAutoRestChange: (Boolean, Int) -> Unit,
    onStart: (Int) -> Unit,
    onExtend: () -> Unit,
    onCancelRunning: () -> Unit,
    onClose: () -> Unit
) {
    val colors = DarmixTheme.colors
    val palette = restPalette(colors)
    val running = timer.active

    Dialog(
        onDismissRequest = { if (!running) onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.screen)
                .fillMaxWidth()
                .clip(DarmixShapes.Hero)
                .background(colors.card)
                .border(1.dp, palette.fill.copy(alpha = 0.3f), DarmixShapes.Hero)
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AnimatedContent(
                targetState = running,
                transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
                label = "rest-mode"
            ) { isRunning ->
                if (isRunning) {
                    RunningContent(
                        timer = timer,
                        palette = palette,
                        autoRest = autoRest,
                        onAutoRestChange = onAutoRestChange,
                        onExtend = onExtend,
                        onSkip = onCancelRunning
                    )
                } else {
                    SetupContent(
                        initialSeconds = initialSeconds,
                        palette = palette,
                        autoRest = autoRest,
                        onAutoRestChange = onAutoRestChange,
                        onStart = onStart,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun RunningContent(
    timer: TimerState,
    palette: RestPalette,
    autoRest: RestAutoState,
    onAutoRestChange: (Boolean, Int) -> Unit,
    onExtend: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = DarmixTheme.colors
    val haptic = LocalHapticFeedback.current
    val clock = rememberFrameClock()
    val remainingSeconds by remember(timer) {
        derivedStateOf { (((timer.endAtElapsed - clock.value).coerceAtLeast(0L) + 999L) / 1000L).toInt() }
    }
    val urgent = remainingSeconds in 1..FINAL_COUNTDOWN
    val ringColor by animateColorAsState(
        targetValue = if (urgent) palette.urgent else palette.fill,
        animationSpec = Motion.snappy(),
        label = "ring-color"
    )
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds in 1..FINAL_COUNTDOWN) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Overline(text = "Recuperando", color = palette.fill)
            if (autoRest.enabled) AutoBadge(palette = palette)
        }

        Box(modifier = Modifier.size(232.dp), contentAlignment = Alignment.Center) {
            Bezel(
                progress = {
                    if (timer.totalSeconds <= 0) 0f
                    else ((timer.endAtElapsed - clock.value) / (timer.totalSeconds * 1000f)).coerceIn(0f, 1f)
                },
                ring = ringColor,
                track = colors.cardHigh,
                ticks = colors.hairlineStrong,
                modifier = Modifier.fillMaxSize()
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pulse by animateFloatAsState(
                    targetValue = if (urgent) 1.08f else 1f,
                    animationSpec = Motion.bouncy(),
                    label = "pulse"
                )
                Text(
                    text = formatTime(remainingSeconds),
                    style = DarmixTextStyles.MetricHero.copy(fontSize = DarmixTextStyles.MetricHero.fontSize * 0.86f),
                    color = if (urgent) palette.urgent else colors.textPrimary,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                )
                Text(
                    text = "de ${formatTime(timer.totalSeconds)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textTertiary
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
            DarmixButton(
                text = "+${RestTimer.EXTEND_SECONDS} s",
                onClick = onExtend,
                style = DarmixButtonStyle.Secondary,
                icon = Icons.Rounded.Add,
                height = 52.dp,
                haptic = HapticFeedbackType.ContextClick,
                modifier = Modifier.weight(1f)
            )
            DarmixButton(
                text = "Saltar",
                onClick = onSkip,
                style = DarmixButtonStyle.Ghost,
                icon = Icons.Rounded.SkipNext,
                height = 52.dp,
                haptic = HapticFeedbackType.ContextClick,
                modifier = Modifier.weight(1f)
            )
        }

        AutoToggle(
            enabled = autoRest.enabled,
            seconds = if (autoRest.enabled) autoRest.seconds else timer.totalSeconds,
            palette = palette,
            onChange = { onAutoRestChange(it, if (it) timer.totalSeconds else autoRest.seconds) }
        )
    }
}

@Composable
private fun SetupContent(
    initialSeconds: Int,
    palette: RestPalette,
    autoRest: RestAutoState,
    onAutoRestChange: (Boolean, Int) -> Unit,
    onStart: (Int) -> Unit,
    onClose: () -> Unit
) {
    val colors = DarmixTheme.colors
    var seconds by rememberSaveable {
        mutableIntStateOf(initialSeconds.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS))
    }
    fun select(value: Int) {
        seconds = value.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS)
        if (autoRest.enabled) onAutoRestChange(true, seconds)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Overline(text = "Descanso", color = palette.fill)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            DarmixIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Menos 5 segundos",
                onClick = { select(seconds - STEP_SECONDS) },
                enabled = seconds > RestTimer.MIN_SECONDS,
                size = 48.dp,
                shape = CircleShape
            )
            AnimatedContent(
                targetState = seconds,
                transitionSpec = { scaleIn(initialScale = 0.9f) + fadeIn() togetherWith fadeOut() },
                label = "setup-time",
                modifier = Modifier.width(172.dp)
            ) { value ->
                Text(
                    text = formatTime(value),
                    style = DarmixTextStyles.MetricHero,
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            DarmixIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Más 5 segundos",
                onClick = { select(seconds + STEP_SECONDS) },
                enabled = seconds < RestTimer.MAX_SECONDS,
                size = 48.dp,
                shape = CircleShape
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            QuickPresets.forEach { preset ->
                PresetChip(
                    label = if (preset < 60) "${preset}s" else formatTime(preset),
                    selected = seconds == preset,
                    palette = palette,
                    onClick = { select(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = seconds.toFloat(),
                onValueChange = { select((it / STEP_SECONDS).roundToInt() * STEP_SECONDS) },
                valueRange = RestTimer.MIN_SECONDS.toFloat()..RestTimer.MAX_SECONDS.toFloat(),
                steps = (RestTimer.MAX_SECONDS - RestTimer.MIN_SECONDS) / STEP_SECONDS - 1,
                colors = SliderDefaults.colors(
                    thumbColor = palette.fill,
                    activeTrackColor = palette.fill,
                    inactiveTrackColor = colors.cardHigh,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = formatTime(RestTimer.MIN_SECONDS), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                Spacer(Modifier.weight(1f))
                Text(text = formatTime(RestTimer.MAX_SECONDS), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            }
        }

        AutoToggle(
            enabled = autoRest.enabled,
            seconds = seconds,
            palette = palette,
            onChange = { onAutoRestChange(it, seconds) }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
            DarmixButton(
                text = "Omitir",
                onClick = onClose,
                style = DarmixButtonStyle.Ghost,
                height = 56.dp,
                haptic = null,
                modifier = Modifier.width(108.dp)
            )
            DarmixButton(
                text = "INICIAR",
                onClick = { onStart(seconds) },
                icon = Icons.Rounded.PlayArrow,
                height = 56.dp,
                containerColor = palette.fill,
                contentColor = palette.onFill,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AutoToggle(
    enabled: Boolean,
    seconds: Int,
    palette: RestPalette,
    onChange: (Boolean) -> Unit
) {
    val colors = DarmixTheme.colors
    val background by animateColorAsState(
        targetValue = if (enabled) palette.soft else colors.cardRaised,
        animationSpec = Motion.snappy(),
        label = "auto-bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(background)
            .clickable(role = Role.Switch) { onChange(!enabled) }
            .padding(start = Spacing.md, end = Spacing.sm, top = Spacing.sm + 2.dp, bottom = Spacing.sm + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (enabled) palette.fill else colors.cardHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoMode,
                contentDescription = null,
                tint = if (enabled) palette.onFill else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = "Inicio automático", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                text = if (enabled) {
                    "Cada serie inicia ${formatTime(seconds)} de descanso sola"
                } else {
                    "Activa para no tener que tocar Iniciar"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onFill,
                checkedTrackColor = palette.fill,
                checkedBorderColor = palette.fill,
                uncheckedThumbColor = colors.textTertiary,
                uncheckedTrackColor = colors.cardHigh,
                uncheckedBorderColor = colors.hairlineStrong
            )
        )
    }
}

@Composable
private fun AutoBadge(palette: RestPalette) {
    Row(
        modifier = Modifier
            .clip(DarmixShapes.Tag)
            .background(palette.fill.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Rounded.AutoMode, contentDescription = null, tint = palette.fill, modifier = Modifier.size(12.dp))
        Text(text = "AUTO", style = MaterialTheme.typography.labelSmall, color = palette.fill)
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    palette: RestPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val background by animateColorAsState(
        targetValue = if (selected) palette.fill else colors.cardHigh,
        animationSpec = Motion.snappy(),
        label = "preset-bg"
    )
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(DarmixShapes.Chip)
            .background(background)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) palette.onFill else colors.textSecondary
        )
    }
}

@Composable
private fun Bezel(
    progress: () -> Float,
    ring: Color,
    track: Color,
    ticks: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val p = progress()
        val stroke = 12.dp.toPx()
        val radius = size.minDimension / 2f - stroke / 2f
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        drawCircle(
            brush = Brush.radialGradient(listOf(ring.copy(alpha = 0.10f), Color.Transparent), center = center, radius = radius),
            radius = radius,
            center = center
        )
        drawArc(track, 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(stroke))
        if (p > 0f) {
            drawArc(ring, -90f, 360f * p, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val angle = Math.toRadians((-90f + 360f * p).toDouble())
            val head = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
            drawCircle(ring.copy(alpha = 0.35f), radius = stroke * 0.95f, center = head)
            drawCircle(Color.White, radius = stroke * 0.28f, center = head)
        }

        val inner = radius - stroke - 6.dp.toPx()
        val count = 60
        for (i in 0 until count) {
            val fraction = i / count.toFloat()
            val major = i % 5 == 0
            val length = if (major) 9.dp.toPx() else 4.dp.toPx()
            val lit = fraction < p
            rotate(fraction * 360f, center) {
                drawLine(
                    color = if (lit) ring.copy(alpha = if (major) 0.9f else 0.5f) else ticks.copy(alpha = if (major) 0.9f else 0.45f),
                    start = Offset(center.x, center.y - inner),
                    end = Offset(center.x, center.y - inner + length),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun rememberFrameClock(): State<Long> {
    val now = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { now.longValue = SystemClock.elapsedRealtime() }
        }
    }
    return now
}

private fun formatTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
