package com.darmix.darmixfit.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseCatalog
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.common.DarmixToastHost
import com.darmix.darmixfit.ui.common.showToast
import com.darmix.darmixfit.ui.components.DarmixConfirmDialog
import com.darmix.darmixfit.ui.components.DarmixSheet
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.SkeletonBlock
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val DayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val RailWidth = 34.dp
private val RepPresets = listOf("6-8 reps", "8-12 reps", "10-12 reps", "12-15 reps", "15-20 reps", "Al fallo", "30 segundos")
private val TitlePresets = listOf("Empuje", "Tirón", "Pierna", "Torso", "Cuerpo completo", "Brazos", "Core", "Descanso")

private val KeyShape = CutCornerShape(8.dp)
private val CardShape = CutCornerShape(topEnd = 14.dp)
private val SupersetShape = CutCornerShape(14.dp)
private val TagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val PlateShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
private val SmallPlate = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
private val FieldShape = CutCornerShape(10.dp)
private val ThumbShape = CutCornerShape(8.dp)
private val MenuShape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
private val EmptyShape = CutCornerShape(16.dp)

private val Display = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black)

private sealed interface FormTarget {
    data object Add : FormTarget
    data class Edit(val exercise: ExerciseEntity) : FormTarget
}

private data class ExerciseActions(
    val onEdit: (ExerciseEntity) -> Unit,
    val onDelete: (ExerciseEntity) -> Unit,
    val onPickImage: (ExerciseEntity) -> Unit,
    val onRemoveImage: (ExerciseEntity) -> Unit,
    val onRestoreImage: (ExerciseEntity) -> Unit
)

@Composable
fun EditRoutineScreen(
    modifier: Modifier = Modifier,
    viewModel: EditRoutineViewModel = viewModel(factory = EditRoutineViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = DarmixTheme.colors

    var form by remember { mutableStateOf<FormTarget?>(null) }
    var toDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var editingTitle by remember { mutableStateOf(false) }

    var pendingImageFor by rememberSaveable { mutableStateOf<Long?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val id = pendingImageFor
        if (uri != null && id != null) viewModel.setImage(id, uri)
        pendingImageFor = null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showToast(it) }
    }

    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex < 2 } }

    val actions = remember(viewModel) {
        ExerciseActions(
            onEdit = { form = FormTarget.Edit(it) },
            onDelete = { toDelete = it },
            onPickImage = {
                pendingImageFor = it.id
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveImage = { viewModel.removeImage(it.id) },
            onRestoreImage = viewModel::restoreImage
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.md, bottom = 120.dp)
        ) {
            item(key = "days", contentType = "days") {
                DayKeys(
                    selected = state.selectedDay,
                    counts = state.dayCounts,
                    onSelect = viewModel::selectDay
                )
            }
            item(key = "header", contentType = "header") {
                DayHeader(state = state, onEditTitle = { editingTitle = true })
            }

            when {
                state.loading -> items(count = 3, key = { "skeleton-$it" }, contentType = { "skeleton" }) {
                    SkeletonBlock(height = 110.dp, modifier = Modifier.padding(bottom = Spacing.md))
                }

                state.entries.isEmpty() -> item(key = "empty-${state.selectedDay}", contentType = "empty") {
                    RechargeDay(onAdd = { form = FormTarget.Add })
                }

                else -> itemsIndexed(
                    items = state.entries,
                    key = { _, entry -> entry.first().id },
                    contentType = { _, entry -> if (entry.size > 1) "superset" else "single" }
                ) { index, entry ->
                    val anchor = entry.first()
                    val hasNext = index < state.entries.lastIndex
                    val superset = entry.size > 1
                    Column(modifier = Modifier.animateItem()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OrderRail(
                                number = index + 1,
                                color = if (superset) colors.tideText else colors.textTertiary,
                                canMoveUp = index > 0,
                                canMoveDown = hasNext,
                                onMoveUp = { viewModel.move(anchor, up = true) },
                                onMoveDown = { viewModel.move(anchor, up = false) }
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Box(modifier = Modifier.weight(1f)) {
                                if (superset) {
                                    SupersetCard(members = entry, onUnlink = { viewModel.unlink(anchor) }, actions = actions)
                                } else {
                                    SingleCard(exercise = anchor, actions = actions)
                                }
                            }
                        }
                        if (hasNext) {
                            LinkConnector(onLink = { viewModel.linkWithNext(anchor) })
                        }
                    }
                }
            }
        }

        DarmixToastHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.sm)
        )

        if (!state.loading && state.entries.isNotEmpty()) {
            AddFab(
                expanded = fabExpanded,
                onClick = { form = FormTarget.Add },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.screen)
            )
        }
    }

    form?.let { target ->
        ExerciseSheet(
            initial = (target as? FormTarget.Edit)?.exercise,
            onSave = { name, sets, reps, note ->
                when (target) {
                    is FormTarget.Edit -> viewModel.saveExercise(target.exercise, name, sets, reps, note)
                    is FormTarget.Add -> viewModel.addExercise(name, sets, reps, note)
                }
                form = null
            },
            onDismiss = { form = null }
        )
    }

    toDelete?.let { exercise ->
        DarmixConfirmDialog(
            title = "¿Eliminar ejercicio?",
            message = "Se borrará «${exercise.name}» junto con su progreso registrado y su imagen. " +
                    "Si está en una superserie de dos, el otro ejercicio quedará suelto.",
            confirmLabel = "Eliminar",
            icon = Icons.Rounded.DeleteOutline,
            onConfirm = {
                viewModel.deleteExercise(exercise)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }

    if (editingTitle) {
        TitleSheet(
            initial = state.dayTitle,
            onSave = {
                viewModel.updateDayTitle(it)
                editingTitle = false
            },
            onDismiss = { editingTitle = false }
        )
    }
}

private fun Modifier.glowOutline(shape: Shape, color: Color, strength: Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        drawOutline(outline, color, alpha = 0.08f * strength, style = Stroke(14.dp.toPx(), join = StrokeJoin.Miter))
        drawOutline(outline, color, alpha = 0.18f * strength, style = Stroke(6.dp.toPx(), join = StrokeJoin.Miter))
    }
}

