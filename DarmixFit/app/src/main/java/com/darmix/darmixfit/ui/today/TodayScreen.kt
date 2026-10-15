package com.darmix.darmixfit.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.DarmixIconButton
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.FullscreenImageDialog
import com.darmix.darmixfit.ui.components.InfoChip
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.SetBoxes
import com.darmix.darmixfit.ui.components.SetMeter
import com.darmix.darmixfit.ui.components.StatusTag
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import com.darmix.darmixfit.ui.timer.RestTimerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale
import kotlinx.coroutines.delay

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val DayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", MexicoLocale)

private const val LEADING_ITEMS = 3
private const val UNSET = Int.MIN_VALUE
private val RailWidth = 32.dp
private val NodeY = 30.dp

private enum class CardStatus { DONE, ACTIVE, UPCOMING }

private data class Accent(val fill: Color, val onFill: Color, val text: Color)

@Composable
private fun voltAccent(colors: DarmixColors) = remember(colors) {
    Accent(
        fill = colors.voltText,
        onFill = if (colors.isDark) colors.onVolt else Color.White,
        text = colors.voltText
    )
}

@Composable
private fun tideAccent(colors: DarmixColors) = remember(colors) {
    Accent(
        fill = colors.tideText,
        onFill = if (colors.isDark) colors.onTide else Color.White,
        text = colors.tideText
    )
}

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
    onSwitchProgram: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val restVisible by viewModel.restVisible.collectAsStateWithLifecycle()
    val timer by viewModel.timerState.collectAsStateWithLifecycle()
    val restDefault by viewModel.restDefaultSeconds.collectAsStateWithLifecycle()
    val restAuto by viewModel.restAuto.collectAsStateWithLifecycle()
    val allComplete by viewModel.allDayComplete.collectAsStateWithLifecycle()
    val colors = DarmixTheme.colors

    var enlarged by remember { mutableStateOf<ExerciseEntity?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    LaunchedEffect(allComplete) {
        if (!allComplete) dismissed = false
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDate() }

    val listState = rememberLazyListState()
    val lastFocused = remember { mutableIntStateOf(UNSET) }
    LaunchedEffect(state.loading, state.activeIndex) {
        if (state.loading || state.items.isEmpty()) return@LaunchedEffect
        val target = state.activeIndex
        val previous = lastFocused.intValue
        lastFocused.intValue = target
        if (target < 0) return@LaunchedEffect
        val listIndex = LEADING_ITEMS + target - 1
        if (previous == UNSET) {
            if (target > 0) listState.scrollToItem(listIndex)
        } else if (previous != target) {
            delay(320)
            listState.animateScrollToItem(listIndex)
        }
    }

    val onImageClick: (ExerciseEntity) -> Unit = remember { { exercise: ExerciseEntity -> enlarged = exercise } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.huge)
        ) {
            item(key = "header", contentType = "header") {
                TodayHeader(date = state.date, planTitle = state.planTitle)
            }

            when {
                state.loading -> items(count = 3, key = { "skeleton-$it" }, contentType = { "skeleton" }) {
                    SkeletonCard()
                }

                state.items.isEmpty() -> item(key = "rest-day", contentType = "rest") {
                    Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.lg)) {
                        RestDayCard()
                    }
                }

                else -> {
                    item(key = "score", contentType = "score") {
                        Scoreboard(state = state)
                    }
                    item(key = "section", contentType = "section") {
                        SectionLabel(blocks = state.totalCount, sets = state.totalSets)
                    }
                    itemsIndexed(
                        items = state.items,
                        key = { _, item -> item.key },
                        contentType = { _, item -> item::class }
                    ) { index, item ->
                        val status = when {
                            item.isFinished -> CardStatus.DONE
                            index == state.activeIndex -> CardStatus.ACTIVE
                            else -> CardStatus.UPCOMING
                        }
                        val accent = if (item is TodayItem.Superset) tideAccent(colors) else voltAccent(colors)
                        TimelineRow(
                            status = status,
                            isFirst = index == 0,
                            isLast = index == state.items.lastIndex,
                            accent = accent,
                            modifier = Modifier.animateItem()
                        ) {
                            when (item) {
                                is TodayItem.Single -> SingleBlock(
                                    position = index + 1,
                                    totalBlocks = state.totalCount,
                                    entry = item.entry,
                                    status = status,
                                    accent = accent,
                                    onComplete = { viewModel.completeSet(item.entry.exercise) },
                                    onUndo = { viewModel.undoSet(item.entry.exercise) },
                                    onFocus = { viewModel.focus(item) },
                                    onImageClick = onImageClick
                                )

                                is TodayItem.Superset -> SupersetBlock(
                                    position = index + 1,
                                    totalBlocks = state.totalCount,
                                    item = item,
                                    status = status,
                                    accent = accent,
                                    onComplete = { viewModel.completeRound(item.members.map { it.exercise }) },
                                    onUndo = { viewModel.undoRound(item.members.map { it.exercise }) },
                                    onFocus = { viewModel.focus(item) },
                                    onImageClick = onImageClick
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = allComplete && !dismissed,
            enter = fadeIn(Motion.enter()),
            exit = fadeOut(Motion.exit())
        ) {
            SessionCompleteOverlay(
                planTitle = state.planTitle,
                totalSets = state.totalSets,
                totalExercises = state.totalCount,
                onStay = { dismissed = true },
                onSwitchProgram = {
                    dismissed = true
                    onSwitchProgram()
                }
            )
        }
    }

    if (restVisible) {
        RestTimerDialog(
            timer = timer,
            initialSeconds = if (restAuto.enabled) restAuto.seconds else restDefault,
            autoRest = restAuto,
            onAutoRestChange = viewModel::setAutoRest,
            onStart = viewModel::startRest,
            onExtend = viewModel::extendRest,
            onCancelRunning = viewModel::cancelRest,
            onClose = viewModel::dismissRest
        )
    }

    enlarged?.let { exercise ->
        exercise.imageUri?.let { image ->
            FullscreenImageDialog(
                imageName = image,
                title = exercise.name,
                onDismiss = { enlarged = null }
            )
        }
    }
}

@Composable
private fun TodayHeader(date: LocalDate, planTitle: String?) {
    val colors = DarmixTheme.colors
    val dayName = remember(date) {
        date.dayOfWeek.getDisplayName(TextStyle.FULL, MexicoLocale)
    }
    val meta = remember(date) {
        val dayMonth = date.format(DayMonthFormatter).replace(".", "")
        "$dayName $dayMonth · Semana ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
    }

    Column(
        modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
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
        Text(
            text = (planTitle ?: dayName).uppercase(MexicoLocale),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Scoreboard(state: TodayUiState) {
    val colors = DarmixTheme.colors
    val accent = voltAccent(colors)
    val percent = (state.progress * 100).toInt()
    val units = remember(state.items) {
        state.items.map { it.totalUnits to it.completedUnits.coerceAtMost(it.totalUnits) }
    }
    val message = when {
        state.completedSets == 0 -> "Primera serie, primer paso. Arranca."
        state.allComplete -> "Día conquistado. Nada que deber."
        state.progress >= 0.75f -> "Recta final. No aflojes ahora."
        state.progress >= 0.5f -> "Pasaste la mitad. Sostén el ritmo."
        else -> "En marcha. Cada serie cuenta."
    }
    val stripe = if (colors.isDark) Color.White.copy(alpha = 0.025f) else Color.Black.copy(alpha = 0.035f)

    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.screen, vertical = Spacing.md)
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .drawWithCache {
                val step = 14.dp.toPx()
                val lines = Path().apply {
                    var x = -size.height
                    while (x < size.width) {
                        moveTo(x, size.height)
                        lineTo(x + size.height, 0f)
                        x += step
                    }
                }
                val glowCenter = Offset(size.width, 0f)
                val glowRadius = size.width * 0.75f
                val glow = Brush.radialGradient(
                    listOf(colors.volt.copy(alpha = if (colors.isDark) 0.14f else 0.26f), Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius
                )
                val stroke = Stroke(1.dp.toPx())
                onDrawBehind {
                    drawPath(lines, stripe, style = stroke)
                    drawCircle(glow, radius = glowRadius, center = glowCenter)
                }
            }
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Overline(text = "Series del día", color = colors.textTertiary)
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = state.completedSets,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                                    (slideOutVertically { if (up) -it else it } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "sets-counter"
                    ) { value ->
                        Text(text = "$value", style = DarmixTextStyles.MetricHero, color = accent.text)
                    }
                    Text(
                        text = "/${state.totalSets}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScoreStat(value = "$percent%", label = "Completado")
                ScoreStat(value = "${state.doneCount}/${state.totalCount}", label = "Ejercicios")
            }
        }

        DayTicks(
            units = units,
            activeIndex = state.activeIndex,
            fill = accent.fill,
            track = colors.cardHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = accent.text, modifier = Modifier.size(18.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ScoreStat(value: String, label: String) {
    val colors = DarmixTheme.colors
    Column(horizontalAlignment = Alignment.End) {
        Text(text = value, style = DarmixTextStyles.MetricSmall, color = colors.textPrimary)
        Overline(text = label, color = colors.textTertiary)
    }
}

@Composable
private fun DayTicks(
    units: List<Pair<Int, Int>>,
    activeIndex: Int,
    fill: Color,
    track: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalTicks = units.sumOf { it.first }
        if (totalTicks == 0) return@Canvas
        val groupGap = 9.dp.toPx()
        val tickGap = 3.dp.toPx()
        val available = size.width - groupGap * (units.size - 1) - tickGap * (totalTicks - units.size)
        val tickWidth = (available / totalTicks).coerceAtLeast(1f)
        val radius = CornerRadius(minOf(tickWidth / 2f, 3.dp.toPx()))
        var x = 0f
        units.forEachIndexed { group, (total, done) ->
            repeat(total) { i ->
                val filled = i < done
                val next = group == activeIndex && i == done
                val h = when {
                    filled -> size.height
                    next -> size.height * 0.78f
                    else -> size.height * 0.5f
                }
                drawRoundRect(
                    color = when {
                        filled -> fill
                        next -> fill.copy(alpha = 0.4f)
                        else -> track
                    },
                    topLeft = Offset(x, size.height - h),
                    size = Size(tickWidth, h),
                    cornerRadius = radius
                )
                x += tickWidth + tickGap
            }
            x += groupGap - tickGap
        }
    }
}

@Composable
private fun SectionLabel(blocks: Int, sets: Int) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.lg, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = "RUTA DEL DÍA", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.hairline)
        )
        Overline(text = "$blocks bloques · $sets series", color = colors.textTertiary)
    }
}

@Composable
private fun TimelineRow(
    status: CardStatus,
    isFirst: Boolean,
    isLast: Boolean,
    accent: Accent,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = DarmixTheme.colors
    val pulse: State<Float>? = if (status == CardStatus.ACTIVE) {
        rememberInfiniteTransition(label = "node").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
            label = "node-pulse"
        )
    } else {
        null
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.sm + 2.dp, end = Spacing.screen)
            .drawWithCache {
                val x = RailWidth.toPx() / 2f
                val y = NodeY.toPx()
                val lineWidth = 2.dp.toPx()
                val check = Path().apply {
                    val s = 5.dp.toPx()
                    moveTo(x - s * 0.9f, y + s * 0.05f)
                    lineTo(x - s * 0.25f, y + s * 0.7f)
                    lineTo(x + s * 0.95f, y - s * 0.6f)
                }
                val checkStroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val reached = accent.fill.copy(alpha = 0.85f)
                onDrawBehind {
                    val node = Offset(x, y)
                    if (!isFirst) {
                        drawLine(
                            color = if (status != CardStatus.UPCOMING) reached else colors.hairline,
                            start = Offset(x, 0f),
                            end = Offset(x, y),
                            strokeWidth = lineWidth
                        )
                    }
                    if (!isLast) {
                        drawLine(
                            color = if (status == CardStatus.DONE) reached else colors.hairline,
                            start = Offset(x, y),
                            end = Offset(x, size.height),
                            strokeWidth = lineWidth
                        )
                    }
                    when (status) {
                        CardStatus.DONE -> {
                            drawCircle(accent.fill, radius = 11.dp.toPx(), center = node)
                            drawPath(check, accent.onFill, style = checkStroke)
                        }

                        CardStatus.ACTIVE -> {
                            val p = pulse?.value ?: 0f
                            drawCircle(
                                accent.fill.copy(alpha = (1f - p) * 0.4f),
                                radius = 11.dp.toPx() + 12.dp.toPx() * p,
                                center = node
                            )
                            drawCircle(colors.canvas, radius = 12.dp.toPx(), center = node)
                            drawCircle(accent.fill, radius = 12.dp.toPx(), center = node, style = Stroke(3.dp.toPx()))
                            drawCircle(accent.fill, radius = 5.dp.toPx(), center = node)
                        }

                        CardStatus.UPCOMING -> {
                            drawCircle(colors.canvas, radius = 8.dp.toPx(), center = node)
                            drawCircle(colors.hairlineStrong, radius = 8.dp.toPx(), center = node, style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }
            }
    ) {
        Spacer(Modifier.width(RailWidth + Spacing.sm))
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.md)
                .animateContentSize(animationSpec = Motion.snappy())
        ) {
            content()
        }
    }
}

@Composable
private fun FocusShell(accent: Accent, content: @Composable ColumnScope.() -> Unit) {
    val colors = DarmixTheme.colors
    val shape = DarmixShapes.Card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(accent.fill.copy(alpha = if (colors.isDark) 0.13f else 0.10f), Color.Transparent),
                        endY = size.height * 0.55f
                    )
                )
            }
            .border(1.5.dp, accent.fill.copy(alpha = 0.7f), shape)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md + 2.dp),
        content = content
    )
}

