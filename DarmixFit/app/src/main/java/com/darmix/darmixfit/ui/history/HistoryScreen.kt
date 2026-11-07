package com.darmix.darmixfit.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.DayLogRow
import com.darmix.darmixfit.data.HistoryExerciseRow
import com.darmix.darmixfit.data.isComplete
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val WeekLabels = listOf("L", "M", "X", "J", "V", "S", "D")
private val CellGap = 6.dp
private val CellHeight = 38.dp
private val CellShape = CutCornerShape(7.dp)
private val ButtonShape = CutCornerShape(8.dp)
private val CalendarShape = CutCornerShape(topEnd = 18.dp)
private val PanelShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
private val TagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val InnerTagShape = CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp)
private val LoadShape = CutCornerShape(10.dp)
private val SetShape = CutCornerShape(3.dp)
private val ThumbShape = CutCornerShape(7.dp)
private const val SWIPE_THRESHOLD = 120f

private val Display = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black)

private enum class LogState { COMPLETE, PARTIAL, EMPTY, FUTURE }

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = DarmixTheme.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.md,
            bottom = Spacing.huge
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "title", contentType = "title") {
            Text(
                text = "HISTORIAL",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
        item(key = "calendar", contentType = "calendar") {
            CalendarCard(
                state = state,
                onSelect = viewModel::select,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToday = viewModel::goToToday
            )
        }
        item(key = "detail-label", contentType = "label") { SectionLabel("REGISTRO DEL DÍA") }
        item(key = "detail", contentType = "detail") { DayDetail(state = state) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.hairline)
        )
    }
}

private fun Modifier.frame(
    shape: Shape,
    fill: Brush,
    border: Color,
    glow: Color?,
    borderWidth: Float = 1f
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        if (glow != null) {
            drawOutline(outline, glow, alpha = 0.07f, style = Stroke(16.dp.toPx(), join = StrokeJoin.Miter))
            drawOutline(outline, glow, alpha = 0.16f, style = Stroke(7.dp.toPx(), join = StrokeJoin.Miter))
            drawOutline(outline, glow, alpha = 0.4f, style = Stroke(3.dp.toPx(), join = StrokeJoin.Miter))
        }
        drawOutline(outline, fill)
        drawOutline(outline, border, style = Stroke(borderWidth.dp.toPx(), join = StrokeJoin.Miter))
    }
}

@Composable
private fun CalendarCard(
    state: HistoryUiState,
    onSelect: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val colors = DarmixTheme.colors
    val currentPrevious by rememberUpdatedState(onPrevious)
    val currentNext by rememberUpdatedState(onNext)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frame(CalendarShape, SolidColor(colors.card), colors.hairline, null)
            .clip(CalendarShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MonthNav(
            month = state.month,
            canGoNext = state.canGoNext,
            showToday = state.showToday,
            onPrevious = onPrevious,
            onNext = onNext,
            onToday = onToday
        )
        MonthStats(stats = state.stats)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
            WeekLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        AnimatedContent(
            targetState = state.month,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(Motion.Standard, easing = Motion.Decelerate)) { if (forward) it / 3 else -it / 3 } +
                        fadeIn(tween(Motion.Standard))) togetherWith
                        (slideOutHorizontally(tween(Motion.Quick)) { if (forward) -it / 3 else it / 3 } +
                                fadeOut(tween(Motion.Quick))) using SizeTransform(clip = false)
            },
            label = "month",
            modifier = Modifier.pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total > SWIPE_THRESHOLD -> currentPrevious()
                            total < -SWIPE_THRESHOLD -> currentNext()
                        }
                    },
                    onHorizontalDrag = { _, amount -> total += amount }
                )
            }
        ) { month ->
            MonthGrid(month = month, state = state, onSelect = onSelect)
        }
        Legend()
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(ButtonShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairlineStrong, ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) colors.textSecondary else colors.textTertiary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MonthNav(
    month: YearMonth,
    canGoNext: Boolean,
    showToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val colors = DarmixTheme.colors
    val monthName = remember(month) { month.month.getDisplayName(DateTextStyle.FULL, MexicoLocale).uppercase(MexicoLocale) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        NavButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mes anterior", true, onPrevious)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${month.year}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = colors.textTertiary
            )
            AnimatedContent(
                targetState = monthName,
                transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() },
                label = "month-name"
            ) { name ->
                Text(text = name, style = Display.copy(fontSize = 28.sp, lineHeight = 30.sp), color = colors.textPrimary)
            }
        }
        if (showToday) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(28.dp)
                    .clip(TagShape)
                    .background(colors.volt.copy(alpha = 0.14f))
                    .border(1.dp, colors.voltText, TagShape)
                    .clickable(role = Role.Button, onClick = onToday)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HOY",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.5.sp),
                    color = colors.voltText
                )
            }
        }
        NavButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Mes siguiente", canGoNext, onNext)
    }
}

