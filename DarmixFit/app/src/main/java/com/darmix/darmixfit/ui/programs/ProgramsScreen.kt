package com.darmix.darmixfit.ui.programs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixfit.data.ExerciseCatalog
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.data.ProgramTemplate
import com.darmix.darmixfit.data.ProgramTemplates
import com.darmix.darmixfit.data.TemplateExercise
import com.darmix.darmixfit.ui.common.DarmixToastHost
import com.darmix.darmixfit.ui.common.showToast
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.DarmixConfirmDialog
import com.darmix.darmixfit.ui.components.DarmixIconButton
import com.darmix.darmixfit.ui.components.DarmixSheet
import com.darmix.darmixfit.ui.components.DarmixTextField
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.InfoChip
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.PulseDot
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Spacing

private val DayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val DayShort = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
private val EmblemShape = RoundedCornerShape(16.dp)

private fun formatTime(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

private fun onColorFor(color: Color): Color = if (color.luminance() > 0.45f) DarmixPalette.Ink950 else Color.White

private fun templateFor(program: ProgramEntity): ProgramTemplate? =
    ProgramTemplates.all.firstOrNull { it.key == program.templateKey }

private fun ProgramTemplate.weeklySets(): Int = days.sumOf { day ->
    val singles = day.exercises.filter { it.group == null }.sumOf { it.sets }
    val groups = day.exercises.filter { it.group != null }.groupBy { it.group }.values.sumOf { group -> group.minOf { it.sets } }
    singles + groups
}

private fun accentFor(index: Int, colors: DarmixColors): Color = when (index % 4) {
    0 -> colors.voltText
    1 -> colors.tideText
    2 -> colors.leafText
    else -> colors.emberText
}

@Composable
fun ProgramsScreen(
    viewModel: ProgramsViewModel,
    modifier: Modifier = Modifier
) {
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val colors = DarmixTheme.colors
    val active = programs.firstOrNull { it.isActive }
    val others = remember(programs) { programs.filterNot { it.isActive } }
    val canDelete = programs.size > 1

    val toastState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { toastState.showToast(it) }
    }

    var renaming by remember { mutableStateOf<ProgramEntity?>(null) }
    var deleting by remember { mutableStateOf<ProgramEntity?>(null) }
    var viewing by remember { mutableStateOf<ProgramTemplate?>(null) }
    var creatingBlank by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.sm, bottom = Spacing.huge),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(key = "active-label", contentType = "label") {
                SectionLabel(title = "EN USO")
            }
            item(key = "active", contentType = "active") {
                if (active != null) {
                    ActiveProgramCard(
                        program = active,
                        canDelete = canDelete,
                        onRename = { renaming = active },
                        onDelete = { deleting = active }
                    )
                } else {
                    EmptyPrograms()
                }
            }

            if (others.isNotEmpty()) {
                item(key = "mine-label", contentType = "label") {
                    SectionLabel(title = "MIS PROGRAMAS", count = programs.size)
                }
                items(others, key = { "p-${it.id}" }, contentType = { "program" }) { program ->
                    ProgramRow(
                        program = program,
                        canDelete = canDelete,
                        onActivate = { viewModel.activate(program.id) },
                        onRename = { renaming = program },
                        onDelete = { deleting = program },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "templates-label", contentType = "label") {
                SectionLabel(
                    title = "PLANTILLAS",
                    count = ProgramTemplates.all.size,
                    subtitle = "Se agregan como programa nuevo y se activan al instante."
                )
            }
            itemsIndexed(ProgramTemplates.all, key = { _, template -> "t-${template.key}" }, contentType = { _, _ -> "template" }) { index, template ->
                TemplateCard(
                    template = template,
                    accent = accentFor(index, colors),
                    onView = { viewing = template },
                    onAdd = { viewModel.addFromTemplate(template) }
                )
            }
            item(key = "blank", contentType = "blank") {
                BlankCard(onCreate = { creatingBlank = true })
            }
        }

        DarmixToastHost(
            hostState = toastState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.sm)
        )
    }

    renaming?.let { program ->
        NameSheet(
            overline = "Renombrar",
            title = "¿Cómo se llama ahora?",
            initial = program.name,
            confirmLabel = "GUARDAR",
            onSave = {
                viewModel.rename(program.id, it)
                renaming = null
            },
            onDismiss = { renaming = null }
        )
    }

    if (creatingBlank) {
        NameSheet(
            overline = "Programa en blanco",
            title = "Ponle nombre a tu programa",
            initial = "Mi programa",
            confirmLabel = "CREAR",
            onSave = {
                viewModel.addFromTemplate(ProgramTemplates.blank, it)
                creatingBlank = false
            },
            onDismiss = { creatingBlank = false }
        )
    }

    deleting?.let { program ->
        DarmixConfirmDialog(
            title = "¿Eliminar programa?",
            message = "Se borrarán «${program.name}» y todos sus ejercicios, imágenes, progreso e historial. " +
                    "Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            icon = Icons.Rounded.DeleteOutline,
            onConfirm = {
                viewModel.delete(program)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }

    viewing?.let { template ->
        val index = ProgramTemplates.all.indexOf(template)
        TemplateSheet(
            template = template,
            accent = accentFor(index.coerceAtLeast(0), colors),
            onAdd = {
                viewModel.addFromTemplate(template)
                viewing = null
            },
            onDismiss = { viewing = null }
        )
    }
}

@Composable
private fun SectionLabel(title: String, count: Int? = null, subtitle: String? = null) {
    val colors = DarmixTheme.colors
    Column(
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.hairline)
            )
            if (count != null) Overline(text = "$count", color = colors.textTertiary)
        }
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ActiveProgramCard(
    program: ProgramEntity,
    canDelete: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = DarmixTheme.colors
    val template = remember(program.templateKey) { templateFor(program) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .drawWithCache {
                val center = Offset(size.width, 0f)
                val radius = size.width * 0.8f
                val glow = Brush.radialGradient(
                    listOf(colors.volt.copy(alpha = if (colors.isDark) 0.16f else 0.26f), Color.Transparent),
                    center = center,
                    radius = radius
                )
                onDrawBehind { drawCircle(glow, radius = radius, center = center) }
            }
            .border(1.5.dp, colors.voltText.copy(alpha = 0.6f), DarmixShapes.Hero)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PulseDot(color = colors.voltText)
            Overline(text = "Programa activo", color = colors.voltText)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = program.name.uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (template != null && template.name != program.name) {
                Text(text = "Basado en ${template.name}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
            InfoChip(text = "Descanso ${formatTime(program.restSeconds)}", content = colors.tideText)
            if (template != null && template.trainingDays > 0) {
                InfoChip(text = "${template.trainingDays} días", content = colors.textPrimary)
                InfoChip(text = template.level, content = colors.textSecondary)
            }
        }
        if (program.description.isNotBlank()) {
            Text(
                text = program.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
            DarmixButton(
                text = "Renombrar",
                onClick = onRename,
                style = DarmixButtonStyle.Secondary,
                icon = Icons.Rounded.Edit,
                height = 48.dp,
                haptic = null,
                modifier = Modifier.weight(1f)
            )
            DarmixIconButton(
                icon = Icons.Rounded.DeleteOutline,
                contentDescription = "Eliminar",
                onClick = onDelete,
                enabled = canDelete,
                style = DarmixButtonStyle.Secondary,
                containerColor = colors.emberSoft,
                contentColor = colors.emberText,
                size = 48.dp
            )
        }
    }
}

@Composable
private fun ProgramRow(
    program: ProgramEntity,
    canDelete: Boolean,
    onActivate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val template = remember(program.templateKey) { templateFor(program) }
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Card)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Card)
            .padding(start = Spacing.md, end = Spacing.xs, top = Spacing.md, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(EmblemShape)
                .background(colors.cardHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = program.name.firstOrNull { it.isLetter() }?.uppercase() ?: "?",
                style = DarmixTextStyles.MetricSmall,
                color = colors.textSecondary
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = program.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append("Descanso ${formatTime(program.restSeconds)}")
                    if (template != null && template.trainingDays > 0) append(" · ${template.trainingDays} días")
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        DarmixButton(
            text = "Activar",
            onClick = onActivate,
            icon = Icons.Rounded.Bolt,
            height = 40.dp,
            shape = DarmixShapes.Pill
        )
        Box {
            DarmixIconButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "Opciones",
                onClick = { menu = true },
                style = DarmixButtonStyle.Secondary,
                containerColor = Color.Transparent,
                contentColor = colors.textSecondary,
                size = 40.dp,
                shape = CircleShape
            )
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = DarmixShapes.Card,
                containerColor = colors.cardRaised,
                border = BorderStroke(1.dp, colors.hairline),
                shadowElevation = 12.dp,
                tonalElevation = 0.dp
            ) {
                MenuItem(text = "Renombrar", icon = Icons.Rounded.Edit, tint = colors.textPrimary) {
                    menu = false
                    onRename()
                }
                if (canDelete) {
                    HorizontalDivider(color = colors.hairline, modifier = Modifier.padding(vertical = Spacing.xs))
                    MenuItem(text = "Eliminar", icon = Icons.Rounded.DeleteOutline, tint = colors.emberText) {
                        menu = false
                        onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = text, style = MaterialTheme.typography.titleSmall, color = tint) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint) },
        onClick = onClick
    )
}

