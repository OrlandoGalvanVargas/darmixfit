package com.darmix.darmixfit.ui.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.FullscreenImageDialog
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.SkeletonBlock
import com.darmix.darmixfit.ui.components.StatusTag
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val MonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", MexicoLocale)
private val DayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private const val FIRST_DAY_INDEX = 3
private val BadgeShape = RoundedCornerShape(16.dp)

private val IntSetSaver = listSaver<MutableState<Set<Int>>, Int>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) }
)

@Composable
fun WeekScreen(
    modifier: Modifier = Modifier,
    viewModel: WeekViewModel = viewModel(factory = WeekViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = DarmixTheme.colors
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDate() }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable(saver = IntSetSaver) { mutableStateOf(emptySet()) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var enlarged by remember { mutableStateOf<ExerciseEntity?>(null) }
    val todayDow = state.today.dayOfWeek.value

    LaunchedEffect(state.loading, state.days.size) {
        if (initialized || state.loading || state.days.isEmpty()) return@LaunchedEffect
        val index = state.days.indexOfFirst { it.dayOfWeek == todayDow }
        val today = state.days.getOrNull(index)
        if (today != null && !today.isRest) expanded = expanded + todayDow
        if (index >= 3) listState.scrollToItem(FIRST_DAY_INDEX + index - 1)
        initialized = true
    }

    val onImageClick: (ExerciseEntity) -> Unit = remember { { exercise: ExerciseEntity -> enlarged = exercise } }
    val onJump: (Int) -> Unit = { dow ->
        val index = state.days.indexOfFirst { it.dayOfWeek == dow }
        if (index >= 0) {
            if (!state.days[index].isRest) expanded = expanded + dow
            scope.launch { listState.animateScrollToItem(FIRST_DAY_INDEX + index) }
        }
    }

    LazyColumn(
        state = listState,
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
        item(key = "header", contentType = "header") {
            WeekHeader(weekStart = state.weekStart)
        }

        if (state.loading) {
            items(count = 4, key = { "skeleton-$it" }, contentType = { "skeleton" }) { index ->
                SkeletonBlock(height = if (index == 0) 280.dp else 96.dp)
            }
        } else {
            item(key = "board", contentType = "board") {
                WeekBoard(state = state, onDayClick = onJump)
            }
            item(key = "section", contentType = "section") {
                SectionLabel()
            }
            items(
                items = state.days,
                key = { it.dayOfWeek },
                contentType = { if (it.isRest) "rest" else "day" }
            ) { day ->
                if (day.isRest) {
                    RestDayRow(
                        day = day,
                        isToday = day.dayOfWeek == todayDow,
                        modifier = Modifier.animateItem()
                    )
                } else {
                    val isExpanded = day.dayOfWeek in expanded
                    DayCard(
                        day = day,
                        today = state.today,
                        expanded = isExpanded,
                        onToggle = {
                            expanded = if (isExpanded) expanded - day.dayOfWeek else expanded + day.dayOfWeek
                        },
                        onImageClick = onImageClick,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    enlarged?.let { exercise ->
        exercise.imageUri?.let { image ->
            FullscreenImageDialog(imageName = image, title = exercise.name, onDismiss = { enlarged = null })
        }
    }
}

@Composable
private fun WeekHeader(weekStart: LocalDate) {
    val colors = DarmixTheme.colors
    val meta = remember(weekStart) {
        "Semana ${weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} · ${formatRange(weekStart)}"
    }
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
            Overline(text = meta, color = colors.textSecondary)
        }
        Text(text = "TU SEMANA", style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
    }
}

@Composable
private fun WeekBoard(state: WeekUiState, onDayClick: (Int) -> Unit) {
    val colors = DarmixTheme.colors
    val accent = colors.voltText
    val message = when {
        state.trainingDays == 0 -> "Semana libre. Recarga energía."
        state.completedDays >= state.trainingDays -> "Semana completa. Nivel leyenda."
        state.completedDays == 0 -> "Cada gran semana empieza con una serie."
        else -> "Llevas ${state.completedDays} de ${state.trainingDays} días completos."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .drawWithCache {
                val glowCenter = Offset(0f, 0f)
                val glowRadius = size.width * 0.7f
                val glow = Brush.radialGradient(
                    listOf(colors.volt.copy(alpha = if (colors.isDark) 0.12f else 0.22f), Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius
                )
                onDrawBehind { drawCircle(glow, radius = glowRadius, center = glowCenter) }
            }
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Overline(text = "Series de la semana", color = colors.textTertiary)
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "${state.doneSets}", style = DarmixTextStyles.MetricLarge, color = accent)
                    Text(
                        text = "/${state.plannedSets}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                BoardStat(value = "${state.trainingDays}", label = "Días de entreno", color = colors.textPrimary)
                BoardStat(value = "${state.restDays}", label = "Descanso", color = colors.tideText)
            }
        }

        WeekChart(days = state.days, todayDow = state.today.dayOfWeek.value, onDayClick = onDayClick)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

@Composable
private fun BoardStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = value, style = DarmixTextStyles.MetricSmall, color = color)
        Overline(text = label, color = DarmixTheme.colors.textTertiary)
    }
}

@Composable
private fun WeekChart(days: List<WeekDay>, todayDow: Int, onDayClick: (Int) -> Unit) {
    val colors = DarmixTheme.colors
    val accent = colors.voltText
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val byDow = remember(days) { days.associateBy { it.dayOfWeek } }
    val maxSets = remember(days) { (days.maxOfOrNull { it.totalSets } ?: 1).coerceAtLeast(1) }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, easing = Motion.Decelerate)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .drawWithCache {
                val columnWidth = size.width / 7f
                val barWidth = minOf(columnWidth * 0.46f, 22.dp.toPx())
                val chartTop = 8.dp.toPx()
                val chartBottom = size.height - 54.dp.toPx()
                val chartHeight = chartBottom - chartTop
                val minBar = 10.dp.toPx()
                val barRadius = CornerRadius(barWidth / 2f)
                val highlightRadius = CornerRadius(16.dp.toPx())
                val inset = 3.dp.toPx()
                val outline = Stroke(1.5.dp.toPx())
                val baseline = 1.dp.toPx()
                onDrawBehind {
                    val g = grow.value
                    drawRect(
                        color = colors.hairline,
                        topLeft = Offset(0f, chartBottom + 4.dp.toPx()),
                        size = Size(size.width, baseline)
                    )
                    for (dow in 1..7) {
                        val left = columnWidth * (dow - 1)
                        val cx = left + columnWidth / 2f
                        val isToday = dow == todayDow
                        if (isToday) {
                            drawRoundRect(
                                color = accent.copy(alpha = if (colors.isDark) 0.08f else 0.10f),
                                topLeft = Offset(left + inset, 0f),
                                size = Size(columnWidth - inset * 2, size.height),
                                cornerRadius = highlightRadius
                            )
                        }
                        val day = byDow[dow]
                        if (day == null || day.isRest) {
                            drawCircle(
                                color = colors.tide.copy(alpha = 0.55f),
                                radius = 3.dp.toPx(),
                                center = Offset(cx, chartBottom - 5.dp.toPx())
                            )
                            continue
                        }
                        val t = ((g - (dow - 1) * 0.06f) / 0.64f).coerceIn(0f, 1f)
                        val full = maxOf(minBar, chartHeight * day.totalSets / maxSets)
                        val h = full * t
                        if (h <= 0f) continue
                        val top = chartBottom - h
                        val barLeft = cx - barWidth / 2f
                        drawRoundRect(
                            color = if (isToday) accent.copy(alpha = 0.22f) else colors.cardHigh,
                            topLeft = Offset(barLeft, top),
                            size = Size(barWidth, h),
                            cornerRadius = barRadius
                        )
                        val ratio = day.progress ?: 0f
                        if (ratio > 0f) {
                            val fh = h * ratio
                            drawRoundRect(
                                color = accent,
                                topLeft = Offset(barLeft, chartBottom - fh),
                                size = Size(barWidth, fh),
                                cornerRadius = barRadius
                            )
                        }
                        if (isToday) {
                            drawRoundRect(
                                color = accent,
                                topLeft = Offset(barLeft, top),
                                size = Size(barWidth, h),
                                cornerRadius = barRadius,
                                style = outline
                            )
                        }
                    }
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            for (dow in 1..7) {
                val day = byDow[dow]
                val isToday = dow == todayDow
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button
                        ) { onDayClick(dow) }
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = DayLetters[dow - 1],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) accent else colors.textTertiary
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isToday) primary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${day?.date?.dayOfMonth ?: ""}",
                            style = MaterialTheme.typography.titleSmall,
                            color = when {
                                isToday -> onPrimary
                                day?.isComplete == true -> colors.leafText
                                else -> colors.textSecondary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel() {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = "PLAN DIARIO", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.hairline)
        )
        Overline(text = "Toca un día", color = colors.textTertiary)
    }
}

@Composable
private fun DateBadge(
    date: LocalDate,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    val abbreviation = remember(date) {
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, MexicoLocale).replace(".", "").uppercase(MexicoLocale)
    }
    Column(
        modifier = modifier
            .size(width = 52.dp, height = 58.dp)
            .clip(BadgeShape)
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = abbreviation, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.8f))
        Text(text = "${date.dayOfMonth}", style = DarmixTextStyles.MetricSmall, color = content)
    }
}