@Composable
private fun MonthStats(stats: HistoryStats) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .drawBehind {
                val w = 1.dp.toPx()
                drawLine(colors.hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = w)
                drawLine(colors.hairline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = w)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(value = "${stats.monthComplete}", label = "RUTINAS", color = colors.voltText, modifier = Modifier.weight(1f))
        StatDivider()
        StatCell(value = "${stats.monthSets}", label = "SERIES", color = colors.tideText, modifier = Modifier.weight(1f))
        StatDivider()
        StatCell(
            value = stats.compliance?.let { "$it%" } ?: "—",
            label = "CUMPLIMIENTO",
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(DarmixTheme.colors.hairline)
    )
}

@Composable
private fun StatCell(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = Display.copy(fontSize = 26.sp, lineHeight = 26.sp), color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
            color = DarmixTheme.colors.textTertiary,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun MonthGrid(month: YearMonth, state: HistoryUiState, onSelect: (LocalDate) -> Unit) {
    val colors = DarmixTheme.colors
    val bridge = if (colors.isDark) lerp(colors.volt, Color.Black, 0.35f) else colors.voltText.copy(alpha = 0.55f)
    val offset = month.atDay(1).dayOfWeek.value - 1
    val length = month.lengthOfMonth()
    val weeks = (offset + length + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(CellGap + 4.dp)) {
        for (week in 0 until weeks) {
            val runs = remember(month, week, state.statuses) {
                val complete = (0 until 7).filter { column ->
                    val day = week * 7 + column - offset + 1
                    day in 1..length && state.statuses[month.atDay(day)] == DayStatus.COMPLETE
                }
                buildList {
                    var start = -1
                    var previous = -2
                    for (column in complete) {
                        if (column != previous + 1) {
                            if (start >= 0 && previous > start) add(start to previous)
                            start = column
                        }
                        previous = column
                    }
                    if (start >= 0 && previous > start) add(start to previous)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (runs.isEmpty()) return@drawBehind
                        val gap = CellGap.toPx()
                        val cell = (size.width - gap * 6) / 7f
                        val band = 4.dp.toPx()
                        runs.forEach { (from, to) ->
                            val left = from * (cell + gap) + cell / 2f
                            val right = to * (cell + gap) + cell / 2f
                            drawRect(
                                color = bridge,
                                topLeft = Offset(left, (size.height - band) / 2f),
                                size = Size(right - left, band)
                            )
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(CellGap)
            ) {
                for (column in 0 until 7) {
                    val day = week * 7 + column - offset + 1
                    if (day in 1..length) {
                        val date = month.atDay(day)
                        DayCell(
                            date = date,
                            status = state.statuses[date],
                            ratio = state.ratios[date] ?: 0f,
                            isToday = date == state.today,
                            isSelected = date == state.selected,
                            isFuture = date.isAfter(state.today),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .height(CellHeight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus?,
    ratio: Float,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val complete = status == DayStatus.COMPLETE
    val partial = status == DayStatus.PARTIAL
    val fill: Brush? = when {
        complete -> Brush.verticalGradient(listOf(colors.voltBright, colors.volt))
        partial -> Brush.verticalGradient(
            listOf(
                colors.volt.copy(alpha = 0.18f + 0.3f * ratio),
                colors.volt.copy(alpha = 0.1f + 0.24f * ratio)
            )
        )
        else -> null
    }
    val content = when {
        complete -> colors.onVolt
        partial -> colors.voltText
        isFuture -> colors.textTertiary.copy(alpha = 0.55f)
        isToday -> colors.textPrimary
        else -> colors.textSecondary
    }
    val outline = when {
        complete -> null
        isToday -> colors.voltText
        partial -> colors.voltText.copy(alpha = 0.5f)
        isFuture -> null
        else -> colors.hairlineStrong
    }
    val halo = colors.volt
    val marker = if (isToday) colors.volt else colors.textPrimary

    Box(
        modifier = modifier
            .height(CellHeight)
            .drawBehind {
                if (isToday) {
                    val e = 3.dp.toPx()
                    val haloOutline = CellShape.createOutline(Size(size.width + e * 2, size.height + e * 2), layoutDirection, this)
                    translate(-e, -e) {
                        drawOutline(haloOutline, halo, alpha = 0.6f, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Miter))
                    }
                }
                if (isSelected) {
                    val w = 5.dp.toPx()
                    val h = 6.dp.toPx()
                    val top = size.height + 3.dp.toPx() + if (isToday) 2.dp.toPx() else 0f
                    val path = Path().apply {
                        moveTo(size.width / 2f, top)
                        lineTo(size.width / 2f + w, top + h)
                        lineTo(size.width / 2f - w, top + h)
                        close()
                    }
                    drawPath(path, marker)
                }
            }
            .clip(CellShape)
            .then(if (fill != null) Modifier.background(fill) else Modifier)
            .then(if (outline != null) Modifier.border(1.dp, outline, CellShape) else Modifier)
            .clickable(enabled = !isFuture, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (complete || isToday) FontWeight.ExtraBold else FontWeight.Bold),
            color = content
        )
    }
}

@Composable
private fun Legend() {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = "Completa") {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CutCornerShape(2.5.dp))
                    .background(colors.volt)
            )
        }
        LegendItem(label = "Parcial") {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CutCornerShape(2.5.dp))
                    .background(colors.volt.copy(alpha = 0.4f))
            )
        }
        LegendItem(label = "Sin entreno") {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CutCornerShape(2.5.dp))
                    .border(1.dp, colors.hairlineStrong, CutCornerShape(2.5.dp))
            )
        }
        LegendItem(label = "Racha") {
            Box(
                Modifier
                    .size(width = 14.dp, height = 4.dp)
                    .background(if (colors.isDark) lerp(colors.volt, Color.Black, 0.35f) else colors.voltText.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = DarmixTheme.colors.textSecondary
        )
    }
}

@Composable
private fun DayDetail(state: HistoryUiState) {
    AnimatedContent(
        targetState = state.selected,
        transitionSpec = {
            (fadeIn(tween(Motion.Standard)) + slideInVertically(tween(Motion.Standard, easing = Motion.Decelerate)) { it / 10 }) togetherWith
                    fadeOut(tween(Motion.Quick)) using SizeTransform(clip = false)
        },
        label = "day-detail"
    ) { date ->
        val key = date.toString()
        val logs = state.dayLogs.filter { it.log.date == key }
        DebriefCard(
            date = date,
            isFuture = date.isAfter(state.today),
            isToday = date == state.today,
            logs = logs,
            exercises = if (logs.isEmpty()) emptyList() else state.dayExercises
        )
    }
}

private fun logState(logs: List<DayLogRow>, isFuture: Boolean): LogState = when {
    isFuture -> LogState.FUTURE
    logs.isEmpty() -> LogState.EMPTY
    logs.all { it.log.isComplete } -> LogState.COMPLETE
    else -> LogState.PARTIAL
}

private fun stateAccent(state: LogState, colors: DarmixColors): Color = when (state) {
    LogState.COMPLETE -> colors.leafText
    LogState.PARTIAL -> colors.voltText
    LogState.EMPTY, LogState.FUTURE -> colors.textTertiary
}

@Composable
private fun DebriefCard(
    date: LocalDate,
    isFuture: Boolean,
    isToday: Boolean,
    logs: List<DayLogRow>,
    exercises: List<HistoryExerciseRow>
) {
    val colors = DarmixTheme.colors
    val state = logState(logs, isFuture)
    val accent = stateAccent(state, colors)
    val lit = state == LogState.COMPLETE || state == LogState.PARTIAL
    val weekday = remember(date) { date.dayOfWeek.getDisplayName(DateTextStyle.FULL, MexicoLocale).uppercase(MexicoLocale) }
    val monthAbbr = remember(date) {
        date.month.getDisplayName(DateTextStyle.SHORT, MexicoLocale).replace(".", "").uppercase(MexicoLocale)
    }
    val fill = remember(colors, accent, lit) {
        Brush.verticalGradient(
            0f to if (lit) lerp(colors.card, accent, 0.06f) else colors.card,
            0.4f to colors.card,
            1f to colors.card
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frame(
                shape = PanelShape,
                fill = fill,
                border = if (lit) accent else colors.hairline,
                glow = if (lit) accent else null,
                borderWidth = if (lit) 1.5f else 1f
            )
            .clip(PanelShape)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = date.dayOfMonth.toString().padStart(2, '0'),
                    style = Display.copy(fontSize = 64.sp, lineHeight = 56.sp),
                    color = colors.textPrimary
                )
                Column(modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)) {
                    Text(
                        text = weekday,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp),
                        color = if (lit) accent else colors.textSecondary
                    )
                    Text(
                        text = if (isToday) "$monthAbbr · HOY" else monthAbbr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp),
                        color = colors.textTertiary
                    )
                }
            }
            Stamp(
                text = when (state) {
                    LogState.COMPLETE -> "COMPLETADO"
                    LogState.PARTIAL -> "PARCIAL"
                    LogState.EMPTY -> "SIN ENTRENO"
                    LogState.FUTURE -> "PRÓXIMO"
                },
                color = accent
            )
        }

        if (logs.isEmpty()) {
            Spacer(Modifier.height(14.dp))
            EmptyDay(isFuture = isFuture)
        } else {
            logs.forEach { row ->
                ProgramDebrief(row = row, exercises = exercises.filter { it.programId == row.log.programId })
            }
        }
    }
}