@Composable
private fun Emblem(days: Int, accent: Color, size: Dp = 56.dp) {
    val onAccent = onColorFor(accent)
    Box(
        modifier = Modifier
            .size(size)
            .clip(EmblemShape)
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f)))),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "$days", style = DarmixTextStyles.Metric.copy(fontSize = DarmixTextStyles.Metric.fontSize * 0.8f), color = onAccent)
            Text(
                text = "D",
                style = MaterialTheme.typography.titleSmall,
                color = onAccent.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun LevelMeter(level: String, accent: Color) {
    val colors = DarmixTheme.colors
    val lower = level.lowercase()
    val score = when {
        "avanzado" in lower -> 3
        "intermedio" in lower -> 2
        "principiante" in lower -> 1
        else -> 0
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = (5 + i * 3).dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                score == 0 -> accent.copy(alpha = 0.45f)
                                i <= score -> accent
                                else -> colors.cardHigh
                            }
                        )
                )
            }
        }
        Text(
            text = if (score == 0) "Todos los niveles" else level,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun WeekStrip(template: ProgramTemplate, accent: Color) {
    val colors = DarmixTheme.colors
    val onAccent = onColorFor(accent)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        template.days.forEachIndexed { index, day ->
            val training = day.exercises.isNotEmpty()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (training) accent else colors.cardHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = DayLetters[index],
                    style = MaterialTheme.typography.labelLarge,
                    color = if (training) onAccent else colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun TemplateStat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    Column(modifier = modifier) {
        Text(text = value, style = DarmixTextStyles.MetricSmall, color = colors.textPrimary)
        Overline(text = label, color = colors.textTertiary)
    }
}

@Composable
private fun TemplateCard(
    template: ProgramTemplate,
    accent: Color,
    onView: () -> Unit,
    onAdd: () -> Unit
) {
    val colors = DarmixTheme.colors
    val weeklySets = remember(template) { template.weeklySets() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(accent.copy(alpha = if (colors.isDark) 0.08f else 0.06f), Color.Transparent),
                        endY = size.height * 0.45f
                    )
                )
            }
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .clickable(role = Role.Button, onClick = onView)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Emblem(days = template.trainingDays, accent = accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LevelMeter(level = template.level, accent = accent)
            }
        }
        WeekStrip(template = template, accent = accent)
        Row(modifier = Modifier.fillMaxWidth()) {
            TemplateStat(value = "${template.trainingDays}", label = "Días", modifier = Modifier.weight(1f))
            TemplateStat(value = "$weeklySets", label = "Series / sem", modifier = Modifier.weight(1f))
            TemplateStat(value = formatTime(template.restSeconds), label = "Descanso", modifier = Modifier.weight(1f))
        }
        Text(
            text = template.summary,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
            DarmixButton(
                text = "Ver plan",
                onClick = onView,
                style = DarmixButtonStyle.Ghost,
                icon = Icons.Rounded.Visibility,
                height = 48.dp,
                haptic = null,
                modifier = Modifier.weight(1f)
            )
            DarmixButton(
                text = "AGREGAR",
                onClick = onAdd,
                icon = Icons.Rounded.Add,
                height = 48.dp,
                containerColor = accent,
                contentColor = onColorFor(accent),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BlankCard(onCreate: () -> Unit) {
    val colors = DarmixTheme.colors
    Row(
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
            .clickable(role = Role.Button, onClick = onCreate)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(EmblemShape)
                .background(colors.cardHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.voltText, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Desde cero", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(
                text = "Un programa en blanco para armarlo a tu manera.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun EmptyPrograms() {
    val colors = DarmixTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(text = "Sin programa activo", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text(text = "Elige una plantilla o crea uno desde cero.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun NameSheet(
    overline: String,
    title: String,
    initial: String,
    confirmLabel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                Overline(text = overline, color = colors.voltText)
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
            DarmixTextField(value = text, onValueChange = { text = it }, label = "Nombre del programa")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                DarmixButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    style = DarmixButtonStyle.Ghost,
                    haptic = null,
                    modifier = Modifier.width(120.dp)
                )
                DarmixButton(
                    text = confirmLabel,
                    onClick = { onSave(text.trim()) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TemplateSheet(
    template: ProgramTemplate,
    accent: Color,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = DarmixTheme.colors
    val weeklySets = remember(template) { template.weeklySets() }
    DarmixSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Emblem(days = template.trainingDays, accent = accent, size = 64.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Overline(text = "Plantilla", color = accent)
                    Text(text = template.name.uppercase(), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    LevelMeter(level = template.level, accent = accent)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
                InfoChip(text = "${template.trainingDays} días", content = colors.textPrimary)
                InfoChip(text = "$weeklySets series / sem", content = accent)
                InfoChip(text = "Descanso ${formatTime(template.restSeconds)}", content = colors.tideText)
            }
            Text(text = template.summary, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)

            template.days.forEachIndexed { index, day ->
                if (day.exercises.isEmpty()) {
                    RestLine(dayLabel = DayShort[index], title = day.title)
                } else {
                    PlanDay(dayLabel = DayShort[index], title = day.title, exercises = day.exercises, accent = accent)
                }
            }

            DarmixButton(
                text = "AGREGAR Y ACTIVAR",
                onClick = onAdd,
                icon = Icons.Rounded.Add,
                height = 56.dp,
                containerColor = accent,
                contentColor = onColorFor(accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RestLine(dayLabel: String, title: String) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(colors.cardRaised)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text = dayLabel, style = MaterialTheme.typography.labelLarge, color = colors.tideText, modifier = Modifier.width(36.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun PlanDay(dayLabel: String, title: String, exercises: List<TemplateExercise>, accent: Color) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Card)
            .background(colors.cardRaised)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = dayLabel, style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.width(36.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        exercises.forEachIndexed { index, exercise ->
            val grouped = exercise.group != null
            val previousSame = index > 0 && grouped && exercises[index - 1].group == exercise.group
            val nextSame = index < exercises.lastIndex && grouped && exercises[index + 1].group == exercise.group
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(44.dp)
                        .drawBehind {
                            if (grouped) {
                                val x = size.width / 2f
                                val w = 2.dp.toPx()
                                if (previousSame) drawLine(tide.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height / 2f), w)
                                if (nextSame) drawLine(tide.copy(alpha = 0.5f), Offset(x, size.height / 2f), Offset(x, size.height), w)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (grouped) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(tide),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Repeat, contentDescription = "Superserie", tint = onColorFor(tide), modifier = Modifier.size(12.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.hairlineStrong)
                        )
                    }
                }
                ExerciseImage(
                    imageName = remember(exercise.name) { ExerciseCatalog.imageRefFor(exercise.name) },
                    exerciseName = exercise.name,
                    size = 44.dp,
                    animated = false
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${exercise.sets} × ${exercise.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (grouped) tide else accent
                    )
                }
            }
        }
    }
}
