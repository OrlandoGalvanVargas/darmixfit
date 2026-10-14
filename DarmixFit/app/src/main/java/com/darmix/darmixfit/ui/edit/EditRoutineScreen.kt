package com.darmix.darmixfit.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.theme.DangerRed
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE_ES_MX = Locale.forLanguageTag("es-MX")

private sealed interface FormTarget {
    data object Add : FormTarget
    data class Edit(val exercise: ExerciseEntity) : FormTarget
}

@Composable
fun EditRoutineScreen(
    modifier: Modifier = Modifier,
    viewModel: EditRoutineViewModel = viewModel(factory = EditRoutineViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var form by remember { mutableStateOf<FormTarget?>(null) }
    var toDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var editingTitle by remember { mutableStateOf(false) }

    var pendingImageFor by rememberSaveable { mutableStateOf<Long?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val id = pendingImageFor
        if (uri != null && id != null) viewModel.setImage(id, uri)
        pendingImageFor = null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val exerciseCount = state.entries.sumOf { if (it.size == 1) 1 else it.size }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "days") {
                DaySelector(selected = state.selectedDay, onSelect = viewModel::selectDay)
            }

            item(key = "header") {
                DayHeader(
                    dayOfWeek = state.selectedDay,
                    title = state.dayTitle,
                    exerciseCount = exerciseCount,
                    onEditTitle = { editingTitle = true },
                )
            }

            when {
                state.loading -> item(key = "loading") {
                    Text(
                        text = "Cargando rutina…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                state.entries.isEmpty() -> item(key = "empty") {
                    EmptyDayCard()
                }

                else -> itemsIndexed(
                    items = state.entries,
                    key = { _, entry -> entry.first().id },
                ) { index, entry ->
                    val hasNext = index < state.entries.lastIndex
                    val anchor = entry.first()
                    EntryCard(
                        entry = entry,
                        canMoveUp = index > 0,
                        canMoveDown = hasNext,
                        onMoveUp = { viewModel.move(anchor, up = true) },
                        onMoveDown = { viewModel.move(anchor, up = false) },
                        onLinkWithNext = if (hasNext) ({ viewModel.linkWithNext(anchor) }) else null,
                        onUnlink = { viewModel.unlink(anchor) },
                        onEdit = { form = FormTarget.Edit(it) },
                        onDelete = { toDelete = it },
                        onPickImage = {
                            pendingImageFor = it.id
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveImage = { viewModel.removeImage(it.id) },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp, start = 16.dp, end = 16.dp),
        )

        ExtendedFloatingActionButton(
            onClick = { form = FormTarget.Add },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Agregar ejercicio", fontWeight = FontWeight.Bold)
        }
    }

    form?.let { target ->
        ExerciseFormDialog(
            title = if (target is FormTarget.Edit) "Editar ejercicio" else "Nuevo ejercicio",
            initial = (target as? FormTarget.Edit)?.exercise,
            onSave = { name, sets, reps, note ->
                when (target) {
                    is FormTarget.Edit -> viewModel.saveExercise(target.exercise, name, sets, reps, note)
                    is FormTarget.Add -> viewModel.addExercise(name, sets, reps, note)
                }
                form = null
            },
            onDismiss = { form = null },
        )
    }

    toDelete?.let { exercise ->
        DeleteExerciseDialog(
            exercise = exercise,
            onConfirm = {
                viewModel.deleteExercise(exercise)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }

    if (editingTitle) {
        TitleDialog(
            initial = state.dayTitle,
            onSave = {
                viewModel.updateDayTitle(it)
                editingTitle = false
            },
            onDismiss = { editingTitle = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Selector de día — pills custom scrollables
// ─────────────────────────────────────────────────────────────

@Composable
private fun DaySelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..7).forEach { dow ->
            DayPill(
                label = DayOfWeek.of(dow)
                    .getDisplayName(TextStyle.SHORT, LOCALE_ES_MX)
                    .trimEnd('.')
                    .replaceFirstChar { it.uppercase() },
                selected = dow == selected,
                onClick = { onSelect(dow) },
            )
        }
    }
}

@Composable
private fun DayPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (selected) primary else outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Header del día
// ─────────────────────────────────────────────────────────────

@Composable
private fun DayHeader(
    dayOfWeek: Int,
    title: String,
    exerciseCount: Int,
    onEditTitle: () -> Unit,
) {
    val dayName = DayOfWeek.of(dayOfWeek)
        .getDisplayName(TextStyle.FULL, LOCALE_ES_MX)
        .replaceFirstChar { it.uppercase() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            CountChip(
                text = when (exerciseCount) {
                    0 -> "DESCANSO"
                    1 -> "1 EJERCICIO"
                    else -> "$exerciseCount EJERCICIOS"
                },
                muted = exerciseCount == 0,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title.ifBlank { "Sin título" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEditTitle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar título del día",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun CountChip(text: String, muted: Boolean) {
    val bg = if (muted) MaterialTheme.colorScheme.surfaceContainerHighest
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val fg = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Entry card (single o superserie)
// ─────────────────────────────────────────────────────────────

@Composable
private fun EntryCard(
    entry: List<ExerciseEntity>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLinkWithNext: (() -> Unit)?,
    onUnlink: () -> Unit,
    onEdit: (ExerciseEntity) -> Unit,
    onDelete: (ExerciseEntity) -> Unit,
    onPickImage: (ExerciseEntity) -> Unit,
    onRemoveImage: (ExerciseEntity) -> Unit,
) {
    val isSuperset = entry.size > 1
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline

    val borderColor = if (isSuperset) secondary.copy(alpha = 0.4f) else outline.copy(alpha = 0.4f)
    val borderWidth = if (isSuperset) 1.5.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Chip de superserie
            if (isSuperset) {
                SupersetChip(rounds = entry.minOf { it.sets })
            }

            // Ejercicios
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                entry.forEachIndexed { index, exercise ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(outline.copy(alpha = 0.3f)),
                        )
                    }
                    ExerciseBlock(
                        exercise = exercise,
                        showSets = !isSuperset,
                        onEdit = { onEdit(exercise) },
                        onDelete = { onDelete(exercise) },
                        onPickImage = { onPickImage(exercise) },
                        onRemoveImage = { onRemoveImage(exercise) },
                    )
                }
            }

            // Fila inferior: reorden + enlaces
            val showActionsRow = true
            if (showActionsRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MoveButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Subir",
                        enabled = canMoveUp,
                        onClick = onMoveUp,
                    )
                    MoveButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bajar",
                        enabled = canMoveDown,
                        onClick = onMoveDown,
                    )

                    Spacer(Modifier.weight(1f))

                    if (isSuperset) {
                        SmallChipButton(
                            text = "Separar",
                            onClick = onUnlink,
                        )
                    }
                    onLinkWithNext?.let { link ->
                        SmallChipButton(
                            text = "Enlazar →",
                            onClick = link,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupersetChip(rounds: Int) {
    val secondary = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .background(secondary.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, secondary.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "SUPERSERIE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = secondary,
        )
        Text(
            text = "· $rounds rondas",
            style = MaterialTheme.typography.labelMedium,
            color = secondary.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun MoveButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SmallChipButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Bloque de ejercicio dentro de un entry
// ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseBlock(
    exercise: ExerciseEntity,
    showSets: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    val hasImage = exercise.imageUri != null

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Imagen + botón quitar ──
        Box {
            ExerciseImage(
                imageName = exercise.imageUri,
                exerciseName = exercise.name,
                size = 64.dp,
                onClick = onPickImage,
            )
            if (hasImage) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRemoveImage,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quitar imagen",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        // ── Info ──
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (showSets) "${exercise.sets} series · ${exercise.reps}"
                else exercise.reps,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (exercise.note.isNotBlank()) {
                Text(
                    text = exercise.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── Acciones verticales ──
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconSquareAction(
                icon = Icons.Default.Edit,
                contentDescription = "Editar ejercicio",
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onEdit,
            )
            IconSquareAction(
                icon = Icons.Default.Delete,
                contentDescription = "Eliminar ejercicio",
                container = DangerRed.copy(alpha = 0.12f),
                content = DangerRed,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun IconSquareAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyDayCard() {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(primary.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = "Día vacío",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Usa «Agregar ejercicio» para armar tu rutina de este día.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Dialog: formulario add/edit
// ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseFormDialog(
    title: String,
    initial: ExerciseEntity?,
    onSave: (name: String, sets: Int, reps: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var sets by remember { mutableStateOf((initial?.sets ?: 3).toString()) }
    var reps by remember { mutableStateOf(initial?.reps.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    val setsValue = sets.toIntOrNull()?.takeIf { it in 1..20 }
    val valid = name.isNotBlank() && setsValue != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter(Char::isDigit).take(2) },
                    label = { Text("Series (1 a 20)") },
                    singleLine = true,
                    isError = setsValue == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial?.supersetGroup != null) {
                    Text(
                        text = "Este ejercicio está en una superserie: las series se aplican a todos sus ejercicios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Repeticiones (ej. 8-12 reps, Al fallo)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota técnica") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    setsValue?.let { onSave(name.trim(), it, reps.trim(), note.trim()) }
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────
// Dialog: editar título del día
// ─────────────────────────────────────────────────────────────

@Composable
private fun TitleDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Título del día",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Ej. Empuje (Pecho, Hombro y Tríceps)") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = { onSave(text.trim()) },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────
// Dialog: eliminar ejercicio (destructivo)
// ─────────────────────────────────────────────────────────────

@Composable
private fun DeleteExerciseDialog(
    exercise: ExerciseEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DangerRed.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, DangerRed.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Eliminar ejercicio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Text(
                text = "¿Eliminar «${exercise.name}»? También se borra su progreso registrado y su imagen. " +
                        "Si forma parte de una superserie de dos, el otro ejercicio quedará suelto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Eliminar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}