@Composable
private fun Stamp(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(TagShape)
            .border(1.5.dp, color, TagShape)
            .padding(2.5.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(InnerTagShape)
                .border(1.dp, color.copy(alpha = 0.5f), InnerTagShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black),
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyDay(isFuture: Boolean) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LoadShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairline, LoadShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = if (isFuture) "Este día aún no llega. Prepárate." else "Sin entrenamiento registrado este día.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ProgramDebrief(row: DayLogRow, exercises: List<HistoryExerciseRow>) {
    val colors = DarmixTheme.colors
    val log = row.log
    val complete = log.isComplete
    val accent = if (complete) colors.leafText else colors.voltText

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = row.programName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        LoadPanel(done = log.doneSets, planned = log.plannedSets)
        if (exercises.isNotEmpty()) {
            Circuit(exercises = exercises, accent = accent)
        }
    }
}

@Composable
private fun LoadPanel(done: Int, planned: Int) {
    val colors = DarmixTheme.colors
    val lit = if (colors.isDark) {
        Brush.verticalGradient(listOf(colors.voltBright, colors.volt))
    } else {
        Brush.verticalGradient(listOf(lerp(colors.volt, colors.voltDeep, 0.2f), lerp(colors.volt, colors.voltDeep, 0.55f)))
    }
    val off = if (colors.isDark) lerp(colors.card, colors.hairline, 0.6f) else colors.hairline
    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(LoadShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairline, LoadShape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "CARGA DEL DÍA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = colors.textTertiary,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 3.dp)
            )
            Text(text = "$done", style = Display.copy(fontSize = 22.sp, lineHeight = 22.sp), color = colors.voltText)
            Text(
                text = "/$planned SERIES",
                style = Display.copy(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.textTertiary
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(planned.coerceAtLeast(1)) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(SetShape)
                        .then(if (index < done) Modifier.background(lit) else Modifier.background(off))
                )
            }
        }
    }
}