@Composable
private fun FocusTopBar(position: Int, totalBlocks: Int, accent: Accent, label: String? = null) {
    val colors = DarmixTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusTag(text = "Ahora", container = accent.fill, content = accent.onFill, pulse = true)
        if (label != null) {
            StatusTag(
                text = label,
                container = accent.fill.copy(alpha = 0.14f),
                content = accent.text,
                icon = Icons.Rounded.Repeat
            )
        }
        Spacer(Modifier.weight(1f))
        Overline(text = "Bloque $position de $totalBlocks", color = colors.textTertiary)
    }
}

@Composable
private fun FocusActions(
    unitLabel: String,
    completed: Int,
    accent: Accent,
    useAccentButton: Boolean,
    onComplete: () -> Unit,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
    ) {
        DarmixIconButton(
            icon = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = "Deshacer",
            onClick = onUndo,
            enabled = completed > 0,
            size = 58.dp
        )
        DarmixButton(
            text = "COMPLETAR ${unitLabel.uppercase()} ${completed + 1}",
            onClick = onComplete,
            icon = Icons.Rounded.Check,
            height = 58.dp,
            containerColor = if (useAccentButton) accent.fill else Color.Unspecified,
            contentColor = if (useAccentButton) accent.onFill else Color.Unspecified,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TipBox(text: String, accent: Accent) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(colors.cardRaised)
            .drawBehind {
                drawRect(accent.fill, size = Size(3.dp.toPx(), size.height))
            }
            .padding(start = Spacing.md + 3.dp, end = Spacing.md, top = Spacing.md - 2.dp, bottom = Spacing.md - 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = accent.text,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SingleBlock(
    position: Int,
    totalBlocks: Int,
    entry: ExerciseProgress,
    status: CardStatus,
    accent: Accent,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onFocus: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit
) {
    val exercise = entry.exercise
    val imageClick: (() -> Unit)? = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
    when (status) {
        CardStatus.DONE -> DoneRow(
            title = exercise.name,
            detail = "${exercise.sets}/${exercise.sets} series",
            accent = accent,
            onUndo = onUndo
        )

        CardStatus.UPCOMING -> UpcomingRow(
            title = exercise.name,
            detail = "${exercise.sets} × ${exercise.reps}",
            total = exercise.sets,
            completed = entry.completed,
            accent = accent,
            image = {
                ExerciseImage(imageName = exercise.imageUri, exerciseName = exercise.name, size = 54.dp, onClick = imageClick)
            },
            onStart = onFocus
        )

        CardStatus.ACTIVE -> FocusShell(accent = accent) {
            FocusTopBar(position = position, totalBlocks = totalBlocks, accent = accent)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md + 2.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseImage(imageName = exercise.imageUri, exerciseName = exercise.name, size = 92.dp, onClick = imageClick)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = DarmixTheme.colors.textPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
                        InfoChip(text = "${exercise.sets} series")
                        InfoChip(text = exercise.reps, content = accent.text)
                    }
                }
            }
            if (exercise.note.isNotBlank()) TipBox(text = exercise.note, accent = accent)
            SetBoxes(total = exercise.sets, completed = entry.completed, accent = accent.fill, onAccent = accent.onFill)
            FocusActions(
                unitLabel = "Serie",
                completed = entry.completed,
                accent = accent,
                useAccentButton = false,
                onComplete = onComplete,
                onUndo = onUndo
            )
        }
    }
}