@Composable
private fun DayKeys(selected: Int, counts: List<Int>, onSelect: (Int) -> Unit) {
    val today = remember { LocalDate.now().dayOfWeek.value }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs, bottom = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (dow in 1..7) {
            DayKey(
                letter = DayLetters[dow - 1],
                count = counts.getOrElse(dow - 1) { 0 },
                selected = dow == selected,
                isToday = dow == today,
                onClick = { onSelect(dow) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayKey(
    letter: String,
    count: Int,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val rest = count == 0
    val ink = colors.onVolt
    val border = when {
        selected -> colors.volt
        rest -> colors.tideText.copy(alpha = 0.4f)
        else -> colors.hairlineStrong
    }
    val base = when {
        rest -> lerp(colors.card, colors.tide, if (colors.isDark) 0.05f else 0.08f)
        else -> colors.card
    }
    Box(
        modifier = modifier
            .height(62.dp)
            .then(if (selected) Modifier.glowOutline(KeyShape, colors.volt, if (colors.isDark) 1f else 0.6f) else Modifier)
            .clip(KeyShape)
            .then(
                if (selected) Modifier.background(Brush.verticalGradient(listOf(colors.voltBright, colors.volt)))
                else Modifier.background(base)
            )
            .border(1.dp, border, KeyShape)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                color = if (selected) ink else colors.textTertiary
            )
            if (rest) {
                ChargingBattery(
                    size = 20.dp,
                    color = if (selected) ink else colors.tideText,
                    bolt = if (selected) colors.volt else colors.onTide
                )
            } else {
                Text(
                    text = "$count",
                    style = Display.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = if (selected) ink else colors.textPrimary
                )
            }
        }
        if (isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(width = 10.dp, height = 3.dp)
                    .background(if (selected) ink else colors.voltText)
            )
        }
    }
}

@Composable
private fun DayHeader(state: EditUiState, onEditTitle: () -> Unit) {
    val colors = DarmixTheme.colors
    val dayName = remember(state.selectedDay) {
        DayOfWeek.of(state.selectedDay).getDisplayName(DateTextStyle.FULL, MexicoLocale).uppercase(MexicoLocale)
    }
    val dash = colors.hairlineStrong
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(text = dayName, style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Editar título", onClick = onEditTitle)
                .drawBehind {
                    drawLine(
                        color = dash,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    )
                }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = state.dayTitle.ifBlank { "Agregar título del día" },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                color = if (state.dayTitle.isBlank()) colors.voltText else colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
        }
        if (!state.loading && state.exerciseCount > 0) {
            StatsBar(state = state)
        }
    }
}