@Composable
private fun DayCard(
    day: WeekDay,
    today: LocalDate,
    expanded: Boolean,
    onToggle: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val isToday = day.date == today
    val isPast = day.date.isBefore(today)
    val shape = DarmixShapes.Card
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Motion.snappy(),
        label = "chevron"
    )
    val (badgeBg, badgeFg) = when {
        isToday -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        day.isComplete -> colors.leafSoft to colors.leafText
        isPast -> colors.cardHigh to colors.textSecondary
        else -> colors.cardHigh to colors.textPrimary
    }
    val meta = remember(day) {
        buildString {
            append(if (day.exerciseCount == 1) "1 ejercicio" else "${day.exerciseCount} ejercicios")
            append(" · ${day.totalSets} series")
            if (day.supersetCount > 0) append(if (day.supersetCount == 1) " · 1 superserie" else " · ${day.supersetCount} superseries")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .then(
                if (isToday) {
                    Modifier
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(colors.voltText.copy(alpha = if (colors.isDark) 0.10f else 0.08f), Color.Transparent),
                                    endY = size.height * 0.5f
                                )
                            )
                        }
                        .border(1.5.dp, colors.voltText.copy(alpha = 0.7f), shape)
                } else {
                    Modifier.border(1.dp, colors.hairline, shape)
                }
            )
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(Spacing.md + 2.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            DateBadge(date = day.date, background = badgeBg, content = badgeFg)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DayStatus(day = day, isToday = isToday, isPast = isPast)
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Contraer" else "Expandir",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }

        val progress = day.progress
        if (progress != null && progress > 0f && progress < 1f) {
            ProgressLine(progress = progress, color = colors.voltText)
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
        ) {
            ThumbStack(entries = day.entries)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
        ) {
            ExerciseList(entries = day.entries, onImageClick = onImageClick)
        }
    }
}