@Composable
private fun SupersetBlock(
    position: Int,
    totalBlocks: Int,
    item: TodayItem.Superset,
    status: CardStatus,
    accent: Accent,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onFocus: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit
) {
    val names = remember(item.members) { item.members.joinToString(" + ") { it.exercise.name } }
    when (status) {
        CardStatus.DONE -> DoneRow(
            title = names,
            detail = "Superserie · ${item.totalRounds}/${item.totalRounds} rondas",
            accent = accent,
            onUndo = onUndo
        )

        CardStatus.UPCOMING -> UpcomingRow(
            title = names,
            detail = "Superserie · ${item.totalRounds} rondas",
            total = item.totalRounds,
            completed = item.completedRounds,
            accent = accent,
            image = { SupersetBadge(count = item.members.size, accent = accent) },
            onStart = onFocus
        )

        CardStatus.ACTIVE -> FocusShell(accent = accent) {
            FocusTopBar(position = position, totalBlocks = totalBlocks, accent = accent, label = "Superserie")
            Column {
                item.members.forEachIndexed { index, member ->
                    SupersetMemberRow(
                        letter = ('A' + index).toString(),
                        exercise = member.exercise,
                        isFirst = index == 0,
                        isLast = index == item.members.lastIndex,
                        accent = accent,
                        onImageClick = onImageClick
                    )
                }
            }
            SetBoxes(total = item.totalRounds, completed = item.completedRounds, accent = accent.fill, onAccent = accent.onFill)
            FocusActions(
                unitLabel = "Ronda",
                completed = item.completedRounds,
                accent = accent,
                useAccentButton = true,
                onComplete = onComplete,
                onUndo = onUndo
            )
        }
    }
}

