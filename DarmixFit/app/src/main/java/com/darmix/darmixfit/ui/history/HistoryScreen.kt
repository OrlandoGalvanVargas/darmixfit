package com.darmix.darmixfit.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Today
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.DayLogRow
import com.darmix.darmixfit.data.HistoryExerciseRow
import com.darmix.darmixfit.data.isComplete
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.DarmixIconButton
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.SetMeter
import com.darmix.darmixfit.ui.components.StatusTag
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val LongDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", MexicoLocale)
private val WeekLabels = listOf("L", "M", "X", "J", "V", "S", "D")
private val CellGap = 6.dp
private val CellShape = RoundedCornerShape(12.dp)
private const val SWIPE_THRESHOLD = 120f

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
        item(key = "title", contentType = "title") { ScreenTitle(year = state.today.year) }
        item(key = "streak", contentType = "streak") { StreakHero(stats = state.stats) }
        item(key = "calendar", contentType = "calendar") {
            CalendarCard(
                state = state,
                onSelect = viewModel::select,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToday = viewModel::goToToday
            )
        }
        item(key = "detail", contentType = "detail") { DayDetail(state = state) }
        item(key = "summary-label", contentType = "label") { SummaryLabel(month = state.month) }
        item(key = "summary", contentType = "summary") { StatsGrid(stats = state.stats) }
    }
}

@Composable
private fun ScreenTitle(year: Int) {
    val colors = DarmixTheme.colors
    Column(
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(
                Modifier
                    .size(width = 16.dp, height = 3.dp)
                    .background(colors.voltText, CircleShape)
            )
            Overline(text = "Historial · $year", color = colors.textSecondary)
        }
        Text(text = "BITÁCORA", style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
    }
}

@Composable
private fun StreakHero(stats: HistoryStats) {
    val colors = DarmixTheme.colors
    val streak = stats.weeklyStreak
    val lit = streak > 0
    val accent = if (lit) colors.voltText else colors.textTertiary
    val message = when {
        streak == 0 -> "Completa una rutina esta semana para encender tu racha."
        streak == 1 -> "La primera semana ya está. Ahora no la apagues."
        streak < 4 -> "Semana tras semana. Así se construye."
        else -> "Constancia de leyenda. Sigue alimentando la flama."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .drawWithCache {
                val center = Offset(size.width, size.height * 0.2f)
                val radius = size.width * 0.75f
                val glow = Brush.radialGradient(
                    listOf(colors.volt.copy(alpha = if (lit) (if (colors.isDark) 0.16f else 0.26f) else 0f), Color.Transparent),
                    center = center,
                    radius = radius
                )
                onDrawBehind { drawCircle(glow, radius = radius, center = center) }
            }
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Overline(text = "Racha semanal", color = colors.textTertiary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "$streak", style = DarmixTextStyles.MetricHero, color = accent)
                    Text(
                        text = if (streak == 1) "SEMANA" else "SEMANAS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = Spacing.sm, bottom = 10.dp)
                    )
                }
            }
            FlameBadge(lit = lit)
        }
        WeekChain(weeks = stats.recentWeeks)
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun FlameBadge(lit: Boolean) {
    val colors = DarmixTheme.colors
    val flicker = if (lit) {
        rememberInfiniteTransition(label = "flame").animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "flicker"
        )
    } else {
        null
    }
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (lit) {
                    Brush.verticalGradient(listOf(DarmixPalette.VoltBright, DarmixPalette.VoltDeep))
                } else {
                    Brush.verticalGradient(listOf(colors.cardHigh, colors.cardHigh))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (lit) DarmixPalette.VoltInk else colors.textTertiary,
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    val s = flicker?.value ?: 1f
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        )
    }
}