@Composable
private fun StatsBar(state: EditUiState) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(KeyShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairline, KeyShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatSegment(
            value = state.exerciseCount,
            label = if (state.exerciseCount == 1) "EJERCICIO" else "EJERCICIOS",
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        SegmentDivider()
        StatSegment(value = state.totalSets, label = "SERIES", color = colors.voltText, modifier = Modifier.weight(1f))
        SegmentDivider()
        StatSegment(
            value = state.supersetCount,
            label = if (state.supersetCount == 1) "SUPERSERIE" else "SUPERSERIES",
            color = colors.tideText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(DarmixTheme.colors.hairline)
    )
}

@Composable
private fun StatSegment(value: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "$value", style = Display.copy(fontSize = 20.sp, lineHeight = 20.sp), color = color)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.2.sp),
            color = DarmixTheme.colors.textTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun OrderRail(
    number: Int,
    color: Color,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(
        modifier = Modifier.width(RailWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RailArrow(icon = Icons.Rounded.KeyboardArrowUp, description = "Subir", enabled = canMoveUp, onClick = onMoveUp)
        Text(
            text = number.toString().padStart(2, '0'),
            style = Display.copy(fontSize = 26.sp, lineHeight = 26.sp),
            color = color
        )
        RailArrow(icon = Icons.Rounded.KeyboardArrowDown, description = "Bajar", enabled = canMoveDown, onClick = onMoveDown)
    }
}

@Composable
private fun RailArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = DarmixTheme.colors
    val shape = CutCornerShape(5.dp)
    Box(
        modifier = Modifier
            .size(width = 26.dp, height = 22.dp)
            .clip(shape)
            .background(if (enabled) colors.cardRaised else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) colors.textSecondary else colors.hairlineStrong,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SingleCard(exercise: ExerciseEntity, actions: ExerciseActions) {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.hairline, CardShape)
            .clickable(role = Role.Button) { actions.onEdit(exercise) }
            .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
    ) {
        ExerciseRow(exercise = exercise, showSets = true, imageSize = 60.dp, accent = colors.voltText, actions = actions)
    }
}

@Composable
private fun SupersetCard(members: List<ExerciseEntity>, onUnlink: () -> Unit, actions: ExerciseActions) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val onTide = if (colors.isDark) colors.onTide else Color.White
    val rounds = remember(members) { members.minOf { it.sets } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SupersetShape)
            .background(lerp(colors.card, colors.tide, if (colors.isDark) 0.06f else 0.05f))
            .border(1.dp, tide.copy(alpha = 0.45f), SupersetShape)
            .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            HudTag(text = "SUPERSERIE", color = tide, icon = Icons.Rounded.Repeat)
            Text(
                text = "$rounds RONDAS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                color = tide
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(TagShape)
                    .clickable(role = Role.Button, onClick = onUnlink)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                Text(
                    text = "SEPARAR",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.2.sp),
                    color = colors.textSecondary
                )
            }
        }
        members.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { actions.onEdit(exercise) },
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
                Box(modifier = Modifier.weight(1f)) {
                    ExerciseRow(exercise = exercise, showSets = false, imageSize = 48.dp, accent = tide, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun HudTag(text: String, color: Color, icon: ImageVector? = null) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(TagShape)
            .background(colors.card)
            .border(1.dp, color, TagShape)
            .padding(start = 9.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.5.sp),
            color = color
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    showSets: Boolean,
    imageSize: Dp,
    accent: Color,
    actions: ExerciseActions
) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .clip(ThumbShape)
                .clickable(role = Role.Button) { actions.onPickImage(exercise) }
        ) {
            ExerciseImage(imageName = exercise.imageUri, exerciseName = exercise.name, size = imageSize, animated = false)
            if (exercise.imageUri == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CutCornerShape(topStart = 6.dp))
                        .background(colors.volt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Agregar imagen", tint = colors.onVolt, modifier = Modifier.size(14.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, lineHeight = 18.sp),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (showSets) "${exercise.sets} × ${exercise.reps}" else exercise.reps,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (exercise.note.isNotBlank()) {
                Text(
                    text = exercise.note,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ExerciseMenu(exercise = exercise, actions = actions)
    }
}

@Composable
private fun ExerciseMenu(exercise: ExerciseEntity, actions: ExerciseActions) {
    val colors = DarmixTheme.colors
    var open by remember { mutableStateOf(false) }
    val bundled = remember(exercise.name) { ExerciseCatalog.imageRefFor(exercise.name) }
    Box {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 40.dp)
                .clip(CutCornerShape(6.dp))
                .clickable(role = Role.Button, onClickLabel = "Opciones") { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MenuShape,
            containerColor = colors.cardRaised,
            border = BorderStroke(1.dp, colors.hairlineStrong),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            MenuItem(text = "Editar", icon = Icons.Rounded.Edit) {
                open = false
                actions.onEdit(exercise)
            }
            MenuItem(
                text = if (exercise.imageUri == null) "Agregar imagen" else "Cambiar imagen",
                icon = Icons.Rounded.AddPhotoAlternate
            ) {
                open = false
                actions.onPickImage(exercise)
            }
            if (bundled != null && bundled != exercise.imageUri) {
                MenuItem(text = "Usar imagen incluida", icon = Icons.Rounded.AutoAwesome, tint = colors.voltText) {
                    open = false
                    actions.onRestoreImage(exercise)
                }
            }
            if (exercise.imageUri != null) {
                MenuItem(text = "Quitar imagen", icon = Icons.Rounded.HideImage) {
                    open = false
                    actions.onRemoveImage(exercise)
                }
            }
            HorizontalDivider(color = colors.hairline, modifier = Modifier.padding(vertical = Spacing.xs))
            MenuItem(text = "Eliminar", icon = Icons.Rounded.DeleteOutline, tint = colors.emberText) {
                open = false
                actions.onDelete(exercise)
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, tint: Color = DarmixTheme.colors.textPrimary, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = text, style = MaterialTheme.typography.titleSmall, color = tint) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint) },
        onClick = onClick
    )
}