@Composable
private fun SupersetBadge(count: Int, accent: Accent) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(DarmixShapes.Control)
            .background(accent.fill.copy(alpha = 0.14f))
            .border(1.dp, accent.fill.copy(alpha = 0.4f), DarmixShapes.Control),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "×$count", style = DarmixTextStyles.MetricSmall, color = accent.text)
    }
}

@Composable
private fun SupersetMemberRow(
    letter: String,
    exercise: ExerciseEntity,
    isFirst: Boolean,
    isLast: Boolean,
    accent: Accent,
    onImageClick: (ExerciseEntity) -> Unit
) {
    val colors = DarmixTheme.colors
    val rail = accent.fill.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(80.dp)
                .drawBehind {
                    val x = size.width / 2f
                    val mid = size.height / 2f
                    val w = 2.dp.toPx()
                    if (!isFirst) drawLine(rail, Offset(x, 0f), Offset(x, mid), w)
                    if (!isLast) drawLine(rail, Offset(x, mid), Offset(x, size.height), w)
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(DarmixShapes.Tag)
                    .background(accent.fill),
                contentAlignment = Alignment.Center
            ) {
                Text(text = letter, style = MaterialTheme.typography.labelLarge, color = accent.onFill)
            }
        }
        ExerciseImage(
            imageName = exercise.imageUri,
            exerciseName = exercise.name,
            size = 60.dp,
            onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = exercise.reps, style = MaterialTheme.typography.bodyMedium, color = accent.text, maxLines = 1)
            if (exercise.note.isNotBlank()) {
                Text(
                    text = exercise.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UpcomingRow(
    title: String,
    detail: String,
    total: Int,
    completed: Int,
    accent: Accent,
    image: @Composable () -> Unit,
    onStart: () -> Unit
) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Card)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Card)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(Modifier.graphicsLayer { alpha = 0.75f }) { image() }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (completed > 0) "$detail · $completed/$total hechas" else detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (completed > 0) {
                SetMeter(
                    total = total,
                    completed = completed,
                    accent = accent.fill.copy(alpha = 0.6f),
                    height = 4.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        DarmixButton(
            text = if (completed > 0) "Seguir" else "Empezar",
            onClick = onStart,
            icon = Icons.Rounded.PlayArrow,
            style = DarmixButtonStyle.Ghost,
            contentColor = colors.textPrimary,
            height = 40.dp,
            shape = CircleShape,
            haptic = HapticFeedbackType.ContextClick
        )
    }
}