@Composable
private fun Circuit(exercises: List<HistoryExerciseRow>, accent: Color) {
    val colors = DarmixTheme.colors
    val rail = accent
    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .drawBehind {
                if (exercises.size < 2) return@drawBehind
                val x = 10.dp.toPx()
                val top = 28.dp.toPx()
                val bottom = size.height - 28.dp.toPx()
                drawLine(rail.copy(alpha = 0.7f), Offset(x, top), Offset(x, bottom), strokeWidth = 2.dp.toPx())
            }
    ) {
        exercises.forEach { exercise ->
            val done = exercise.completedSets.coerceAtMost(exercise.sets)
            val complete = done >= exercise.sets
            val nodeColor = when {
                complete -> colors.leafText
                done > 0 -> colors.voltText
                else -> colors.hairlineStrong
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .drawBehind { drawCircle(colors.card, radius = size.minDimension / 2f + 3.dp.toPx()) }
                        .clip(CircleShape)
                        .background(if (complete) nodeColor else colors.card)
                        .border(1.5.dp, nodeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (complete) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (colors.isDark) colors.canvasDeep else Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Box(modifier = Modifier.clip(ThumbShape)) {
                    ExerciseImage(imageName = exercise.imageUri, exerciseName = exercise.name, size = 40.dp, animated = false)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp, lineHeight = 17.sp),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$done/${exercise.sets} series",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold),
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(exercise.sets) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 16.dp)
                                .clip(SetShape)
                                .background(if (index < done) nodeColor else colors.hairline)
                        )
                    }
                }
            }
        }
    }
}
