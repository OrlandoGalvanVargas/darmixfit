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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.FullscreenImageDialog
import com.darmix.darmixfit.ui.components.SkeletonBlock
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val DayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private const val FIRST_DAY_INDEX = 3
private const val SEGMENT_GAP = 4f
private const val SEGMENT_SPAN = 360f / 7f

private val CardShape = CutCornerShape(14.dp)
private val TagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val ThumbShape = CutCornerShape(6.dp)
private val ImageShape = CutCornerShape(8.dp)
private val SupersetShape = CutCornerShape(12.dp)
private val PillShape = CutCornerShape(10.dp)

private val GaugeSize = 260.dp
private val RingRadius = 84.5.dp
private val RingWidth = 13.dp
private val DiscRadius = 116.dp
private val TickInner = 68.dp
private val TickOuter = 71.5.dp
private val TickMajor = 74.dp
private val LabelRadius = 105.6.dp
private val GaugeLift = 28.dp
private val GaugeTrim = 14.dp

private val IntSetSaver = listSaver<MutableState<Set<Int>>, Int>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) }
)

private enum class DayState { TODAY, COMPLETE, PARTIAL, MISSED, UPCOMING, REST }

private fun WeekDay.state(today: LocalDate): DayState = when {
    isRest -> DayState.REST
    date == today -> DayState.TODAY
    isComplete -> DayState.COMPLETE
    date.isBefore(today) && (doneSets ?: 0) > 0 -> DayState.PARTIAL
    date.isBefore(today) -> DayState.MISSED
    else -> DayState.UPCOMING
}

