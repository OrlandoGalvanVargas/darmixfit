package com.darmix.darmixfit.ui.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.data.ProgramTemplate
import com.darmix.darmixfit.data.ProgramTemplates
import com.darmix.darmixfit.ui.theme.DangerRed
import com.darmix.darmixfit.ui.common.FitFlowSnackbar
import com.darmix.darmixfit.ui.common.FitFlowSnackbarVisuals
import com.darmix.darmixfit.ui.common.MessageType
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun ProgramsScreen(
    viewModel: ProgramsViewModel,
    modifier: Modifier = Modifier,
) {
    val programs by viewModel.programs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(
                FitFlowSnackbarVisuals(
                    message = msg.text,
                    type = msg.type,
                )
            )
        }
    }

    var renaming by remember { mutableStateOf<ProgramEntity?>(null) }
    var deleting by remember { mutableStateOf<ProgramEntity?>(null) }
    var viewing by remember { mutableStateOf<ProgramTemplate?>(null) }
    var creatingBlank by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ─── Sección: Mis programas ───
            item(key = "mine-title") {
                SectionHeader(
                    title = "Mis programas",
                    trailing = "${programs.size}",
                    subtitle = "Cada uno guarda su propio progreso.",
                    icon = Icons.Default.List,          // ← icono
                )
            }

            if (programs.isEmpty()) {
                item(key = "empty") {
                    EmptyProgramsCard()
                }
            } else {
                items(programs, key = { "p-${it.id}" }) { program ->
                    ProgramCard(
                        program = program,
                        canDelete = programs.size > 1,
                        onActivate = { viewModel.activate(program.id) },
                        onRename = { renaming = program },
                        onDelete = { deleting = program },
                    )
                }
            }

            // ─── Sección: Plantillas ───
            item(key = "templates-title") {
                SectionHeader(
                    title = "Agregar desde plantilla",
                    subtitle = "Se añade como programa nuevo y se activa.",
                    modifier = Modifier.padding(top = 12.dp),
                    icon = Icons.Default.Add,            // ← icono
                )
            }

            items(ProgramTemplates.all, key = { "t-${it.key}" }) { template ->
                TemplateCard(
                    template = template,
                    onView = { viewing = template },
                    onAdd = { viewModel.addFromTemplate(template) },
                )
            }

            item(key = "blank") {
                BlankProgramCard(onCreate = { creatingBlank = true })
            }

            item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        ) { data ->
            val visuals = data.visuals as? FitFlowSnackbarVisuals
            FitFlowSnackbar(
                message = data.visuals.message,
                type = visuals?.type ?: MessageType.INFO,
            )
        }
    }

    renaming?.let { program ->
        NameDialog(
            title = "Renombrar programa",
            initial = program.name,
            onSave = {
                viewModel.rename(program.id, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    if (creatingBlank) {
        NameDialog(
            title = "Programa en blanco",
            initial = "Mi programa",
            onSave = {
                viewModel.addFromTemplate(ProgramTemplates.blank, it)
                creatingBlank = false
            },
            onDismiss = { creatingBlank = false },
        )
    }

    deleting?.let { program ->
        DeleteDialog(
            program = program,
            onConfirm = {
                viewModel.delete(program)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }

    viewing?.let { template ->
        TemplateDetailDialog(
            template = template,
            onAdd = {
                viewModel.addFromTemplate(template)
                viewing = null
            },
            onDismiss = { viewing = null },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Header de sección — compacto, con badge opcional
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    icon: ImageVector? = null,   // ← nuevo
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Icono en círculo lima al 14% (sustituye a la barra)
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            } else {
                // Fallback a la barra
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(22.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = CircleShape,
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Card de programa (activo / inactivo)
// ─────────────────────────────────────────────────────────────

@Composable
private fun ProgramCard(
    program: ProgramEntity,
    canDelete: Boolean,
    onActivate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val active = program.isActive
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) primary else outline.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.large,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 4.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Fila superior: nombre + chip ACTIVO ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (active) {
                    ActiveChip()
                }
            }

            // ── Chips de metadatos ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(
                    icon = Icons.Default.Schedule,
                    label = formatTime(program.restSeconds),
                )
            }

            // ── Descripción ──
            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }

            // ── Acciones ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active) {
                    OutlinedButton(
                        onClick = onRename,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Renombrar")
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                DangerRed.copy(alpha = if (canDelete) 0.12f else 0.05f)
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = if (canDelete) DangerRed
                            else DangerRed.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Button(
                        onClick = onActivate,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Activar", fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Renombrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                DangerRed.copy(alpha = if (canDelete) 0.12f else 0.05f)
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = if (canDelete) DangerRed
                            else DangerRed.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Card de plantilla
// ─────────────────────────────────────────────────────────────

@Composable
private fun TemplateCard(
    template: ProgramTemplate,
    onView: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.large,
            ),
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
            // ── Nombre + chip de nivel ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                LevelChip(level = template.level)
            }

            // ── Chips de metadatos ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(
                    icon = Icons.Default.FitnessCenter,
                    label = "${template.trainingDays} días",
                )
                MetaChip(
                    icon = Icons.Default.Schedule,
                    label = formatTime(template.restSeconds),
                )
            }

            // ── Resumen ──
            Text(
                text = template.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Acciones ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onView,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Ver plan")
                }
                Button(
                    onClick = onAdd,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Card de programa en blanco — CTA claro al final
// ─────────────────────────────────────────────────────────────

@Composable
private fun BlankProgramCard(onCreate: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = primary.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.large,
            ),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primary.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, primary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ProgramTemplates.blank.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Empieza desde cero y arma tu plan a tu manera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onCreate,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text("Crear programa", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Empty state (por si acaso)
// ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyProgramsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.large,
            ),
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Sin programas todavía",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Agrega una plantilla o crea uno desde cero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Chips reutilizables
// ─────────────────────────────────────────────────────────────

@Composable
private fun ActiveChip() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "ACTIVO",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun MetaChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LevelChip(level: String) {
    // Color por nivel: lima = principiante, ámbar = intermedio, rojo = avanzado.
    // Si no matchea, cae a "outline" neutro.
    val (bg, fg) = when (level.lowercase(Locale.ROOT)) {
        "principiante", "beginner" -> Pair(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.primary,
        )
        "intermedio", "intermediate" -> Pair(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.secondary,
        )
        "avanzado", "advanced" -> Pair(
            DangerRed.copy(alpha = 0.16f),
            DangerRed,
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = level.uppercase(LocalLocale.current.platformLocale),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Dialog: detalle de plantilla
// ─────────────────────────────────────────────────────────────

@Composable
private fun TemplateDetailDialog(
    template: ProgramTemplate,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${template.trainingDays} días · ${template.level} · " +
                            "descanso ${formatTime(template.restSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                template.days.forEachIndexed { index, day ->
                    DayBlock(
                        dayIndex = index + 1,
                        title = day.title,
                        exercises = day.exercises.map {
                            Triple(it.name, "${it.sets} × ${it.reps}", it.group != null)
                        },
                    )
                }
                Text(
                    text = "↔ = superserie o circuito: los ejercicios se hacen seguidos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAdd,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("Agregar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cerrar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun DayBlock(
    dayIndex: Int,
    title: String,
    exercises: List<Triple<String, String, Boolean>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = CircleShape,
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "D${dayIndex}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        if (exercises.isEmpty()) {
            Text(
                text = "Sin ejercicios",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            exercises.forEach { (name, detail, isSuperset) ->
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (isSuperset) "↔" else "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuperset) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Dialog: renombrar / crear
// ─────────────────────────────────────────────────────────────

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
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
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nombre del programa") },
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
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
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
// Dialog: eliminar (destructivo, enfatizado en rojo)
// ─────────────────────────────────────────────────────────────

@Composable
private fun DeleteDialog(
    program: ProgramEntity,
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
                    text = "Eliminar programa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Text(
                text = "¿Eliminar «${program.name}»? Se borran todos sus ejercicios, " +
                        "imágenes, progreso e historial. Esta acción no se puede deshacer.",
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
            ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
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
// Helpers
// ─────────────────────────────────────────────────────────────

private fun dayName(dayOfWeek: Int): String =
    DayOfWeek.of(dayOfWeek)
        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-MX"))
        .replaceFirstChar { it.uppercase() }

private fun formatTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)