@Composable
private fun DayStatus(day: WeekDay, isToday: Boolean, isPast: Boolean) {
    val colors = DarmixTheme.colors
    val done = day.doneSets ?: 0
    val planned = day.plannedSets ?: day.totalSets
    when {
        isToday -> Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            StatusTag(
                text = "Hoy",
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                pulse = !day.isComplete,
                icon = if (day.isComplete) Icons.Rounded.Check else null
            )
            if (done > 0) {
                Text(text = "$done/$planned series", style = MaterialTheme.typography.labelMedium, color = colors.voltText)
            }
        }

        day.isComplete -> StatusTag(
            text = "Completado",
            container = colors.leafSoft,
            content = colors.leafText,
            icon = Icons.Rounded.Check,
            modifier = Modifier.padding(top = 3.dp)
        )

        isPast && done > 0 -> Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            StatusTag(text = "Parcial", container = colors.voltText.copy(alpha = 0.14f), content = colors.voltText)
            Text(text = "$done/$planned series", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        }

        isPast -> StatusTag(
            text = "Sin registro",
            container = colors.cardHigh,
            content = colors.textTertiary,
            modifier = Modifier.padding(top = 3.dp)
        )

        else -> Unit
    }
}

@Composable
private fun ProgressLine(progress: Float, color: Color) {
    val track = DarmixTheme.colors.cardHigh
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(track)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    size = size.copy(width = size.width * progress),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
    )
}