@Composable
private fun WeekChain(weeks: List<Boolean>) {
    val colors = DarmixTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            weeks.forEachIndexed { index, done ->
                val current = index == weeks.lastIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(if (done) colors.voltText else colors.cardHigh)
                        .then(
                            if (current && !done) Modifier.border(1.5.dp, colors.voltText.copy(alpha = 0.6f), CircleShape)
                            else Modifier
                        )
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Hace ${weeks.size} sem", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
            Spacer(Modifier.weight(1f))
            Text(text = "Esta semana", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
        }
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
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        MonthNav(
            month = state.month,
            canGoNext = state.canGoNext,
            showToday = state.showToday,
            onPrevious = onPrevious,
            onNext = onNext,
            onToday = onToday
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
            WeekLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
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
private fun MonthNav(
    month: YearMonth,
    canGoNext: Boolean,
    showToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val colors = DarmixTheme.colors
    val monthName = remember(month) { month.month.getDisplayName(TextStyle.FULL, MexicoLocale).uppercase(MexicoLocale) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        DarmixIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Mes anterior",
            onClick = onPrevious,
            style = DarmixButtonStyle.Secondary,
            size = 40.dp,
            shape = CircleShape
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Overline(text = "${month.year}", color = colors.textTertiary)
            AnimatedContent(
                targetState = monthName,
                transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() },
                label = "month-name"
            ) { name ->
                Text(text = name, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            }
        }
        if (showToday) {
            Row(
                modifier = Modifier
                    .clip(DarmixShapes.Pill)
                    .background(colors.voltText.copy(alpha = 0.14f))
                    .clickable(role = Role.Button, onClick = onToday)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.Today, contentDescription = null, tint = colors.voltText, modifier = Modifier.size(14.dp))
                Text(text = "HOY", style = MaterialTheme.typography.labelSmall, color = colors.voltText)
            }
        }
        DarmixIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Mes siguiente",
            onClick = onNext,
            enabled = canGoNext,
            style = DarmixButtonStyle.Secondary,
            size = 40.dp,
            shape = CircleShape
        )
    }
}

@Composable
private fun MonthGrid(month: YearMonth, state: HistoryUiState, onSelect: (LocalDate) -> Unit) {
    val colors = DarmixTheme.colors
    val bridge = colors.voltText.copy(alpha = 0.22f)
    val offset = month.atDay(1).dayOfWeek.value - 1
    val length = month.lengthOfMonth()
    val weeks = (offset + length + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
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
                        val band = size.height * 0.46f
                        runs.forEach { (from, to) ->
                            val left = from * (cell + gap) + cell / 2f
                            val right = to * (cell + gap) + cell / 2f
                            drawRoundRect(
                                color = bridge,
                                topLeft = Offset(left, (size.height - band) / 2f),
                                size = Size(right - left, band),
                                cornerRadius = CornerRadius(band / 2f)
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
                                .aspectRatio(1f)
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
    val fill = colors.voltText
    val onFill = if (colors.isDark) colors.onVolt else Color.White
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = Motion.bouncy(),
        label = "cell-scale"
    )
    val background = when {
        status == DayStatus.COMPLETE -> fill
        status == DayStatus.PARTIAL -> fill.copy(alpha = 0.14f + 0.32f * ratio)
        isFuture -> Color.Transparent
        else -> colors.cardRaised
    }
    val content = when {
        status == DayStatus.COMPLETE -> onFill
        status == DayStatus.PARTIAL -> fill
        isFuture -> colors.textTertiary.copy(alpha = 0.55f)
        else -> colors.textSecondary
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CellShape)
            .background(background)
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, colors.textPrimary, CellShape)
                    isToday -> Modifier.border(1.5.dp, fill, CellShape)
                    else -> Modifier
                }
            )
            .clickable(enabled = !isFuture, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.titleSmall, color = content)
        if (isToday && status != DayStatus.COMPLETE) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(fill)
            )
        }
    }
}

@Composable
private fun Legend() {
    val colors = DarmixTheme.colors
    val fill = colors.voltText
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = "Completa") {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(fill))
        }
        LegendItem(label = "Parcial") {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(fill.copy(alpha = 0.3f)))
        }
        LegendItem(label = "Racha") {
            Box(Modifier.size(width = 20.dp, height = 6.dp).clip(CircleShape).background(fill.copy(alpha = 0.3f)))
        }
        LegendItem(label = "Hoy") {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).border(1.5.dp, fill, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = DarmixTheme.colors.textSecondary)
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
        DayDetailCard(
            date = date,
            isFuture = date.isAfter(state.today),
            isToday = date == state.today,
            logs = logs,
            exercises = if (logs.isEmpty()) emptyList() else state.dayExercises
        )
    }
}

