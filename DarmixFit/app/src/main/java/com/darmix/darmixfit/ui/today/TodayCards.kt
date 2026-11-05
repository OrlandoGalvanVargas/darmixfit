package com.darmix.darmixfit.ui.today

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.PulseDot
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlin.math.max

internal enum class EntryStatus { DONE, ACTIVE, UPCOMING }

private val RailWidth = 52.dp
private val RailX = 26.dp
private val DoneNodeY = 25.dp
private val ActiveNodeY = 34.dp
private val UpcomingNodeY = 37.dp
private val ActiveRing = 104.dp
private val ListRing = 54.dp

private class EntryAccent(val fill: Color, val onFill: Color)

private fun accentFor(colors: DarmixColors, superset: Boolean) = if (superset) {
    EntryAccent(colors.tideText, if (colors.isDark) colors.onTide else Color.White)
} else {
    EntryAccent(colors.voltText, if (colors.isDark) colors.onVolt else Color.White)
}

@Composable
internal fun RoutineEntry(
    item: TodayItem,
    position: Int,
    totalBlocks: Int,
    status: EntryStatus,
    isFirst: Boolean,
    isLast: Boolean,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onFocus: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val accent = remember(colors, item is TodayItem.Superset) { accentFor(colors, item is TodayItem.Superset) }
    TimelineRail(
        status = status,
        isFirst = isFirst,
        isLast = isLast,
        accent = accent,
        modifier = modifier
    ) {
        when (status) {
            EntryStatus.DONE -> DoneCard(title = item.title(), accent = accent, onUndo = onUndo)
            EntryStatus.UPCOMING -> UpcomingCard(
                item = item,
                position = position,
                accent = accent,
                onStart = onFocus,
                onImageClick = onImageClick
            )
            EntryStatus.ACTIVE -> ActiveCard(
                item = item,
                position = position,
                totalBlocks = totalBlocks,
                accent = accent,
                onComplete = onComplete,
                onUndo = onUndo,
                onImageClick = onImageClick
            )
        }
    }
}

private fun TodayItem.title(): String = when (this) {
    is TodayItem.Single -> entry.exercise.name
    is TodayItem.Superset -> members.joinToString(" + ") { it.exercise.name }
}

@Composable
private fun TimelineRail(
    status: EntryStatus,
    isFirst: Boolean,
    isLast: Boolean,
    accent: EntryAccent,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = DarmixTheme.colors
    val spin: State<Float>? = if (status == EntryStatus.ACTIVE) {
        rememberInfiniteTransition(label = "rail").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "rail-spin"
        )
    } else {
        null
    }
    val gap = if (status == EntryStatus.ACTIVE) 14.dp else Spacing.md
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = Spacing.screen)
            .drawWithCache {
                val x = RailX.toPx()
                val y = when (status) {
                    EntryStatus.DONE -> DoneNodeY
                    EntryStatus.ACTIVE -> ActiveNodeY
                    EntryStatus.UPCOMING -> UpcomingNodeY
                }.toPx()
                val line = 2.dp.toPx()
                val lit = accent.fill.copy(alpha = 0.85f)
                val check = Path().apply {
                    val s = 4.6.dp.toPx()
                    moveTo(x - s * 0.95f, y + s * 0.05f)
                    lineTo(x - s * 0.25f, y + s * 0.75f)
                    lineTo(x + s * 0.95f, y - s * 0.6f)
                }
                val checkStroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val dash = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.9.dp.toPx()))
                )
                onDrawBehind {
                    val node = Offset(x, y)
                    if (!isFirst) {
                        drawLine(if (status != EntryStatus.UPCOMING) lit else colors.hairline, Offset(x, 0f), node, line)
                    }
                    if (!isLast) {
                        drawLine(if (status == EntryStatus.DONE) lit else colors.hairline, node, Offset(x, size.height), line)
                    }
                    when (status) {
                        EntryStatus.DONE -> {
                            drawCircle(accent.fill.copy(alpha = 0.22f), radius = 18.dp.toPx(), center = node)
                            drawCircle(accent.fill, radius = 13.dp.toPx(), center = node)
                            drawPath(check, accent.onFill, style = checkStroke)
                        }
                        EntryStatus.ACTIVE -> {
                            rotate(spin?.value ?: 0f, node) {
                                drawCircle(accent.fill, radius = 17.dp.toPx(), center = node, style = dash)
                            }
                            drawCircle(accent.fill.copy(alpha = 0.3f), radius = 15.dp.toPx(), center = node)
                            drawCircle(colors.canvas, radius = 12.dp.toPx(), center = node)
                            drawCircle(accent.fill, radius = 12.dp.toPx(), center = node, style = Stroke(2.dp.toPx()))
                            drawCircle(accent.fill, radius = 5.dp.toPx(), center = node)
                        }
                        EntryStatus.UPCOMING -> {
                            drawCircle(colors.canvas, radius = 11.dp.toPx(), center = node)
                            drawCircle(colors.hairlineStrong, radius = 11.dp.toPx(), center = node, style = Stroke(1.5.dp.toPx()))
                            drawCircle(colors.hairlineStrong, radius = 3.dp.toPx(), center = node)
                        }
                    }
                }
            }
    ) {
        Spacer(Modifier.width(RailWidth))
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = gap)
        ) {
            content()
        }
    }
}