private fun stateColor(state: DayState, colors: DarmixColors): Color = when (state) {
    DayState.TODAY -> colors.voltText
    DayState.COMPLETE -> colors.leafText
    DayState.PARTIAL -> colors.voltText.copy(alpha = 0.6f)
    DayState.REST -> colors.tideText
    DayState.MISSED, DayState.UPCOMING -> colors.hairlineStrong
}

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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header", contentType = "header") {
            Text(
                text = "TU SEMANA",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }

        if (state.loading) {
            items(count = 4, key = { "skeleton-$it" }, contentType = { "skeleton" }) { index ->
                SkeletonBlock(height = if (index == 0) 320.dp else 96.dp)
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
                    RestDayCard(
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
private fun WeekBoard(state: WeekUiState, onDayClick: (Int) -> Unit) {
    val colors = DarmixTheme.colors
    val message = when {
        state.trainingDays == 0 -> "Semana libre. Recarga energía."
        state.completedDays >= state.trainingDays -> "Semana completa. Nivel leyenda."
        state.completedDays == 0 -> "Cada gran semana empieza con una serie."
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            BoardStat(value = "${state.trainingDays}", label = "ENTRENO", color = colors.textPrimary, alignEnd = false)
            Spacer(Modifier.weight(1f))
            BoardStat(value = "${state.restDays}", label = "DESCANSO", color = colors.tideText, alignEnd = true)
        }
        WeekGauge(
            days = state.days,
            today = state.today,
            doneSets = state.doneSets,
            plannedSets = state.plannedSets,
            onDayClick = onDayClick,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val lift = GaugeLift.roundToPx()
                val trim = lift + GaugeTrim.roundToPx()
                layout(placeable.width, placeable.height - trim) {
                    placeable.place(0, -lift)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(colors.card)
                .border(1.dp, colors.voltText.copy(alpha = 0.4f), PillShape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                    color = colors.textSecondary
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        append("Llevas ")
                        withStyle(SpanStyle(color = colors.voltText, fontWeight = FontWeight.ExtraBold)) {
                            append("${state.completedDays} de ${state.trainingDays}")
                        }
                        append(" días completos")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun BoardStat(value: String, label: String, color: Color, alignEnd: Boolean) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = value,
            style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 34.sp),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.6.sp),
            color = DarmixTheme.colors.textTertiary
        )
    }
}

@Composable
private fun WeekGauge(
    days: List<WeekDay>,
    today: LocalDate,
    doneSets: Int,
    plannedSets: Int,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val measurer = rememberTextMeasurer()
    val byDow = remember(days) { days.associateBy { it.dayOfWeek } }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, easing = Motion.Decelerate)) }
    val todayDow = today.dayOfWeek.value
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp)
    val track = if (colors.isDark) lerp(colors.card, colors.hairline, 0.6f) else colors.hairline
    val tickDim = colors.hairlineStrong
    val pastel = lerp(colors.volt, Color.White, if (colors.isDark) 0.5f else 0.35f)
    val todayComplete = byDow[todayDow]?.isComplete == true
    val todayArc = if (todayComplete) colors.volt else pastel
    val todayTicks = when {
        !colors.isDark -> colors.voltText
        todayComplete -> pastel
        else -> colors.volt
    }
    val todayHalo = colors.isDark
    val disc = if (colors.isDark) {
        Brush.radialGradient(listOf(lerp(colors.card, colors.volt, 0.06f), colors.canvasDeep))
    } else {
        Brush.radialGradient(listOf(Color.White, colors.card))
    }

    Box(
        modifier = modifier
            .size(GaugeSize)
            .pointerInput(byDow) {
                detectTapGestures { position ->
                    val dx = position.x - size.width / 2f
                    val dy = position.y - size.height / 2f
                    val distance = hypot(dx, dy)
                    if (distance < RingRadius.toPx() - 30.dp.toPx() || distance > LabelRadius.toPx() + 16.dp.toPx()) return@detectTapGestures
                    var angle = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
                    if (angle < 0f) angle += 360f
                    val dow = (angle / SEGMENT_SPAN).toInt().coerceIn(0, 6) + 1
                    if (byDow.containsKey(dow)) onDayClick(dow)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            drawCircle(disc, radius = DiscRadius.toPx(), center = c)
            drawCircle(colors.hairlineStrong, radius = DiscRadius.toPx(), center = c, style = Stroke(1.dp.toPx()))

            val todayStart = (todayDow - 1) * SEGMENT_SPAN
            val todayEnd = todayDow * SEGMENT_SPAN
            for (k in 0 until 60) {
                val a = k * 6f
                val lit = a >= todayStart && a <= todayEnd
                val outer = when {
                    lit -> TickMajor.toPx()
                    k % 5 == 0 -> TickMajor.toPx()
                    else -> TickOuter.toPx()
                }
                val inner = TickInner.toPx()
                drawLine(
                    color = if (lit) todayTicks else tickDim,
                    start = polar(c, inner, a),
                    end = polar(c, outer, a),
                    strokeWidth = if (lit) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Butt
                )
            }

            val ring = RingRadius.toPx()
            val stroke = RingWidth.toPx()
            val arcTopLeft = Offset(c.x - ring, c.y - ring)
            val arcSize = Size(ring * 2f, ring * 2f)
            val g = grow.value
            for (dow in 1..7) {
                val start = (dow - 1) * SEGMENT_SPAN + SEGMENT_GAP
                val sweep = SEGMENT_SPAN - SEGMENT_GAP * 2f
                drawArc(track, start - 90f, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke))
                val day = byDow[dow] ?: continue
                if (day.isRest) {
                    drawArc(colors.tideText.copy(alpha = 0.35f), start - 90f, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke))
                    continue
                }
                val t = ((g - (dow - 1) * 0.06f) / 0.64f).coerceIn(0f, 1f)
                val fill = (day.progress ?: 0f) * t
                if (fill <= 0f) continue
                val color = if (dow == todayDow) todayArc else colors.volt
                if (dow != todayDow || todayHalo) {
                    drawArc(color.copy(alpha = 0.22f), start - 90f, sweep * fill, false, arcTopLeft, arcSize, style = Stroke(stroke + 8.dp.toPx()))
                }
                drawArc(color, start - 90f, sweep * fill, false, arcTopLeft, arcSize, style = Stroke(stroke))
            }

            val labelRadius = LabelRadius.toPx()
            for (dow in 1..7) {
                val day = byDow[dow]
                val labelColor = when (day?.state(today)) {
                    DayState.TODAY -> colors.voltText
                    DayState.REST -> colors.tideText
                    DayState.COMPLETE -> colors.leafText
                    else -> colors.textTertiary
                }
                val layout = measurer.measure(DayLetters[dow - 1], labelStyle.copy(color = labelColor))
                val p = polar(c, labelRadius, (dow - 0.5f) * SEGMENT_SPAN)
                drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SERIES",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = colors.textTertiary
            )
            Text(
                text = "$doneSets",
                style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 62.sp),
                color = colors.voltText
            )
            Text(
                text = "DE $plannedSets",
                style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 22.sp),
                color = colors.textTertiary
            )
        }
    }
}

