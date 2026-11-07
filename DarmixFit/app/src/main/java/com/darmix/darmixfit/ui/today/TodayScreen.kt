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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.FullscreenImageDialog
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import com.darmix.darmixfit.ui.timer.RestTimerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private const val LEADING_ITEMS = 3
private const val UNSET = Int.MIN_VALUE

private val RestShape = CutCornerShape(topEnd = 22.dp, bottomStart = 22.dp)
private val RestTagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val RestStripShape = CutCornerShape(10.dp)

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
    val tide = colors.tideText
    val shape = RestShape
    val glow = if (colors.isDark) 1f else 0.5f
    val bracket = tide

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind {
                    drawOutline(outline, colors.tide, alpha = 0.08f * glow, style = Stroke(16.dp.toPx(), join = StrokeJoin.Miter))
                    drawOutline(outline, colors.tide, alpha = 0.18f * glow, style = Stroke(7.dp.toPx(), join = StrokeJoin.Miter))
                }
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(colors.card, colors.tide, if (colors.isDark) 0.10f else 0.12f),
                        colors.card,
                        colors.card
                    )
                )
            )
            .border(1.5.dp, tide.copy(alpha = 0.6f), shape)
            .drawBehind {
                val inset = 10.dp.toPx()
                val arm = 12.dp.toPx()
                val w = 1.8.dp.toPx()
                listOf(
                    Offset(inset, inset) to Offset(1f, 1f),
                    Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f)
                ).forEach { (p, d) ->
                    drawLine(bracket, p, Offset(p.x + arm * d.x, p.y), strokeWidth = w)
                    drawLine(bracket, p, Offset(p.x, p.y + arm * d.y), strokeWidth = w)
                }
            }
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RechargeCore()
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .height(24.dp)
                .clip(RestTagShape)
                .background(colors.tide.copy(alpha = 0.14f))
                .border(1.dp, tide, RestTagShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(tide)
            )
            Text(
                text = "MODO RECARGA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.2.sp),
                color = tide
            )
        }
        Text(
            text = "HOY ENTRENAS\nDESCANSANDO",
            style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 36.sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = "El músculo no crece cuando levantas,\ncrece cuando sueltas.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
        RecoveryStrip(modifier = Modifier.padding(top = 22.dp))
    }
}

