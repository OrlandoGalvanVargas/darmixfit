package com.darmix.darmixfit.ui.timer

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixfit.service.RestAutoState
import com.darmix.darmixfit.service.RestTimer
import com.darmix.darmixfit.service.TimerState
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val QuickPresets = listOf(30, 60, 90, 120)
private const val STEP_SECONDS = 5
private const val NUDGE_SECONDS = 15
private const val FINAL_COUNTDOWN = 3
private const val GAUGE_START = 135f
private const val GAUGE_SWEEP = 270f
private const val GAUGE_TICKS = 91
private val TickSpacing = 5.dp

private class RestPalette(
    val fill: Color,
    val onFill: Color,
    val deep: Color,
    val hot: Color,
    val dark: Boolean
)

private fun restPalette(colors: DarmixColors): RestPalette {
    val fill = colors.voltText
    return RestPalette(
        fill = fill,
        onFill = if (colors.isDark) colors.onVolt else Color.White,
        deep = lerp(fill, Color.Black, if (colors.isDark) 0.62f else 0.35f),
        hot = lerp(fill, Color.White, if (colors.isDark) 0.72f else 0.25f),
        dark = colors.isDark
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
    val palette = remember(colors) { restPalette(colors) }
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
                .padding(horizontal = Spacing.screen, vertical = Spacing.xl)
                .fillMaxWidth()
                .restFrame(palette, colors)
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
    val clock = rememberFrameClock()
    val remainingSeconds by remember(timer) {
        derivedStateOf { (((timer.endAtElapsed - clock.value).coerceAtLeast(0L) + 999L) / 1000L).toInt() }
    }
    val charge: () -> Float = {
        if (timer.totalSeconds <= 0) 0f
        else (1f - (timer.endAtElapsed - clock.value) / (timer.totalSeconds * 1000f)).coerceIn(0f, 1f)
    }
    val percent by remember(timer) { derivedStateOf { (charge() * 100f).toInt() } }
    val urgent = remainingSeconds in 1..FINAL_COUNTDOWN
    val pulse by animateFloatAsState(
        targetValue = if (urgent) 1.08f else 1f,
        animationSpec = Motion.bouncy(),
        label = "pulse"
    )
    val numberColor by animateColorAsState(
        targetValue = if (urgent) palette.fill else colors.textPrimary,
        animationSpec = Motion.snappy(),
        label = "number-color"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RestTitle(text = "RECUPERANDO", color = palette.fill)
        Spacer(Modifier.height(18.dp))
        Box(modifier = Modifier.size(270.dp), contentAlignment = Alignment.Center) {
            TurboGauge(charge = charge, palette = palette, colors = colors, modifier = Modifier.fillMaxSize())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "CARGA $percent%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 2.6.sp),
                    color = palette.fill
                )
                Text(
                    text = formatTime(remainingSeconds),
                    style = bigNumber(78.sp, palette),
                    color = numberColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                )
                Text(
                    text = "de ${formatTime(timer.totalSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
            Text(
                text = "TURBO",
                style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 2.sp),
                color = colors.textTertiary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ghostShape = remember { CutCornerShape(10.dp) }
            Press(
                onClick = onExtend,
                shape = ghostShape,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(colors.cardRaised)
                    .border(1.5.dp, colors.hairlineStrong, ghostShape)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = "+${RestTimer.EXTEND_SECONDS} S", style = actionStyle(), color = colors.textPrimary)
            }
            val skipShape = remember { CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp) }
            Press(
                onClick = onSkip,
                shape = skipShape,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(lerp(colors.card, palette.fill, 0.08f))
                    .border(1.dp, palette.fill.copy(alpha = 0.5f), skipShape)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = palette.fill, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = "SALTAR", style = actionStyle(), color = palette.fill)
            }
        }
        Spacer(Modifier.height(12.dp))
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
        mutableIntStateOf(snap(initialSeconds))
    }
    fun select(value: Int) {
        val next = value.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS)
        if (next == seconds) return
        seconds = next
        if (autoRest.enabled) onAutoRestChange(true, next)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RestTitle(text = "DESCANSO", color = palette.fill)
        Spacer(Modifier.height(20.dp))
        TimeDrum(seconds = seconds, palette = palette, onSelect = ::select)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Desliza para ajustar · ${formatTime(RestTimer.MIN_SECONDS)} a ${formatTime(RestTimer.MAX_SECONDS)}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PresetChip(
                label = "−$NUDGE_SECONDS",
                selected = false,
                emphasis = true,
                enabled = seconds > RestTimer.MIN_SECONDS,
                palette = palette,
                onClick = { select(seconds - NUDGE_SECONDS) },
                modifier = Modifier.weight(1f)
            )
            QuickPresets.forEach { preset ->
                PresetChip(
                    label = if (preset < 60) "${preset}s" else formatTime(preset),
                    selected = seconds == preset,
                    emphasis = false,
                    enabled = true,
                    palette = palette,
                    onClick = { select(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
            PresetChip(
                label = "+$NUDGE_SECONDS",
                selected = false,
                emphasis = true,
                enabled = seconds < RestTimer.MAX_SECONDS,
                palette = palette,
                onClick = { select(seconds + NUDGE_SECONDS) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        AutoToggle(
            enabled = autoRest.enabled,
            seconds = seconds,
            palette = palette,
            onChange = { onAutoRestChange(it, seconds) }
        )
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ghostShape = remember { CutCornerShape(10.dp) }
            Press(
                onClick = onClose,
                shape = ghostShape,
                modifier = Modifier
                    .width(112.dp)
                    .height(52.dp)
                    .background(colors.cardRaised)
                    .border(1.5.dp, colors.hairlineStrong, ghostShape)
            ) {
                Text(text = "OMITIR", style = actionStyle(), color = colors.textPrimary)
            }
            val startShape = remember { CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp) }
            Press(
                onClick = { onStart(seconds) },
                shape = startShape,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(lerp(palette.fill, Color.White, 0.35f), palette.fill, lerp(palette.fill, Color.Black, 0.18f))
                        )
                    )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.onFill, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = "INICIAR", style = actionStyle().copy(letterSpacing = 1.6.sp), color = palette.onFill)
            }
        }
    }
}