private fun polar(center: Offset, radius: Float, degrees: Float): Offset {
    val radians = Math.toRadians((degrees - 90f).toDouble())
    return Offset(center.x + radius * cos(radians).toFloat(), center.y + radius * sin(radians).toFloat())
}

@Composable
private fun SectionLabel() {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = "RUTINA POR DÍA", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.hairline)
        )
    }
}

private fun Modifier.dayFrame(
    shape: Shape,
    fill: Color,
    border: Color,
    glow: Color?
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        if (glow != null) {
            drawOutline(outline, glow, alpha = 0.08f, style = Stroke(16.dp.toPx(), join = StrokeJoin.Miter))
            drawOutline(outline, glow, alpha = 0.18f, style = Stroke(7.dp.toPx(), join = StrokeJoin.Miter))
            drawOutline(outline, glow, alpha = 0.45f, style = Stroke(3.dp.toPx(), join = StrokeJoin.Miter))
        }
        drawOutline(outline, fill)
        drawOutline(outline, border, style = Stroke(if (glow != null) 1.5.dp.toPx() else 1.dp.toPx(), join = StrokeJoin.Miter))
    }
}

@Composable
private fun DateColumn(date: LocalDate, accent: Color, number: Color) {
    val abbreviation = remember(date) {
        date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, MexicoLocale).replace(".", "").uppercase(MexicoLocale)
    }
    Column(modifier = Modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = abbreviation,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.3.sp),
            color = accent
        )
        Text(
            text = "${date.dayOfMonth}",
            style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 30.sp),
            color = number
        )
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
    val state = day.state(today)
    val isToday = state == DayState.TODAY
    val strip = stateColor(state, colors)
    val muted = state == DayState.MISSED || state == DayState.UPCOMING
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Motion.snappy(),
        label = "chevron"
    )
    val meta = remember(day) {
        buildString {
            append(if (day.exerciseCount == 1) "1 ejercicio" else "${day.exerciseCount} ejercicios")
            append(" · ${day.totalSets} series")
            if (day.supersetCount > 0) append(if (day.supersetCount == 1) " · 1 superserie" else " · ${day.supersetCount} superseries")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .dayFrame(
                shape = CardShape,
                fill = colors.card,
                border = if (isToday) colors.voltText else colors.hairline,
                glow = if (isToday) colors.volt else null
            )
            .clip(CardShape)
            .clickable(role = Role.Button, onClick = onToggle)
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(strip)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateColumn(
                    date = day.date,
                    accent = if (muted) colors.textTertiary else strip,
                    number = if (muted) colors.textSecondary else colors.textPrimary
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.hairline)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = day.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 19.sp),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    DayStatus(day = day, state = state)
                }
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }

            val progress = day.progress
            if (state == DayState.PARTIAL && progress != null) {
                ProgressLine(
                    progress = progress,
                    color = colors.voltText,
                    modifier = Modifier.padding(start = 72.dp, end = 14.dp, bottom = 10.dp)
                )
            }

            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
            ) {
                ThumbRow(
                    entries = day.entries,
                    modifier = Modifier.padding(start = 72.dp, end = 14.dp, bottom = 12.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
            ) {
                ExerciseList(
                    entries = day.entries,
                    onImageClick = onImageClick,
                    modifier = Modifier.padding(start = 12.dp, end = 14.dp, bottom = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun DayStatus(day: WeekDay, state: DayState) {
    val colors = DarmixTheme.colors
    val done = day.doneSets ?: 0
    val planned = day.plannedSets ?: day.totalSets
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        when (state) {
            DayState.TODAY -> {
                HudTag(
                    text = "HOY",
                    color = colors.volt,
                    filled = true,
                    icon = if (day.isComplete) Icons.Rounded.Check else null
                )
                if (done > 0) {
                    Text(
                        text = "$done/$planned series",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.voltText
                    )
                }
            }
            DayState.COMPLETE -> HudTag(text = "COMPLETADO", color = colors.leafText)
            DayState.PARTIAL -> {
                HudTag(text = "PARCIAL", color = colors.voltText)
                Text(
                    text = "$done/$planned series",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
            DayState.MISSED -> HudTag(text = "NO REALIZADO", color = colors.textSecondary)
            DayState.UPCOMING -> HudTag(text = "PENDIENTE", color = colors.textTertiary)
            DayState.REST -> Unit
        }
    }
}

@Composable
private fun HudTag(text: String, color: Color, filled: Boolean = false, icon: ImageVector? = null) {
    val colors = DarmixTheme.colors
    val content = if (filled) colors.onVolt else color
    val base = Modifier
        .height(24.dp)
        .clip(TagShape)
    val styled = if (filled) {
        base.background(color)
    } else {
        base
            .background(colors.card)
            .border(1.dp, color, TagShape)
    }
    Row(
        modifier = styled.padding(start = 9.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.5.sp),
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgressLine(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val track = DarmixTheme.colors.hairline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(track)
            .drawBehind {
                drawRect(color = color, size = size.copy(width = size.width * progress))
            }
    )
}

@Composable
private fun ThumbRow(entries: List<List<ExerciseEntity>>, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val exercises = remember(entries) { entries.flatten() }
    val shown = exercises.take(5)
    val extra = exercises.size - shown.size
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        shown.forEach { exercise ->
            Box(modifier = Modifier.clip(ThumbShape)) {
                ExerciseImage(
                    imageName = exercise.imageUri,
                    exerciseName = exercise.name,
                    size = 32.dp,
                    animated = false
                )
            }
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(ThumbShape)
                    .background(colors.cardHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+$extra", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ExerciseList(
    entries: List<List<ExerciseEntity>>,
    onImageClick: (ExerciseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
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
        style = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 26.sp),
        color = color,
        modifier = modifier.width(30.dp)
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
        Box(modifier = Modifier.clip(ImageShape)) {
            ExerciseImage(
                imageName = exercise.imageUri,
                exerciseName = exercise.name,
                size = 52.dp,
                animated = false,
                onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
            )
        }
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
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
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
            .clip(SupersetShape)
            .background(tide.copy(alpha = 0.07f))
            .border(1.dp, tide.copy(alpha = 0.45f), SupersetShape)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberLabel(number = number, color = tide)
            HudTag(text = "SUPERSERIE", color = tide, icon = Icons.Rounded.Repeat)
            Spacer(Modifier.weight(1f))
            Text(
                text = "$rounds RONDAS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                color = tide
            )
        }
        members.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp))
                        .background(tide),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ('A' + index).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = onTide
                    )
                }
                Box(modifier = Modifier.clip(CutCornerShape(7.dp))) {
                    ExerciseImage(
                        imageName = exercise.imageUri,
                        exerciseName = exercise.name,
                        size = 46.dp,
                        animated = false,
                        onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = exercise.reps,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = tide,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun RestDayCard(day: WeekDay, isToday: Boolean, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .dayFrame(
                shape = CardShape,
                fill = lerp(colors.card, colors.tide, 0.03f),
                border = if (isToday) tide.copy(alpha = 0.7f) else colors.hairline,
                glow = null
            )
            .clip(CardShape)
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(tide)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateColumn(date = day.date, accent = tide, number = colors.textPrimary)
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.hairline)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 19.sp),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Recuperación · aquí también se crece",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                if (isToday) {
                    Box(modifier = Modifier.padding(top = 6.dp)) {
                        HudTag(text = "HOY", color = tide)
                    }
                }
            }
            ChargingBattery(size = 28.dp)
        }
    }
}

@Composable
private fun ChargingBattery(size: Dp) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val deep = lerp(colors.tide, Color.Black, 0.25f)
    val bolt = colors.onTide
    val boltPath = remember {
        Path().apply {
            moveTo(11.9f, 9.3f)
            lineTo(9.2f, 12.4f)
            lineTo(12.6f, 12.4f)
            lineTo(10.1f, 15.5f)
        }
    }
    Canvas(modifier = Modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        val line = 1.8f * unit
        drawCircle(tide.copy(alpha = 0.12f), radius = 11f * unit, center = center)
        drawRoundRect(
            color = tide,
            topLeft = Offset(2.5f * unit, 7f * unit),
            size = Size(17f * unit, 10f * unit),
            cornerRadius = CornerRadius(2f * unit),
            style = Stroke(line)
        )
        drawLine(tide, Offset(22f * unit, 10.5f * unit), Offset(22f * unit, 13.5f * unit), strokeWidth = line, cap = StrokeCap.Round)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(deep, colors.tide), startX = 4.4f * unit, endX = 17.6f * unit),
            topLeft = Offset(4.4f * unit, 8.9f * unit),
            size = Size(13.2f * unit, 6.2f * unit),
            cornerRadius = CornerRadius(0.8f * unit)
        )
        val scaled = Path().apply { addPath(boltPath) }
        scaled.transform(Matrix().apply { scale(unit, unit) })
        drawPath(scaled, bolt, style = Stroke(1.7f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