@Composable
private fun ThumbStack(entries: List<List<ExerciseEntity>>) {
    val colors = DarmixTheme.colors
    val exercises = remember(entries) { entries.flatten() }
    val shown = exercises.take(5)
    val extra = exercises.size - shown.size
    Row(
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        shown.forEach { exercise ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.card)
                    .padding(2.dp)
            ) {
                ExerciseImage(
                    imageName = exercise.imageUri,
                    exerciseName = exercise.name,
                    size = 38.dp,
                    animated = false
                )
            }
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.card)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+$extra", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun ExerciseList(entries: List<List<ExerciseEntity>>, onImageClick: (ExerciseEntity) -> Unit) {
    val colors = DarmixTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline)
        )
        entries.forEachIndexed { index, entry ->
            if (entry.size == 1) {
                ExerciseLine(number = index + 1, exercise = entry.first(), onImageClick = onImageClick)
            } else {
                SupersetGroup(number = index + 1, members = entry, onImageClick = onImageClick)
            }
        }
    }
}

@Composable
private fun NumberLabel(number: Int, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = number.toString().padStart(2, '0'),
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        modifier = modifier.width(28.dp)
    )
}

@Composable
private fun ExerciseLine(number: Int, exercise: ExerciseEntity, onImageClick: (ExerciseEntity) -> Unit) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        NumberLabel(number = number, color = colors.textTertiary)
        ExerciseImage(
            imageName = exercise.imageUri,
            exerciseName = exercise.name,
            size = 50.dp,
            animated = false,
            onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${exercise.sets} × ${exercise.reps}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.voltText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SupersetGroup(number: Int, members: List<ExerciseEntity>, onImageClick: (ExerciseEntity) -> Unit) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val onTide = if (colors.isDark) colors.onTide else Color.White
    val rounds = remember(members) { members.minOf { it.sets } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(tide.copy(alpha = 0.07f))
            .border(1.dp, tide.copy(alpha = 0.25f), DarmixShapes.Control)
            .padding(Spacing.sm + 2.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberLabel(number = number, color = tide)
            StatusTag(text = "Superserie", container = tide.copy(alpha = 0.16f), content = tide, icon = Icons.Rounded.Repeat)
            Spacer(Modifier.weight(1f))
            Text(text = "$rounds rondas", style = MaterialTheme.typography.labelMedium, color = tide)
        }
        members.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(DarmixShapes.Tag)
                            .background(tide),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = ('A' + index).toString(), style = MaterialTheme.typography.labelLarge, color = onTide)
                    }
                }
                ExerciseImage(
                    imageName = exercise.imageUri,
                    exerciseName = exercise.name,
                    size = 44.dp,
                    animated = false,
                    onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = exercise.reps, style = MaterialTheme.typography.bodySmall, color = tide, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RestDayRow(day: WeekDay, isToday: Boolean, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val shape = DarmixShapes.Card
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(
                width = if (isToday) 1.5.dp else 1.dp,
                color = if (isToday) colors.tideText.copy(alpha = 0.6f) else colors.hairline,
                shape = shape
            )
            .padding(Spacing.md + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        DateBadge(date = day.date, background = colors.tideSoft, content = colors.tideText)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = day.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "Recuperación · aquí también se crece", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            if (isToday) {
                StatusTag(
                    text = "Hoy",
                    container = colors.tideText.copy(alpha = 0.16f),
                    content = colors.tideText,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        Crescent(color = colors.tide, size = 26.dp)
    }
}

@Composable
private fun Crescent(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        val body = Path().apply { addOval(Rect(c, r * 0.85f)) }
        val bite = Path().apply { addOval(Rect(c + Offset(r * 0.55f, -r * 0.35f), r * 0.8f)) }
        drawPath(Path().apply { op(body, bite, PathOperation.Difference) }, color)
    }
}

private fun formatRange(start: LocalDate): String {
    val end = start.plusDays(6)
    val startMonth = start.format(MonthFormatter).replace(".", "")
    val endMonth = end.format(MonthFormatter).replace(".", "")
    return if (start.month == end.month) {
        "${start.dayOfMonth} – ${end.dayOfMonth} $endMonth"
    } else {
        "${start.dayOfMonth} $startMonth – ${end.dayOfMonth} $endMonth"
    }
}
