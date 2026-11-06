package com.darmix.darmixfit.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.FullscreenImageDialog
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.StatusTag
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import com.darmix.darmixfit.ui.timer.RestTimerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LEADING_ITEMS = 3
private const val UNSET = Int.MIN_VALUE

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val restVisible by viewModel.restVisible.collectAsStateWithLifecycle()
    val timer by viewModel.timerState.collectAsStateWithLifecycle()
    val restDefault by viewModel.restDefaultSeconds.collectAsStateWithLifecycle()
    val restAuto by viewModel.restAuto.collectAsStateWithLifecycle()
    val victoryVisible by viewModel.victoryVisible.collectAsStateWithLifecycle()
    val colors = DarmixTheme.colors

    var enlarged by remember { mutableStateOf<ExerciseEntity?>(null) }
    val restSeconds = if (restAuto.enabled) restAuto.seconds else restDefault

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentState by rememberUpdatedState(state)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDate()
        if (currentState.allComplete) scope.launch { listState.scrollToItem(0) }
    }

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
                TodayTitle(date = state.date, planTitle = state.planTitle)
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
                    item(key = "reactor", contentType = "reactor") {
                        TodayReactorSection(state = state, restSeconds = restSeconds)
                    }
                    item(key = "section", contentType = "section") {
                        RoutineLabel()
                    }
                    itemsIndexed(
                        items = state.items,
                        key = { _, item -> item.key },
                        contentType = { _, item -> item::class }
                    ) { index, item ->
                        val status = when {
                            item.isFinished -> EntryStatus.DONE
                            index == state.activeIndex -> EntryStatus.ACTIVE
                            else -> EntryStatus.UPCOMING
                        }
                        RoutineEntry(
                            item = item,
                            position = index + 1,
                            totalBlocks = state.totalCount,
                            status = status,
                            isFirst = index == 0,
                            isLast = index == state.items.lastIndex,
                            onComplete = {
                                when (item) {
                                    is TodayItem.Single -> viewModel.completeSet(item.entry.exercise)
                                    is TodayItem.Superset -> viewModel.completeRound(item.members.map { it.exercise })
                                }
                            },
                            onUndo = {
                                when (item) {
                                    is TodayItem.Single -> viewModel.undoSet(item.entry.exercise)
                                    is TodayItem.Superset -> viewModel.undoRound(item.members.map { it.exercise })
                                }
                            },
                            onFocus = { viewModel.focus(item) },
                            onImageClick = onImageClick,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = victoryVisible,
            enter = fadeIn(Motion.enter()),
            exit = fadeOut(Motion.exit())
        ) {
            SessionCompleteOverlay(
                planTitle = state.planTitle,
                totalSets = state.totalSets,
                totalExercises = state.totalCount,
                onStay = viewModel::dismissVictory
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