@Composable
private fun RechargeCore() {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val transition = rememberInfiniteTransition(label = "recharge")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "orbit"
    )
    val counter by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "counter"
    )
    val chase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "chase"
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "breath"
    )
    val track = lerp(colors.card, colors.tide, if (colors.isDark) 0.14f else 0.2f)
    val dim = colors.hairlineStrong
    val core = lerp(colors.canvasDeep, colors.tide, if (colors.isDark) 0.06f else 0.1f)
    val coreTop = lerp(colors.card, colors.tide, if (colors.isDark) 0.16f else 0.2f)
    val ink = colors.onTide

    Canvas(modifier = Modifier.size(210.dp)) {
        val c = center
        val unit = size.minDimension / 210f

        val haloR = 92f * unit * (0.92f + breath * 0.08f)
        drawCircle(
            Brush.radialGradient(listOf(colors.tide.copy(alpha = 0.22f), Color.Transparent), center = c, radius = haloR),
            radius = haloR,
            center = c
        )

        rotate(orbit, pivot = c) {
            for (k in 0 until 72) {
                val a = Math.toRadians(k * 5.0)
                val major = k % 6 == 0
                val r0 = 96f * unit
                val r1 = (if (major) 104f else 100f) * unit
                drawLine(
                    color = if (major) tide.copy(alpha = 0.8f) else dim,
                    start = Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                    end = Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                    strokeWidth = (if (major) 2f else 1.2f) * unit
                )
            }
        }

        val segR = 84f * unit
        val segW = 10f * unit
        val segments = 12
        val span = 360f / segments
        for (i in 0 until segments) {
            val start = i * span - 90f + 3f
            val phase = ((chase * segments - i) % segments + segments) % segments
            val lit = (1f - phase / 4f).coerceIn(0f, 1f)
            drawArc(track, start, span - 6f, false, Offset(c.x - segR, c.y - segR), Size(segR * 2, segR * 2), style = Stroke(segW))
            if (lit > 0f) {
                drawArc(colors.tide.copy(alpha = 0.25f * lit), start, span - 6f, false, Offset(c.x - segR, c.y - segR), Size(segR * 2, segR * 2), style = Stroke(segW + 8f * unit))
                drawArc(tide.copy(alpha = lit), start, span - 6f, false, Offset(c.x - segR, c.y - segR), Size(segR * 2, segR * 2), style = Stroke(segW))
            }
        }

        val arcR = 68f * unit
        rotate(counter, pivot = c) {
            drawArc(colors.tide.copy(alpha = 0.2f), -90f, 250f, false, Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2), style = Stroke(9f * unit, cap = StrokeCap.Round))
            drawArc(tide, -90f, 250f, false, Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2), style = Stroke(3f * unit, cap = StrokeCap.Round))
            val head = Math.toRadians(160.0)
            drawCircle(tide, radius = 4.5f * unit, center = Offset(c.x + arcR * cos(head).toFloat(), c.y + arcR * sin(head).toFloat()))
        }

        val coreR = 56f * unit
        drawCircle(Brush.radialGradient(listOf(coreTop, core), center = Offset(c.x, c.y - coreR * 0.3f), radius = coreR * 1.3f), radius = coreR, center = c)
        drawCircle(tide.copy(alpha = 0.35f), radius = coreR, center = c, style = Stroke(1.2f * unit))

        val bw = 62f * unit
        val bh = 34f * unit
        val left = c.x - bw / 2f - 3f * unit
        val top = c.y - bh / 2f
        val line = 3f * unit
        drawRoundRect(tide, Offset(left, top), Size(bw, bh), CornerRadius(5f * unit), style = Stroke(line))
        drawRoundRect(tide, Offset(left + bw + 2f * unit, c.y - 6f * unit), Size(5f * unit, 12f * unit), CornerRadius(1.5f * unit))
        val cells = 4
        val gap = 3f * unit
        val pad = 5f * unit
        val cellW = (bw - pad * 2 - gap * (cells - 1)) / cells
        val filled = chase * (cells + 1)
        for (i in 0 until cells) {
            val alpha = (filled - i).coerceIn(0f, 1f)
            drawRect(
                color = tide.copy(alpha = 0.18f + 0.82f * alpha),
                topLeft = Offset(left + pad + i * (cellW + gap), top + pad),
                size = Size(cellW, bh - pad * 2)
            )
        }
        val bolt = Path().apply {
            moveTo(c.x + 3f * unit, top + 4f * unit)
            lineTo(c.x - 9f * unit, c.y + 2f * unit)
            lineTo(c.x - 1f * unit, c.y + 2f * unit)
            lineTo(c.x - 5f * unit, top + bh - 4f * unit)
            lineTo(c.x + 9f * unit, c.y - 3f * unit)
            lineTo(c.x + 1f * unit, c.y - 3f * unit)
            close()
        }
        drawPath(bolt, ink)
        drawPath(bolt, tide, style = Stroke(1.4f * unit, join = StrokeJoin.Round))
    }
}

@Composable
private fun RecoveryStrip(modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val items = remember { listOf("MÚSCULO" to "REPARA", "ENERGÍA" to "RECARGA", "MENTE" to "RESET") }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RestStripShape)
            .background(colors.canvasDeep)
            .border(1.dp, tide.copy(alpha = 0.3f), RestStripShape)
    ) {
        items.forEachIndexed { index, (title, value) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.hairline)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
                    color = colors.textTertiary
                )
                Text(
                    text = value,
                    style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 19.sp),
                    color = tide,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
    Text(
        text = "VUELVE MAÑANA AL 100%",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.2.sp),
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 14.dp)
    )
}