@Composable
private fun DayDetailCard(
    date: LocalDate,
    isFuture: Boolean,
    isToday: Boolean,
    logs: List<DayLogRow>,
    exercises: List<HistoryExerciseRow>
) {
    val colors = DarmixTheme.colors
    val complete = logs.isNotEmpty() && logs.all { it.log.isComplete }
    val weekday = remember(date) { date.dayOfWeek.getDisplayName(TextStyle.FULL, MexicoLocale) }
    val longDate = remember(date) { date.format(LongDateFormatter) }
    val monthAbbr = remember(date) { date.month.getDisplayName(TextStyle.SHORT, MexicoLocale).replace(".", "").uppercase(MexicoLocale) }
    val (badgeBg, badgeFg) = when {
        complete -> colors.leafSoft to colors.leafText
        logs.isNotEmpty() -> colors.voltText.copy(alpha = 0.14f) to colors.voltText
        else -> colors.cardHigh to colors.textSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(
                modifier = Modifier
                    .size(width = 56.dp, height = 62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(badgeBg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = monthAbbr, style = MaterialTheme.typography.labelSmall, color = badgeFg.copy(alpha = 0.8f))
                Text(text = "${date.dayOfMonth}", style = DarmixTextStyles.MetricSmall, color = badgeFg)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Overline(text = if (isToday) "Hoy · $weekday" else weekday, color = colors.textTertiary)
                Text(text = longDate, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
            when {
                isFuture -> StatusTag(text = "Próximo", container = colors.cardHigh, content = colors.textTertiary)
                logs.isEmpty() -> StatusTag(text = "Sin registro", container = colors.cardHigh, content = colors.textTertiary)
                complete -> StatusTag(text = "Completa", container = colors.leafSoft, content = colors.leafText, icon = Icons.Rounded.Check)
                else -> StatusTag(text = "Parcial", container = colors.voltText.copy(alpha = 0.14f), content = colors.voltText)
            }
        }

        if (logs.isEmpty()) {
            EmptyDay(isFuture = isFuture)
        } else {
            logs.forEachIndexed { index, row ->
                ProgramLog(row = row, exercises = exercises.filter { it.programId == row.log.programId })
                if (index < logs.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.hairline)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDay(isFuture: Boolean) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(colors.cardRaised)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.cardHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
        }
        Text(
            text = if (isFuture) "Este día aún no llega. Prepárate." else "Sin entrenamiento registrado este día.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ProgramLog(row: DayLogRow, exercises: List<HistoryExerciseRow>) {
    val colors = DarmixTheme.colors
    val log = row.log
    val ratio = if (log.plannedSets <= 0) 0f else (log.doneSets.toFloat() / log.plannedSets).coerceIn(0f, 1f)
    val accent = if (log.isComplete) colors.leafText else colors.voltText

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ProgressRing(progress = ratio, color = accent, size = 60.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = row.programName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${log.doneSets}/${log.plannedSets} series · ${log.doneExercises}/${log.plannedExercises} ejercicios",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        if (exercises.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                exercises.forEach { exercise -> ExerciseLogRow(exercise = exercise) }
            }
        }
    }
}

@Composable
private fun ExerciseLogRow(exercise: HistoryExerciseRow) {
    val colors = DarmixTheme.colors
    val done = exercise.completedSets.coerceAtMost(exercise.sets)
    val complete = done >= exercise.sets
    val accent = if (complete) colors.leafText else colors.voltText
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        ExerciseImage(imageName = exercise.imageUri, exerciseName = exercise.name, size = 42.dp, animated = false)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SetMeter(total = exercise.sets, completed = done, accent = accent, height = 5.dp)
        }
        Text(
            text = "$done/${exercise.sets}",
            style = MaterialTheme.typography.labelLarge,
            color = if (complete) colors.leafText else colors.textSecondary,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color, size: Dp) {
    val colors = DarmixTheme.colors
    val animated by animateFloatAsState(targetValue = progress, animationSpec = Motion.gentle(), label = "ring")
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(colors.cardHigh, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
    }
}

@Composable
private fun SummaryLabel(month: YearMonth) {
    val colors = DarmixTheme.colors
    val name = remember(month) { month.month.getDisplayName(TextStyle.FULL, MexicoLocale).uppercase(MexicoLocale) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = "RESUMEN DE $name", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.hairline)
        )
    }
}

@Composable
private fun StatsGrid(stats: HistoryStats) {
    val colors = DarmixTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatTile(
                value = "${stats.monthComplete}",
                label = "Rutinas completas",
                sub = if (stats.monthPartial > 0) "+${stats.monthPartial} parciales" else null,
                accent = colors.voltText,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "${stats.monthSets}",
                label = "Series hechas",
                accent = colors.tideText,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatTile(
                value = "${stats.weekComplete}",
                label = "Completas esta semana",
                accent = colors.leafText,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "${stats.monthComplete + stats.monthPartial}",
                label = "Días con registro",
                accent = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sub: String? = null
) {
    val colors = DarmixTheme.colors
    Column(
        modifier = modifier
            .clip(DarmixShapes.Plate)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Plate)
            .drawBehind {
                drawRect(accent, size = Size(size.width * 0.35f, 3.dp.toPx()))
            }
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(text = value, style = DarmixTextStyles.Metric, color = accent)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        if (sub != null) {
            Text(text = sub, style = MaterialTheme.typography.labelMedium, color = colors.textTertiary)
        }
    }
}
