package com.darmix.darmixfit.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseCatalog
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.common.DarmixToastHost
import com.darmix.darmixfit.ui.common.showToast
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixConfirmDialog
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.DarmixIconButton
import com.darmix.darmixfit.ui.components.DarmixSheet
import com.darmix.darmixfit.ui.components.DarmixTextField
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.InfoChip
import com.darmix.darmixfit.ui.components.NumberStepper
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.SelectChip
import com.darmix.darmixfit.ui.components.SkeletonBlock
import com.darmix.darmixfit.ui.components.StatusTag
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private val DayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val RailWidth = 36.dp
private val RepPresets = listOf("6-8 reps", "8-12 reps", "10-12 reps", "12-15 reps", "15-20 reps", "Al fallo", "30 segundos")
private val TitlePresets = listOf("Empuje", "Tirón", "Pierna", "Torso", "Cuerpo completo", "Brazos", "Core", "Descanso")

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
                DayBoard(
                    selected = state.selectedDay,
                    counts = state.dayCounts,
                    onSelect = viewModel::selectDay
                )
            }
            item(key = "header", contentType = "header") {
                DayHeader(
                    state = state,
                    onEditTitle = { editingTitle = true }
                )
            }

            when {
                state.loading -> items(count = 3, key = { "skeleton-$it" }, contentType = { "skeleton" }) {
                    SkeletonBlock(height = 110.dp, modifier = Modifier.padding(bottom = Spacing.md))
                }

                state.entries.isEmpty() -> item(key = "empty-${state.selectedDay}", contentType = "empty") {
                    EmptyDay(onAdd = { form = FormTarget.Add })
                }

                else -> itemsIndexed(
                    items = state.entries,
                    key = { _, entry -> entry.first().id },
                    contentType = { _, entry -> if (entry.size > 1) "superset" else "single" }
                ) { index, entry ->
                    val anchor = entry.first()
                    val hasNext = index < state.entries.lastIndex
                    Column(modifier = Modifier.animateItem()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OrderRail(
                                number = index + 1,
                                canMoveUp = index > 0,
                                canMoveDown = hasNext,
                                onMoveUp = { viewModel.move(anchor, up = true) },
                                onMoveDown = { viewModel.move(anchor, up = false) }
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Box(modifier = Modifier.weight(1f)) {
                                if (entry.size > 1) {
                                    SupersetCard(
                                        members = entry,
                                        onUnlink = { viewModel.unlink(anchor) },
                                        actions = actions
                                    )
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

        AddFab(
            expanded = fabExpanded,
            onClick = { form = FormTarget.Add },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.screen)
        )
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

@Composable
private fun DayBoard(selected: Int, counts: List<Int>, onSelect: (Int) -> Unit) {
    val today = remember { LocalDate.now().dayOfWeek.value }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs, bottom = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (dow in 1..7) {
            DayTile(
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
private fun DayTile(
    letter: String,
    count: Int,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val background by animateColorAsState(
        targetValue = if (selected) primary else colors.card,
        animationSpec = Motion.snappy(),
        label = "tile-bg"
    )
    val content = if (selected) onPrimary else colors.textPrimary
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, if (selected) primary else colors.hairline, shape)
            .clickable(role = Role.Tab) {
                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) onPrimary.copy(alpha = 0.8f) else colors.textTertiary
        )
        if (count > 0) {
            Text(text = "$count", style = DarmixTextStyles.MetricSmall.copy(fontSize = DarmixTextStyles.MetricSmall.fontSize * 0.85f), color = content)
        } else {
            Crescent(color = if (selected) onPrimary else colors.tide, size = 16.dp)
        }
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isToday) (if (selected) onPrimary else colors.voltText) else Color.Transparent)
        )
    }
}

@Composable
private fun DayHeader(state: EditUiState, onEditTitle: () -> Unit) {
    val colors = DarmixTheme.colors
    val dayName = remember(state.selectedDay) {
        DayOfWeek.of(state.selectedDay).getDisplayName(TextStyle.FULL, MexicoLocale).uppercase(MexicoLocale)
    }
    Column(
        modifier = Modifier.padding(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(
                Modifier
                    .size(width = 16.dp, height = 3.dp)
                    .background(colors.voltText, CircleShape)
            )
            Overline(text = "Editor de rutina", color = colors.textSecondary)
        }
        Text(text = dayName, style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
        Row(
            modifier = Modifier
                .clip(DarmixShapes.Control)
                .clickable(role = Role.Button, onClick = onEditTitle)
                .padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = state.dayTitle.ifBlank { "Agregar título del día" },
                style = MaterialTheme.typography.titleMedium,
                color = if (state.dayTitle.isBlank()) colors.voltText else colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(Icons.Rounded.Edit, contentDescription = "Editar título", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
        }
        if (!state.loading && state.exerciseCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
                InfoChip(text = if (state.exerciseCount == 1) "1 ejercicio" else "${state.exerciseCount} ejercicios", content = colors.textPrimary)
                InfoChip(text = "${state.totalSets} series", content = colors.voltText)
                if (state.supersetCount > 0) {
                    InfoChip(
                        text = if (state.supersetCount == 1) "1 superserie" else "${state.supersetCount} superseries",
                        content = colors.tideText
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderRail(
    number: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = DarmixTheme.colors
    Column(
        modifier = Modifier.width(RailWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RailArrow(icon = Icons.Rounded.KeyboardArrowUp, description = "Subir", enabled = canMoveUp, onClick = onMoveUp)
        Text(
            text = number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textTertiary
        )
        RailArrow(icon = Icons.Rounded.KeyboardArrowDown, description = "Bajar", enabled = canMoveDown, onClick = onMoveDown)
    }
}

@Composable
private fun RailArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    DarmixIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        enabled = enabled,
        style = DarmixButtonStyle.Secondary,
        containerColor = Color.Transparent,
        contentColor = DarmixTheme.colors.textSecondary,
        size = 32.dp,
        shape = CircleShape,
        haptic = HapticFeedbackType.SegmentTick
    )
}

@Composable
private fun SingleCard(exercise: ExerciseEntity, actions: ExerciseActions) {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Card)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Card)
            .clickable(role = Role.Button) { actions.onEdit(exercise) }
            .padding(Spacing.md)
    ) {
        ExerciseRow(exercise = exercise, showSets = true, imageSize = 64.dp, accent = colors.voltText, actions = actions)
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
            .clip(DarmixShapes.Card)
            .background(colors.card)
            .drawBehind { drawRect(tide.copy(alpha = 0.06f)) }
            .border(1.5.dp, tide.copy(alpha = 0.45f), DarmixShapes.Card)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatusTag(text = "Superserie", container = tide.copy(alpha = 0.16f), content = tide, icon = Icons.Rounded.Repeat)
            Text(text = "$rounds rondas", style = MaterialTheme.typography.labelMedium, color = tide)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(DarmixShapes.Pill)
                    .clickable(role = Role.Button, onClick = onUnlink)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                Text(text = "Separar", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
        members.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DarmixShapes.Control)
                    .clickable(role = Role.Button) { actions.onEdit(exercise) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(DarmixShapes.Tag)
                        .background(tide),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ('A' + index).toString(), style = MaterialTheme.typography.labelLarge, color = onTide)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ExerciseRow(exercise = exercise, showSets = false, imageSize = 52.dp, accent = tide, actions = actions)
                }
            }
        }
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
        Box {
            ExerciseImage(
                imageName = exercise.imageUri,
                exerciseName = exercise.name,
                size = imageSize,
                animated = false
            )
            Box(
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(if (imageSize >= 72.dp) 18.dp else 14.dp))
                    .clickable(role = Role.Button) { actions.onPickImage(exercise) }
            )
            if (exercise.imageUri == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.voltText),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Agregar imagen",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (showSets) "${exercise.sets} × ${exercise.reps}" else exercise.reps,
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (exercise.note.isNotBlank()) {
                Text(
                    text = exercise.note,
                    style = MaterialTheme.typography.bodySmall,
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
        DarmixIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "Opciones",
            onClick = { open = true },
            style = DarmixButtonStyle.Secondary,
            containerColor = Color.Transparent,
            contentColor = colors.textSecondary,
            size = 36.dp,
            shape = CircleShape
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = DarmixShapes.Card,
            containerColor = colors.cardRaised,
            border = BorderStroke(1.dp, colors.hairline),
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
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(RailWidth + Spacing.sm))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = colors.hairlineStrong,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                )
            }
            Row(
                modifier = Modifier
                    .clip(DarmixShapes.Pill)
                    .background(colors.canvas)
                    .border(1.dp, colors.hairline, DarmixShapes.Pill)
                    .clickable(role = Role.Button) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onLink()
                    }
                    .padding(horizontal = Spacing.md, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = colors.tideText, modifier = Modifier.size(14.dp))
                Text(text = "Unir en superserie", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun EmptyDay(onAdd: () -> Unit) {
    val colors = DarmixTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = colors.hairlineStrong,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = size.copy(width = size.width - stroke, height = size.height - stroke),
                    cornerRadius = CornerRadius(30.dp.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
                )
            }
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.tideSoft),
            contentAlignment = Alignment.Center
        ) {
            Crescent(color = colors.tide, size = 28.dp)
        }
        Text(text = "DÍA DE DESCANSO", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(
            text = "Así se queda si no agregas nada. Si quieres entrenar este día, arma tu rutina.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        DarmixButton(
            text = "AGREGAR PRIMER EJERCICIO",
            onClick = onAdd,
            icon = Icons.Rounded.Add,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}

@Composable
private fun AddFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(DarmixShapes.Pill)
            .background(primary)
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick()
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Agregar ejercicio", tint = onPrimary, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Text(
                text = "AGREGAR",
                style = MaterialTheme.typography.labelLarge,
                color = onPrimary,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
    }
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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ExerciseImage(imageName = previewImage, exerciseName = name.ifBlank { "?" }, size = 64.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Overline(text = if (initial == null) "Nuevo ejercicio" else "Editar ejercicio", color = colors.voltText)
                    Text(
                        text = name.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (name.isBlank()) colors.textTertiary else colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (suggested) {
                        Text(text = "Imagen incluida encontrada", style = MaterialTheme.typography.labelMedium, color = colors.voltText)
                    }
                }
            }

            DarmixTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre del ejercicio",
                placeholder = "Ej. Sentadilla Copa (Goblet Squat)"
            )

            NumberStepper(
                value = sets,
                onValueChange = { sets = it },
                range = 1..20,
                label = "Series",
                hint = if (initial?.supersetGroup != null) "Se aplica a toda la superserie" else null
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Overline(text = "Repeticiones", color = colors.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)
                ) {
                    RepPresets.forEach { preset ->
                        SelectChip(text = preset, selected = reps == preset, onClick = { reps = preset })
                    }
                }
                DarmixTextField(value = reps, onValueChange = { reps = it }, label = "Repeticiones o tiempo")
            }

            DarmixTextField(
                value = note,
                onValueChange = { note = it },
                label = "Nota técnica",
                placeholder = "Ej. Espalda neutra y baja con control",
                singleLine = false,
                minLines = 2
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                DarmixButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    style = DarmixButtonStyle.Ghost,
                    haptic = null,
                    modifier = Modifier.width(120.dp)
                )
                DarmixButton(
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
    val colors = DarmixTheme.colors
    var text by rememberSaveable { mutableStateOf(initial) }
    DarmixSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Overline(text = "Título del día", color = colors.voltText)
                Text(text = "¿Qué toca este día?", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)
            ) {
                TitlePresets.forEach { preset ->
                    SelectChip(text = preset, selected = text == preset, onClick = { text = preset })
                }
            }
            DarmixTextField(
                value = text,
                onValueChange = { text = it },
                label = "Título",
                placeholder = "Ej. Empuje (Pecho, Hombro y Tríceps)"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                DarmixButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    style = DarmixButtonStyle.Ghost,
                    haptic = null,
                    modifier = Modifier.width(120.dp)
                )
                DarmixButton(
                    text = "GUARDAR",
                    onClick = { onSave(text.trim()) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