@Composable
private fun RestTitle(text: String, color: Color) {
    Box(
        modifier = Modifier
            .drawWithCache {
                val reach = 34.dp.toPx()
                val left = -reach
                val width = size.width + reach * 2f
                val low = size.height - 1.dp.toPx()
                val high = low - 7.5.dp.toPx()
                val path = Path().apply {
                    moveTo(left, low)
                    lineTo(left + width * 0.24f, low)
                    lineTo(left + width * 0.30f, high)
                    lineTo(left + width * 0.70f, high)
                    lineTo(left + width * 0.76f, low)
                    lineTo(left + width, low)
                }
                val brush = Brush.horizontalGradient(
                    0f to color.copy(alpha = 0f),
                    0.22f to color,
                    0.78f to color,
                    1f to color.copy(alpha = 0f),
                    startX = left,
                    endX = left + width
                )
                val stroke = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
                onDrawBehind { drawPath(path, brush, style = stroke) }
            }
            .padding(start = 6.dp, end = 6.dp, bottom = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 2.8.sp),
            color = color,
            modifier = Modifier.padding(start = 2.8.dp)
        )
    }
}

@Composable
private fun TimeDrum(seconds: Int, palette: RestPalette, onSelect: (Int) -> Unit) {
    val colors = DarmixTheme.colors
    val density = LocalDensity.current
    val stepPx = with(density) { (TickSpacing * STEP_SECONDS).toPx() }
    val scope = rememberCoroutineScope()
    var drag by remember { mutableFloatStateOf(0f) }
    val currentSeconds by rememberUpdatedState(seconds)
    val currentSelect by rememberUpdatedState(onSelect)
    val flash = remember { Animatable(1f) }
    val armed = remember { mutableStateOf(false) }
    LaunchedEffect(seconds) {
        if (!armed.value) {
            armed.value = true
            return@LaunchedEffect
        }
        flash.snapTo(0.2f)
        flash.animateTo(1f, tween(durationMillis = 340, easing = Motion.Decelerate))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(stepPx) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val from = drag
                        scope.launch { animate(from, 0f) { value, _ -> drag = value } }
                    },
                    onDragCancel = { drag = 0f }
                ) { change, amount ->
                    change.consume()
                    var next = drag + amount
                    var value = currentSeconds
                    while (next <= -stepPx && value < RestTimer.MAX_SECONDS) {
                        next += stepPx
                        value += STEP_SECONDS
                    }
                    while (next >= stepPx && value > RestTimer.MIN_SECONDS) {
                        next -= stepPx
                        value -= STEP_SECONDS
                    }
                    if (value == RestTimer.MAX_SECONDS) next = next.coerceAtLeast(-stepPx * 0.35f)
                    if (value == RestTimer.MIN_SECONDS) next = next.coerceAtMost(stepPx * 0.35f)
                    drag = next
                    if (value != currentSeconds) currentSelect(value)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            contentAlignment = Alignment.Center
        ) {
            val windowShape = remember { RoundedCornerShape(4.dp) }
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .graphicsLayer {
                        alpha = flash.value
                        val scale = 0.94f + 0.06f * flash.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(width = 128.dp, height = 84.dp)
                    .clip(windowShape)
                    .background(Brush.verticalGradient(listOf(palette.fill.copy(alpha = 0.09f), palette.fill.copy(alpha = 0f))))
                    .border(1.dp, palette.fill.copy(alpha = 0.35f), windowShape)
                    .drawBehind { corners(palette.fill, 10.dp.toPx(), 2.dp.toPx()) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrumValue(seconds - NUDGE_SECONDS * 2, 20.sp, 0.22f, 0.13f, colors.textSecondary, onSelect)
                DrumValue(seconds - NUDGE_SECONDS, 30.sp, 0.5f, 0.18f, colors.textSecondary, onSelect)
                Box(modifier = Modifier.weight(0.38f), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = seconds,
                        transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
                        label = "drum-value"
                    ) { value ->
                        Text(
                            text = formatTime(value),
                            style = bigNumber(66.sp, palette),
                            color = palette.fill,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                DrumValue(seconds + NUDGE_SECONDS, 30.sp, 0.5f, 0.18f, colors.textSecondary, onSelect)
                DrumValue(seconds + NUDGE_SECONDS * 2, 20.sp, 0.22f, 0.13f, colors.textSecondary, onSelect)
            }
        }
        Canvas(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(32.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val spacing = TickSpacing.toPx()
            val center = size.width / 2f
            val visible = (size.width / 2f / spacing).toInt() + 2
            val base = 18.dp.toPx()
            for (k in -visible..visible) {
                val t = seconds + k
                if (t < RestTimer.MIN_SECONDS || t > RestTimer.MAX_SECONDS) continue
                val x = center + k * spacing + drag
                val major = t % STEP_SECONDS == 0
                val top = if (major) 2.dp.toPx() else 8.dp.toPx()
                drawLine(
                    color = if (major) colors.hairlineStrong else colors.hairline,
                    start = Offset(x, top),
                    end = Offset(x, base),
                    strokeWidth = if (major) 1.6.dp.toPx() else 1.dp.toPx()
                )
            }
            drawLine(palette.fill, Offset(center, 0f), Offset(center, base), strokeWidth = 2.dp.toPx())
            val arrow = Path().apply {
                moveTo(center - 6.dp.toPx(), 30.dp.toPx())
                lineTo(center + 6.dp.toPx(), 30.dp.toPx())
                lineTo(center, 22.dp.toPx())
                close()
            }
            drawPath(arrow, palette.fill)
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.25f to Color.Black,
                    0.75f to Color.Black,
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }
}

@Composable
private fun RowScope.DrumValue(
    value: Int,
    fontSize: TextUnit,
    alpha: Float,
    weight: Float,
    color: Color,
    onSelect: (Int) -> Unit
) {
    val valid = value in RestTimer.MIN_SECONDS..RestTimer.MAX_SECONDS
    Box(
        modifier = Modifier
            .weight(weight)
            .height(56.dp)
            .clickable(enabled = valid, indication = null, interactionSource = null) { onSelect(value) },
        contentAlignment = Alignment.Center
    ) {
        if (valid) {
            Text(
                text = formatTime(value),
                style = DarmixTextStyles.Metric.copy(fontSize = fontSize, lineHeight = fontSize),
                color = color.copy(alpha = alpha),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    emphasis: Boolean,
    enabled: Boolean,
    palette: RestPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val shape = remember { RoundedCornerShape(4.dp) }
    val background by animateColorAsState(
        targetValue = if (selected) palette.fill else colors.cardRaised,
        animationSpec = Motion.snappy(),
        label = "preset-bg"
    )
    Press(
        onClick = onClick,
        shape = shape,
        enabled = enabled,
        role = Role.RadioButton,
        modifier = modifier
            .height(38.dp)
            .background(background)
            .border(1.dp, if (selected) palette.fill else colors.hairline, shape)
    ) {
        Text(
            text = label,
            style = DarmixTextStyles.Metric.copy(fontSize = 17.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold),
            color = when {
                selected -> palette.onFill
                emphasis -> palette.fill
                else -> colors.textSecondary
            },
            maxLines = 1
        )
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
    val shape = remember { RoundedCornerShape(4.dp) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(lerp(colors.cardRaised, palette.fill, if (enabled) 0.05f else 0f))
            .clickable(role = Role.Switch) { onChange(!enabled) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(1.dp, colors.hairlineStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoMode,
                contentDescription = null,
                tint = palette.fill,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = "Inicio automático", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                text = if (enabled) {
                    "Cada serie inicia ${formatTime(seconds)} de descanso sola"
                } else {
                    "Arranca al completar cada serie"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        RestSwitch(checked = enabled, palette = palette)
    }
}

@Composable
private fun RestSwitch(checked: Boolean, palette: RestPalette) {
    val colors = DarmixTheme.colors
    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = Motion.snappy(),
        label = "switch"
    )
    val track by animateColorAsState(
        targetValue = if (checked) palette.fill else colors.cardHigh,
        animationSpec = Motion.snappy(),
        label = "switch-track"
    )
    val outline = if (checked) palette.fill else colors.hairlineStrong
    val knob = if (checked) palette.onFill else colors.textTertiary
    Canvas(modifier = Modifier.size(width = 50.dp, height = 28.dp)) {
        val radius = size.height / 2f
        drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius))
        drawRoundRect(
            outline,
            topLeft = Offset(0.75.dp.toPx(), 0.75.dp.toPx()),
            size = Size(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            style = Stroke(1.5.dp.toPx())
        )
        val knobRadius = 9.5.dp.toPx()
        val inset = 4.dp.toPx() + knobRadius
        val x = inset + (size.width - inset * 2f) * position
        drawCircle(knob, radius = knobRadius, center = Offset(x, size.height / 2f))
    }
}

@Composable
private fun TurboGauge(
    charge: () -> Float,
    palette: RestPalette,
    colors: DarmixColors,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = remember { TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) }
    Canvas(modifier = modifier) {
        val u = size.minDimension / 270f
        val c = Offset(size.width / 2f, size.height / 2f)
        val p = charge()
        val head = GAUGE_START + GAUGE_SWEEP * p
        val dial = if (palette.dark) colors.canvasDeep else colors.cardRaised

        drawCircle(dial, radius = 132f * u, center = c)
        drawCircle(colors.hairline, radius = 132f * u, center = c, style = Stroke(1.dp.toPx()))

        if (p > 0f) {
            val wedge = Path().apply {
                moveTo(c.x, c.y)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(c, 132f * u),
                    startAngleDegrees = GAUGE_START,
                    sweepAngleDegrees = GAUGE_SWEEP * p,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(
                wedge,
                Brush.radialGradient(
                    0.55f to palette.fill.copy(alpha = 0f),
                    0.86f to palette.fill.copy(alpha = if (palette.dark) 0.22f else 0.16f),
                    1f to palette.fill.copy(alpha = 0f),
                    center = c,
                    radius = 132f * u
                )
            )
        }

        drawCircle(
            Brush.radialGradient(
                0f to lerp(dial, palette.fill, if (palette.dark) 0.1f else 0.06f),
                0.75f to dial,
                1f to dial,
                center = c,
                radius = 92f * u
            ),
            radius = 92f * u,
            center = c
        )
        drawCircle(colors.hairline, radius = 92f * u, center = c, style = Stroke(1.dp.toPx()))

        for (i in 0 until GAUGE_TICKS) {
            val t = i / (GAUGE_TICKS - 1f)
            val angle = GAUGE_START + GAUGE_SWEEP * t
            val major = i % 10 == 0
            val inner = (if (major) 104f else 109f) * u
            val outer = 124f * u
            val lit = p > 0f && t <= p + 0.0001f
            if (lit) {
                val k = t / p.coerceAtLeast(0.0001f)
                val color = if (k < 0.85f) lerp(palette.deep, palette.fill, (k * 1.25f).coerceAtMost(1f))
                else lerp(palette.fill, palette.hot, (k - 0.85f) / 0.15f)
                val width = (if (major) 3.2f else 2.4f) * u
                if (k > 0.4f) radial(c, angle, inner, outer, palette.fill.copy(alpha = 0.18f * k), width * 3f)
                radial(c, angle, inner, outer, color, width)
            } else {
                radial(c, angle, inner, outer, if (major) colors.hairlineStrong else colors.hairline, (if (major) 2.6f else 2f) * u)
            }
        }

        val bandRect = androidx.compose.ui.geometry.Rect(c, 97f * u)
        drawArc(
            color = colors.hairline,
            startAngle = GAUGE_START,
            sweepAngle = GAUGE_SWEEP,
            useCenter = false,
            topLeft = bandRect.topLeft,
            size = bandRect.size,
            style = Stroke(4f * u, cap = StrokeCap.Round)
        )
        if (p > 0f) {
            val band = Brush.linearGradient(
                listOf(palette.deep, palette.fill, palette.hot),
                start = Offset(c.x - 105f * u, c.y + 95f * u),
                end = Offset(c.x + 105f * u, c.y - 95f * u)
            )
            drawArc(
                brush = band,
                startAngle = GAUGE_START,
                sweepAngle = GAUGE_SWEEP * p,
                useCenter = false,
                topLeft = bandRect.topLeft,
                size = bandRect.size,
                alpha = 0.3f,
                style = Stroke(10f * u, cap = StrokeCap.Round)
            )
            drawArc(
                brush = band,
                startAngle = GAUGE_START,
                sweepAngle = GAUGE_SWEEP * p,
                useCenter = false,
                topLeft = bandRect.topLeft,
                size = bandRect.size,
                style = Stroke(4f * u, cap = StrokeCap.Round)
            )
            radial(c, head, 100f * u, 130f * u, palette.fill.copy(alpha = 0.18f), 14f * u)
            radial(c, head, 100f * u, 130f * u, palette.fill.copy(alpha = 0.35f), 7f * u)
            radial(c, head, 100f * u, 130f * u, palette.hot, 3.5f * u)
            val dot = polar(c, head, 97f * u)
            drawCircle(palette.fill.copy(alpha = 0.35f), radius = 8f * u, center = dot)
            drawCircle(palette.hot, radius = 4f * u, center = dot)
        }

        listOf(0f to "0", 0.5f to "50", 1f to "100").forEach { (t, label) ->
            val position = polar(c, GAUGE_START + GAUGE_SWEEP * t, 82f * u)
            val reached = p >= t && p > 0f
            val layout = measurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = layout,
                color = if (reached) palette.fill else colors.textTertiary,
                topLeft = Offset(position.x - layout.size.width / 2f, position.y - layout.size.height / 2f)
            )
        }
    }
}

private fun polar(c: Offset, angle: Float, radius: Float): Offset {
    val r = Math.toRadians(angle.toDouble())
    return Offset(c.x + radius * cos(r).toFloat(), c.y + radius * sin(r).toFloat())
}

private fun DrawScope.radial(c: Offset, angle: Float, inner: Float, outer: Float, color: Color, width: Float) {
    drawLine(color, polar(c, angle, inner), polar(c, angle, outer), strokeWidth = width, cap = StrokeCap.Round)
}

private fun DrawScope.corners(color: Color, length: Float, width: Float) {
    val w = size.width
    val h = size.height
    val half = width / 2f
    drawLine(color, Offset(0f, half), Offset(length, half), width)
    drawLine(color, Offset(half, 0f), Offset(half, length), width)
    drawLine(color, Offset(w - length, half), Offset(w, half), width)
    drawLine(color, Offset(w - half, 0f), Offset(w - half, length), width)
    drawLine(color, Offset(0f, h - half), Offset(length, h - half), width)
    drawLine(color, Offset(half, h - length), Offset(half, h), width)
    drawLine(color, Offset(w - length, h - half), Offset(w, h - half), width)
    drawLine(color, Offset(w - half, h - length), Offset(w - half, h), width)
}

private fun Modifier.restFrame(palette: RestPalette, colors: DarmixColors): Modifier = drawWithCache {
    val cut = 20.dp.toPx()
    val border = 1.5.dp.toPx()
    val outline = framePath(size.width, size.height, cut)
    val fill = Brush.verticalGradient(
        0f to lerp(colors.card, palette.fill, if (palette.dark) 0.07f else 0.05f),
        0.38f to colors.card,
        1f to colors.card
    )
    val glow = if (palette.dark) 1f else 0.4f
    onDrawBehind {
        drawPath(outline, palette.fill, alpha = 0.07f * glow, style = Stroke(18.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outline, palette.fill, alpha = 0.16f * glow, style = Stroke(8.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outline, palette.fill, alpha = 0.45f * glow, style = Stroke(3.5.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outline, fill)
        drawPath(outline, palette.fill, style = Stroke(border, join = StrokeJoin.Miter))
    }
}

private fun framePath(w: Float, h: Float, cut: Float): Path = Path().apply {
    moveTo(0f, 0f)
    lineTo(w - cut, 0f)
    lineTo(w, cut)
    lineTo(w, h)
    lineTo(cut, h)
    lineTo(0f, h - cut)
    close()
}

@Composable
private fun bigNumber(fontSize: TextUnit, palette: RestPalette): TextStyle =
    DarmixTextStyles.Metric.copy(
        fontSize = fontSize,
        lineHeight = fontSize,
        shadow = Shadow(palette.fill.copy(alpha = if (palette.dark) 0.35f else 0.15f), blurRadius = 36f)
    )

@Composable
private fun actionStyle(): TextStyle =
    MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp, letterSpacing = 1.4.sp)

@Composable
private fun Press(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role = Role.Button,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = Motion.snappy(),
        label = "rest-press"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(shape)
            .then(modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = role,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
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

private fun snap(value: Int): Int {
    val clamped = value.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS)
    val rounded = ((clamped + STEP_SECONDS / 2) / STEP_SECONDS) * STEP_SECONDS
    return rounded.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS)
}

private fun formatTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