@Composable
private fun LinkConnector(onLink: () -> Unit) {
    val colors = DarmixTheme.colors
    val dash = colors.hairlineStrong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(RailWidth + Spacing.sm))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .drawBehind {
                    drawLine(
                        color = dash,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .height(26.dp)
                    .clip(TagShape)
                    .background(colors.canvas)
                    .border(1.dp, colors.hairlineStrong, TagShape)
                    .clickable(role = Role.Button, onClick = onLink)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = colors.tideText, modifier = Modifier.size(14.dp))
                Text(
                    text = "UNIR EN SUPERSERIE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.4.sp),
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RechargeDay(onAdd: () -> Unit) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val spin = rememberInfiniteTransition(label = "recharge")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "recharge-angle"
    )
    val track = lerp(colors.card, colors.tide, if (colors.isDark) 0.12f else 0.18f)
    val tickDim = colors.hairlineStrong
    val bracket = tide

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmptyShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(colors.card, colors.tide, if (colors.isDark) 0.05f else 0.06f),
                        colors.card
                    )
                )
            )
            .border(1.dp, tide.copy(alpha = 0.35f), EmptyShape)
            .drawBehind {
                val inset = 8.dp.toPx()
                val arm = 10.dp.toPx()
                val w = 1.6.dp.toPx()
                val corners = listOf(
                    Offset(inset, inset) to Offset(1f, 1f),
                    Offset(size.width - inset, inset) to Offset(-1f, 1f),
                    Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f),
                    Offset(inset, size.height - inset) to Offset(1f, -1f)
                )
                corners.forEach { (p, d) ->
                    drawLine(bracket, p, Offset(p.x + arm * d.x, p.y), strokeWidth = w)
                    drawLine(bracket, p, Offset(p.x, p.y + arm * d.y), strokeWidth = w)
                }
            }
            .padding(horizontal = 18.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                val unit = size.minDimension / 150f
                for (k in 0 until 60) {
                    val a = Math.toRadians(k * 6.0)
                    val major = k % 5 == 0
                    val r0 = 66f * unit
                    val r1 = 71f * unit
                    drawLine(
                        color = if (major) tide.copy(alpha = 0.7f) else tickDim,
                        start = Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                        end = Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                        strokeWidth = 1.5f * unit
                    )
                }
                val r = 58f * unit
                val stroke = 10f * unit
                drawCircle(track, radius = r, center = c, style = Stroke(stroke))
                rotate(angle, pivot = c) {
                    drawArc(
                        color = tide.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(stroke + 8f * unit)
                    )
                    drawArc(
                        color = tide,
                        startAngle = -90f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(stroke)
                    )
                }
                drawCircle(
                    Brush.radialGradient(
                        listOf(lerp(colors.card, colors.tide, 0.12f), lerp(colors.canvasDeep, colors.tide, 0.04f)),
                        center = Offset(c.x, c.y - 10f * unit),
                        radius = 50f * unit
                    ),
                    radius = 47f * unit,
                    center = c
                )
                drawCircle(tide.copy(alpha = 0.25f), radius = 47f * unit, center = c, style = Stroke(1f * unit))
            }
            ChargingBattery(size = 54.dp, color = tide, bolt = colors.onTide)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "SIN EJERCICIOS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.4.sp),
            color = tide
        )
        Text(
            text = "DÍA DE RECARGA",
            style = Display.copy(fontSize = 30.sp, lineHeight = 34.sp),
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Tu cuerpo también crece descansando. Si prefieres entrenar este día, arma tu rutina.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 10.dp, end = 10.dp)
        )
        PrimaryPlate(
            text = "ARMAR RUTINA",
            icon = Icons.Rounded.Add,
            onClick = onAdd,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
private fun ChargingBattery(size: Dp, color: Color, bolt: Color) {
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
        drawRoundRect(
            color = color,
            topLeft = Offset(2.5f * unit, 7f * unit),
            size = Size(17f * unit, 10f * unit),
            cornerRadius = CornerRadius(2f * unit),
            style = Stroke(line)
        )
        drawLine(color, Offset(22f * unit, 10.5f * unit), Offset(22f * unit, 13.5f * unit), strokeWidth = line, cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(4.4f * unit, 8.9f * unit),
            size = Size(13.2f * unit, 6.2f * unit),
            cornerRadius = CornerRadius(0.8f * unit)
        )
        val scaled = Path().apply { addPath(boltPath) }
        scaled.transform(Matrix().apply { scale(unit, unit) })
        drawPath(scaled, bolt, style = Stroke(1.7f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun PrimaryPlate(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = SmallPlate,
    height: Dp = 48.dp
) {
    val colors = DarmixTheme.colors
    val fill = if (enabled) {
        Brush.verticalGradient(listOf(colors.voltBright, colors.volt, lerp(colors.volt, colors.voltDeep, 0.3f)))
    } else {
        Brush.verticalGradient(listOf(colors.cardHigh, colors.cardHigh))
    }
    val content = if (enabled) colors.onVolt else colors.textTertiary
    Row(
        modifier = modifier
            .height(height)
            .then(if (enabled) Modifier.glowOutline(shape, colors.volt, if (colors.isDark) 0.8f else 0.4f) else Modifier)
            .clip(shape)
            .background(fill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Black),
            color = content
        )
    }
}

@Composable
private fun GhostPlate(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(SmallPlate)
            .border(1.dp, colors.hairlineStrong, SmallPlate)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 1.4.sp),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun AddFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier
            .height(52.dp)
            .glowOutline(PlateShape, colors.volt, if (colors.isDark) 1f else 0.5f)
            .clip(PlateShape)
            .background(Brush.verticalGradient(listOf(colors.voltBright, colors.volt, lerp(colors.volt, colors.voltDeep, 0.3f))))
            .clickable(role = Role.Button, onClickLabel = "Agregar ejercicio", onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Agregar ejercicio", tint = colors.onVolt, modifier = Modifier.size(22.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Text(
                text = "AGREGAR",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Black),
                color = colors.onVolt,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
        color = DarmixTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
}

@Composable
private fun HudField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: Dp = 48.dp
) {
    val colors = DarmixTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        targetValue = if (focused) colors.voltText else colors.hairline,
        animationSpec = tween(Motion.Quick),
        label = "field-border"
    )
    Column {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            interactionSource = interaction,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(colors.voltText),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .clip(FieldShape)
                        .background(colors.canvasDeep)
                        .border(1.dp, border, FieldShape)
                        .drawBehind {
                            if (focused) drawRect(colors.voltText, size = Size(3.dp.toPx(), size.height))
                        }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textTertiary
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun SetStepper(value: Int, onValueChange: (Int) -> Unit, range: IntRange, hint: String?) {
    val colors = DarmixTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(colors.canvasDeep)
                .border(1.dp, colors.hairline, FieldShape)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SERIES",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            StepButton(icon = Icons.Rounded.Remove, description = "Menos", enabled = value > range.first) {
                onValueChange((value - 1).coerceIn(range))
            }
            Text(
                text = "$value",
                style = Display.copy(fontSize = 32.sp, lineHeight = 32.sp),
                color = colors.voltText,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )
            StepButton(icon = Icons.Rounded.Add, description = "Más", enabled = value < range.last) {
                onValueChange((value + 1).coerceIn(range))
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.tideText,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = DarmixTheme.colors
    val shape = CutCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(if (enabled) colors.cardRaised else Color.Transparent)
            .border(1.dp, if (enabled) colors.hairlineStrong else colors.hairline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) colors.textPrimary else colors.textTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PresetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(TagShape)
            .then(
                if (selected) Modifier.background(colors.volt)
                else Modifier
                    .background(colors.canvasDeep)
                    .border(1.dp, colors.hairlineStrong, TagShape)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = if (selected) colors.onVolt else colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun SheetHeader(overline: String, title: String, titleMuted: Boolean, image: @Composable () -> Unit, extra: String? = null) {
    val colors = DarmixTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        image()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 2.dp)
                        .background(colors.voltText)
                )
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.2.sp),
                    color = colors.voltText
                )
            }
            Text(
                text = title,
                style = Display.copy(fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold),
                color = if (titleMuted) colors.textTertiary else colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (extra != null) {
                Text(text = extra, style = MaterialTheme.typography.labelMedium, color = colors.voltText)
            }
        }
    }
}