@Composable
private fun DoneRow(title: String, detail: String, accent: Accent, onUndo: () -> Unit) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Card)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairline, DarmixShapes.Card)
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm + 2.dp, bottom = Spacing.sm + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = accent.text)
        }
        DarmixIconButton(
            icon = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = "Deshacer última serie",
            onClick = onUndo,
            style = DarmixButtonStyle.Secondary,
            size = 38.dp,
            shape = CircleShape
        )
    }
}

@Composable
private fun SkeletonCard() {
    val colors = DarmixTheme.colors
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmer"
    )
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm)
            .fillMaxWidth()
            .height(140.dp)
            .clip(DarmixShapes.Card)
            .background(colors.card)
            .drawBehind {
                val x = size.width * shimmer.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, colors.cardHigh, Color.Transparent),
                        start = Offset(x - size.width * 0.4f, 0f),
                        end = Offset(x + size.width * 0.4f, size.height)
                    )
                )
            }
    )
}

@Composable
private fun RestDayCard() {
    val colors = DarmixTheme.colors
    val transition = rememberInfiniteTransition(label = "rest")
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "breath"
    )
    val stars = remember {
        listOf(
            Triple(0.14f, 0.22f, 0.0f),
            Triple(0.84f, 0.18f, 0.33f),
            Triple(0.78f, 0.80f, 0.66f),
            Triple(0.20f, 0.76f, 0.5f),
            Triple(0.50f, 0.06f, 0.15f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(Brush.verticalGradient(listOf(colors.tideSoft, colors.card)))
            .border(1.dp, colors.tide.copy(alpha = 0.25f), DarmixShapes.Hero)
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .drawWithCache {
                    val c = size.center
                    val crescentR = size.minDimension * 0.22f
                    val crescent = Path().apply {
                        op(
                            Path().apply { addOval(Rect(c, crescentR)) },
                            Path().apply { addOval(Rect(c + Offset(crescentR * 0.62f, -crescentR * 0.38f), crescentR * 0.98f)) },
                            PathOperation.Difference
                        )
                    }
                    val ring = Stroke(width = 1.dp.toPx())
                    onDrawBehind {
                        val b = breath.value
                        val glowR = size.minDimension * (0.40f + b * 0.06f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(colors.tide.copy(alpha = 0.26f), Color.Transparent),
                                center = c,
                                radius = glowR
                            ),
                            radius = glowR,
                            center = c
                        )
                        drawCircle(colors.tide.copy(alpha = 0.18f), radius = size.minDimension * 0.48f, center = c, style = ring)
                        drawCircle(colors.tide.copy(alpha = 0.34f), radius = size.minDimension * 0.36f, center = c, style = ring)
                        stars.forEach { (sx, sy, offset) ->
                            val twinkle = (b + offset) % 1f
                            drawCircle(
                                color = colors.tide.copy(alpha = 0.25f + twinkle * 0.6f),
                                radius = 1.5.dp.toPx() + twinkle * 1.dp.toPx(),
                                center = Offset(size.width * sx, size.height * sy)
                            )
                        }
                        drawPath(crescent, colors.tide)
                    }
                }
        )

        StatusTag(text = "Recuperación", container = colors.tide.copy(alpha = 0.16f), content = colors.tideText)

        Text(
            text = "HOY ENTRENAS\nDESCANSANDO",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "El músculo no crece cuando levantas,\ncrece cuando sueltas.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Box(
            Modifier
                .width(40.dp)
                .height(2.dp)
                .background(colors.tide.copy(alpha = 0.5f), CircleShape)
        )
        Overline(text = "Vuelve mañana al 100%", color = colors.tideText)
    }
}