@Composable
private fun DoneCard(title: String, accent: EntryAccent, onUndo: () -> Unit) {
    val colors = DarmixTheme.colors
    val shape = remember { CutCornerShape(topEnd = 12.dp) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairline, shape)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "COMPLETADO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.8.sp),
            color = accent.fill,
            maxLines = 1
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        HudPress(
            onClick = onUndo,
            shape = CircleShape,
            modifier = Modifier
                .size(34.dp)
                .border(1.dp, colors.hairline, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Deshacer última serie",
                tint = colors.textSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun UpcomingCard(
    item: TodayItem,
    position: Int,
    accent: EntryAccent,
    onStart: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit
) {
    val colors = DarmixTheme.colors
    val superset = item is TodayItem.Superset
    val shape = remember { CutCornerShape(topEnd = 14.dp) }
    val detail = when (item) {
        is TodayItem.Single -> "${item.entry.exercise.sets} × ${item.entry.exercise.reps}"
        is TodayItem.Superset -> "Superserie · ${item.totalRounds} rondas"
    }
    HudPress(
        onClick = onStart,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .border(1.dp, if (superset) accent.fill.copy(alpha = 0.45f) else colors.hairline, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = position.toString().padStart(2, '0'),
                style = DarmixTextStyles.Metric.copy(fontSize = 26.sp, lineHeight = 26.sp),
                color = if (superset) accent.fill else colors.textTertiary.copy(alpha = 0.6f),
                modifier = Modifier.width(28.dp)
            )
            SetRing(
                total = item.totalUnits,
                completed = item.completedUnits,
                accent = accent.fill,
                size = ListRing,
                stroke = 4.dp,
                inset = 6.dp,
                emphasis = false
            ) { inner ->
                RingCenter(item = item, size = inner, accent = accent, onImageClick = onImageClick)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (superset) SupersetLabel(accent.fill)
                Text(
                    text = item.title(),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 19.sp),
                    color = colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "›",
                style = DarmixTextStyles.Metric.copy(fontSize = 30.sp, lineHeight = 30.sp),
                color = accent.fill
            )
        }
    }
}

@Composable
private fun ActiveCard(
    item: TodayItem,
    position: Int,
    totalBlocks: Int,
    accent: EntryAccent,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit
) {
    val colors = DarmixTheme.colors
    val superset = item as? TodayItem.Superset
    val single = item as? TodayItem.Single
    val unitLabel = if (superset != null) "Ronda" else "Serie"
    val current = (item.completedUnits + 1).coerceAtMost(item.totalUnits)
    val note = single?.entry?.exercise?.note.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .energyFrame(accent.fill, colors)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PulseDot(color = accent.fill, size = 8.dp)
            Text(
                text = "EN CURSO",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp),
                color = accent.fill
            )
            if (superset != null) {
                Spacer(Modifier.weight(1f))
                SupersetLabel(accent.fill)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (single != null) {
            Text(
                text = single.entry.exercise.name,
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 24.sp),
                color = colors.textPrimary
            )
        } else if (superset != null) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                superset.members.forEachIndexed { index, member ->
                    SupersetMemberLine(letter = ('A' + index).toString(), exercise = member.exercise, accent = accent)
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SetRing(
                total = item.totalUnits,
                completed = item.completedUnits,
                accent = accent.fill,
                size = ActiveRing,
                stroke = 8.dp,
                inset = 17.dp,
                emphasis = true
            ) { inner ->
                RingCenter(item = item, size = inner, accent = accent, onImageClick = onImageClick)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (single != null) {
                    StatTile(label = "REPETICIONES", value = repsValue(single.entry.exercise.reps), accent = accent.fill, highlight = true)
                } else if (superset != null) {
                    StatTile(label = "EJERCICIOS", value = "${superset.members.size}", accent = accent.fill, highlight = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatTile(
                        label = unitLabel.uppercase(),
                        value = "$current/${item.totalUnits}",
                        accent = accent.fill,
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "BLOQUE",
                        value = "$position/$totalBlocks",
                        accent = accent.fill,
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (note.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            TechniqueNote(text = note, accent = accent.fill)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val undoShape = remember { CutCornerShape(10.dp) }
            HudPress(
                onClick = onUndo,
                enabled = item.completedUnits > 0,
                shape = undoShape,
                modifier = Modifier
                    .size(width = 50.dp, height = 46.dp)
                    .background(colors.cardRaised)
                    .border(1.dp, colors.hairlineStrong, undoShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Deshacer",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            val buttonShape = remember { CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp) }
            HudPress(
                onClick = onComplete,
                shape = buttonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(lerp(accent.fill, Color.White, 0.35f), accent.fill, lerp(accent.fill, Color.Black, 0.18f))
                        )
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = accent.onFill, modifier = Modifier.size(18.dp))
                    Text(
                        text = "COMPLETAR ${unitLabel.uppercase()} $current",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent.onFill,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun repsValue(reps: String): String {
    val trimmed = reps.trim()
    val stripped = trimmed.replace(Regex("\\s*reps?\\.?$", RegexOption.IGNORE_CASE), "").trim()
    return stripped.ifEmpty { trimmed }
}

@Composable
private fun SupersetLabel(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.Repeat, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(
            text = "SUPERSERIE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = color
        )
    }
}

@Composable
private fun SupersetMemberLine(letter: String, exercise: ExerciseEntity, accent: EntryAccent) {
    val colors = DarmixTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp))
                .background(accent.fill),
            contentAlignment = Alignment.Center
        ) {
            Text(text = letter, style = MaterialTheme.typography.labelLarge, color = accent.onFill)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = exercise.reps, style = MaterialTheme.typography.bodySmall, color = accent.fill, maxLines = 1)
        }
    }
}

@Composable
private fun RingCenter(item: TodayItem, size: Dp, accent: EntryAccent, onImageClick: (ExerciseEntity) -> Unit) {
    val colors = DarmixTheme.colors
    when (item) {
        is TodayItem.Single -> {
            val exercise = item.entry.exercise
            ExerciseImage(
                imageName = exercise.imageUri,
                exerciseName = exercise.name,
                size = size,
                modifier = Modifier.clip(CircleShape),
                onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
            )
        }
        is TodayItem.Superset -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(accent.fill.copy(alpha = 0.14f))
                .border(1.dp, accent.fill.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×${item.members.size}",
                style = DarmixTextStyles.Metric.copy(fontSize = (size.value * 0.36f).sp, lineHeight = (size.value * 0.36f).sp),
                color = accent.fill
            )
        }
    }
}

@Composable
private fun SetRing(
    total: Int,
    completed: Int,
    accent: Color,
    size: Dp,
    stroke: Dp,
    inset: Dp,
    emphasis: Boolean,
    center: @Composable (Dp) -> Unit
) {
    val colors = DarmixTheme.colors
    val pulse: State<Float>? = if (emphasis && completed < total) {
        rememberInfiniteTransition(label = "ring").animateFloat(
            initialValue = 0.3f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "ring-pulse"
        )
    } else {
        null
    }
    val filled by animateFloatAsState(
        targetValue = completed.toFloat(),
        animationSpec = Motion.snappy(),
        label = "ring-filled"
    )
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val segments = total.coerceAtLeast(1)
                val width = stroke.toPx()
                val radius = this.size.minDimension / 2f - (if (emphasis) 6.dp.toPx() else width / 2f + 0.5.dp.toPx())
                val gap = if (emphasis) 6f else 5f
                val step = 360f / segments
                if (emphasis) {
                    drawCircle(colors.canvasDeep, radius = this.size.minDimension / 2f - 1.dp.toPx())
                    drawCircle(colors.hairlineStrong, radius = this.size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
                for (i in 0 until segments) {
                    val start = -90f + i * step + gap
                    val sweep = step - gap * 2f
                    val fill = (filled - i).coerceIn(0f, 1f)
                    arc(colors.hairlineStrong, start, sweep, radius, width, 1f)
                    if (fill > 0f) {
                        if (emphasis) arc(accent, start, sweep * fill, radius, width * 2.2f, 0.22f)
                        arc(accent, start, sweep * fill, radius, width, 1f)
                    } else if (i == completed && pulse != null) {
                        arc(accent, start, sweep, radius, width, pulse.value * 0.5f)
                    }
                }
                if (emphasis) {
                    drawCircle(colors.hairlineStrong, radius = radius - 6.dp.toPx(), style = Stroke(1.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        center(size - inset * 2)
    }
}

private fun DrawScope.arc(color: Color, start: Float, sweep: Float, radius: Float, width: Float, alpha: Float) {
    if (sweep <= 0f || alpha <= 0f) return
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        alpha = alpha,
        style = Stroke(width)
    )
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, highlight: Boolean, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val shape = remember { CutCornerShape(topEnd = 8.dp) }
    val edge = if (highlight) accent else colors.hairlineStrong
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.canvasDeep)
            .drawBehind { drawRect(edge, size = Size(2.dp.toPx(), size.height)) }
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
            color = colors.textTertiary,
            maxLines = 1
        )
        Text(
            text = value,
            style = DarmixTextStyles.Metric.copy(fontSize = 21.sp, lineHeight = 23.sp),
            color = if (highlight) accent else colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TechniqueNote(text: String, accent: Color) {
    val colors = DarmixTheme.colors
    Column {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Text(
                text = "TÉCNICA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.2.sp),
                color = accent,
                modifier = Modifier.padding(start = 4.dp, end = 26.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
            ) {
                val stroke = 1.6.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, 1.dp.toPx())
                    lineTo(6.dp.toPx(), 6.5.dp.toPx())
                    lineTo(size.width, 6.5.dp.toPx())
                }
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        0f to accent,
                        0.75f to accent,
                        1f to accent.copy(alpha = 0f)
                    ),
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary.copy(alpha = 0.86f)
        )
    }
}

private fun Modifier.energyFrame(accent: Color, colors: DarmixColors): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val outerCut = 18.dp.toPx()
    val border = 3.dp.toPx()
    val gap = 4.dp.toPx()
    val innerCut = 15.dp.toPx()
    val line = 2.dp.toPx()
    val outer = cutPath(0f, 0f, w, h, outerCut)
    val band = cutPath(border, border, w - border, h - border, outerCut - border * 0.42f)
    val inset = border + gap
    val inner = cutPath(inset, inset, w - inset, h - inset, innerCut)
    val dark = colors.isDark
    val bandBase = if (dark) {
        Brush.linearGradient(
            listOf(lerp(accent, Color.Black, 0.45f), lerp(accent, Color.Black, 0.55f), lerp(accent, Color.Black, 0.5f)),
            start = Offset.Zero,
            end = Offset(w, h)
        )
    } else {
        Brush.linearGradient(
            listOf(lerp(accent, Color.White, 0.62f), lerp(accent, Color.White, 0.72f), lerp(accent, Color.White, 0.66f)),
            start = Offset.Zero,
            end = Offset(w, h)
        )
    }
    val reach = max(w, h) * 0.6f
    val shineA = Brush.radialGradient(listOf(accent.copy(alpha = if (dark) 0.55f else 0.22f), Color.Transparent), center = Offset.Zero, radius = reach)
    val shineB = Brush.radialGradient(listOf(accent.copy(alpha = if (dark) 0.45f else 0.18f), Color.Transparent), center = Offset(w, h), radius = reach)
    val rim = if (dark) accent else lerp(accent, Color.White, 0.18f)
    val glow = if (dark) 1f else 0.45f
    val panel = Brush.verticalGradient(
        0f to lerp(colors.card, accent, if (colors.isDark) 0.1f else 0.08f),
        0.55f to colors.card,
        1f to colors.card,
        startY = inset,
        endY = h - inset
    )
    onDrawBehind {
        drawPath(outer, accent, alpha = 0.08f * glow, style = Stroke(16.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outer, accent, alpha = 0.18f * glow, style = Stroke(8.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outer, accent, alpha = 0.5f * glow, style = Stroke(3.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outer, rim)
        drawPath(band, bandBase)
        drawPath(band, shineA)
        drawPath(band, shineB)
        drawPath(inner, accent, alpha = 0.45f * glow, style = Stroke(line * 4f, join = StrokeJoin.Miter))
        drawPath(inner, panel)
        drawPath(inner, rim, style = Stroke(line, join = StrokeJoin.Miter))
    }
}

private fun cutPath(left: Float, top: Float, right: Float, bottom: Float, cut: Float): Path = Path().apply {
    moveTo(left, top)
    lineTo(right - cut, top)
    lineTo(right, top + cut)
    lineTo(right, bottom)
    lineTo(left + cut, bottom)
    lineTo(left, bottom - cut)
    close()
}

@Composable
private fun HudPress(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = Motion.snappy(),
        label = "hud-press"
    )
    Box(
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
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