@Composable
private fun SheetDivider() {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(colors.voltText.copy(alpha = 0.6f), colors.hairline, Color.Transparent)))
    )
}

@Composable
private fun ExerciseSheet(
    initial: ExerciseEntity?,
    onSave: (name: String, sets: Int, reps: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = DarmixTheme.colors
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var sets by rememberSaveable { mutableIntStateOf(initial?.sets ?: 3) }
    var reps by rememberSaveable { mutableStateOf(initial?.reps.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    val previewImage = remember(name, initial) { initial?.imageUri ?: ExerciseCatalog.imageRefFor(name) }
    val suggested = initial == null && previewImage != null
    val valid = name.isNotBlank()

    DarmixSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SheetHeader(
                overline = if (initial == null) "NUEVO EJERCICIO" else "EDITAR EJERCICIO",
                title = name.ifBlank { "Sin nombre" },
                titleMuted = name.isBlank(),
                extra = if (suggested) "Imagen incluida encontrada" else null,
                image = {
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(10.dp))
                            .border(1.dp, if (previewImage != null) colors.voltText else colors.hairlineStrong, CutCornerShape(10.dp))
                    ) {
                        ExerciseImage(imageName = previewImage, exerciseName = name.ifBlank { "?" }, size = 64.dp)
                    }
                }
            )
            SheetDivider()

            HudField(
                value = name,
                onValueChange = { name = it },
                label = "NOMBRE",
                placeholder = "Ej. Sentadilla copa (Goblet squat)"
            )

            SetStepper(
                value = sets,
                onValueChange = { sets = it },
                range = 1..20,
                hint = if (initial?.supersetGroup != null) "Se aplica a toda la superserie" else null
            )

            Column {
                FieldLabel("REPETICIONES")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RepPresets.forEach { preset ->
                        PresetChip(text = preset, selected = reps == preset, onClick = { reps = preset })
                    }
                }
                HudField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = "O ESCRÍBELAS",
                    placeholder = "Repeticiones o tiempo"
                )
            }

            HudField(
                value = note,
                onValueChange = { note = it },
                label = "NOTA TÉCNICA",
                placeholder = "Ej. Espalda neutra y baja con control",
                singleLine = false,
                minHeight = 76.dp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostPlate(text = "CANCELAR", onClick = onDismiss, modifier = Modifier.width(120.dp))
                PrimaryPlate(
                    text = "GUARDAR",
                    onClick = { onSave(name.trim(), sets, reps.trim(), note.trim()) },
                    enabled = valid,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TitleSheet(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    DarmixSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SheetHeader(
                overline = "TÍTULO DEL DÍA",
                title = "¿Qué toca este día?",
                titleMuted = false,
                image = {}
            )
            SheetDivider()
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TitlePresets.forEach { preset ->
                    PresetChip(text = preset, selected = text == preset, onClick = { text = preset })
                }
            }
            HudField(
                value = text,
                onValueChange = { text = it },
                label = "TÍTULO",
                placeholder = "Ej. Empuje (Pecho, Hombro y Tríceps)"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostPlate(text = "CANCELAR", onClick = onDismiss, modifier = Modifier.width(120.dp))
                PrimaryPlate(
                    text = "GUARDAR",
                    onClick = { onSave(text.trim()) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
