package com.darmix.darmixfit.ui.programs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixfit.data.ExerciseCatalog
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.data.ProgramTemplate
import com.darmix.darmixfit.data.ProgramTemplates
import com.darmix.darmixfit.data.TemplateExercise
import com.darmix.darmixfit.ui.common.DarmixToastHost
import com.darmix.darmixfit.ui.common.showToast
import com.darmix.darmixfit.ui.components.DarmixConfirmDialog
import com.darmix.darmixfit.ui.components.DarmixSheet
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing

private val DayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val DayShort = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")

private val HeroShape = CutCornerShape(topEnd = 20.dp, bottomStart = 20.dp)
private val CardShape = CutCornerShape(topEnd = 16.dp)
private val PlateShape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
private val TagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val EmblemShape = CutCornerShape(12.dp)
private val BarShape = CutCornerShape(8.dp)
private val CellShape = CutCornerShape(5.dp)
private val FieldShape = CutCornerShape(10.dp)
private val DayCardShape = CutCornerShape(12.dp)
private val MenuShape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)

private val Display = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black)

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

private fun levelScore(level: String): Int {
    val lower = level.lowercase()
    return when {
        "avanzado" in lower -> 3
        "intermedio" in lower -> 2
        "principiante" in lower -> 1
        else -> 0
    }
}

private fun Modifier.glow(shape: Shape, color: Color, strength: Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        drawOutline(outline, color, alpha = 0.07f * strength, style = Stroke(16.dp.toPx(), join = StrokeJoin.Miter))
        drawOutline(outline, color, alpha = 0.16f * strength, style = Stroke(7.dp.toPx(), join = StrokeJoin.Miter))
        drawOutline(outline, color, alpha = 0.4f * strength, style = Stroke(3.dp.toPx(), join = StrokeJoin.Miter))
    }
}

private class Stat(val value: String, val label: String, val color: Color)

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            overline = "RENOMBRAR",
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
            overline = "PROGRAMA EN BLANCO",
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.hairline)
            )
            if (count != null) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 22.dp)
                        .widthIn(min = 22.dp)
                        .clip(CellShape)
                        .background(colors.hairlineStrong)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = colors.textPrimary
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun StatBar(stats: List<Stat>, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(BarShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairline, BarShape)
    ) {
        stats.forEachIndexed { index, stat ->
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
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.value,
                    style = Display.copy(fontSize = 22.sp, lineHeight = 22.sp),
                    color = stat.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
                    color = colors.textTertiary,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
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
    val stats = buildList {
        if (template != null && template.trainingDays > 0) {
            add(Stat("${template.trainingDays}", "DÍAS", colors.textPrimary))
            val score = levelScore(template.level)
            add(
                Stat(
                    when (score) {
                        3 -> "AVANZ."
                        2 -> "INTERM."
                        1 -> "PRINC."
                        else -> "TODOS"
                    },
                    "NIVEL",
                    colors.textPrimary
                )
            )
        }
        add(Stat(formatTime(program.restSeconds), "DESCANSO", colors.tideText))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glow(HeroShape, colors.volt, if (colors.isDark) 1f else 0.45f)
            .clip(HeroShape)
            .background(
                Brush.verticalGradient(
                    0f to lerp(colors.card, colors.volt, if (colors.isDark) 0.07f else 0.08f),
                    0.4f to colors.card,
                    1f to colors.card
                )
            )
            .border(1.5.dp, colors.voltText, HeroShape)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .height(24.dp)
                    .clip(TagShape)
                    .background(colors.volt)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colors.onVolt)
                )
                Text(
                    text = "PROGRAMA ACTIVO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.5.sp),
                    color = colors.onVolt
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "EN USO",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.6.sp),
                color = colors.textTertiary
            )
        }
        Text(
            text = program.name.uppercase(),
            style = Display.copy(fontSize = 38.sp, lineHeight = 38.sp),
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (template != null && template.name != program.name) {
            Text(
                text = "Basado en ${template.name}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        StatBar(stats = stats, modifier = Modifier.padding(top = 14.dp))
        if (program.description.isNotBlank()) {
            Text(
                text = program.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostPlate(text = "RENOMBRAR", icon = Icons.Rounded.Edit, onClick = onRename, modifier = Modifier.weight(1f))
            DangerButton(enabled = canDelete, onClick = onDelete)
        }
    }
}

@Composable
private fun DangerButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = DarmixTheme.colors
    val shape = CutCornerShape(10.dp)
    val ember = colors.emberText
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(shape)
            .background(if (enabled) colors.emberSoft else Color.Transparent)
            .border(1.dp, if (enabled) ember.copy(alpha = 0.6f) else colors.hairline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Eliminar", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.DeleteOutline,
            contentDescription = "Eliminar",
            tint = if (enabled) ember else colors.textTertiary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
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
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.hairline, CardShape)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(EmblemShape)
                .background(colors.canvasDeep)
                .border(1.dp, colors.hairlineStrong, EmblemShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = program.name.firstOrNull { it.isLetter() }?.uppercase() ?: "?",
                style = Display.copy(fontSize = 24.sp, lineHeight = 24.sp),
                color = colors.textSecondary
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = program.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append("Descanso ${formatTime(program.restSeconds)}")
                    if (template != null && template.trainingDays > 0) append(" · ${template.trainingDays} días")
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary
            )
        }
        AccentPlate(
            text = "ACTIVAR",
            icon = Icons.Rounded.Bolt,
            accent = colors.volt,
            onClick = onActivate,
            height = 38.dp,
            horizontalPadding = 12.dp
        )
        Box {
            Box(
                modifier = Modifier
                    .size(width = 30.dp, height = 40.dp)
                    .clip(CutCornerShape(6.dp))
                    .clickable(role = Role.Button, onClickLabel = "Opciones") { menu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = MenuShape,
                containerColor = colors.cardRaised,
                border = BorderStroke(1.dp, colors.hairlineStrong),
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
private fun Emblem(days: Int, accent: Color, size: Dp = 58.dp) {
    val onAccent = onColorFor(accent)
    Box(
        modifier = Modifier
            .size(size)
            .clip(EmblemShape)
            .background(
                Brush.linearGradient(
                    listOf(lerp(accent, Color.White, 0.3f), accent, lerp(accent, Color.Black, 0.35f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$days",
            style = Display.copy(fontSize = (size.value * 0.66f).sp, lineHeight = (size.value * 0.7f).sp),
            color = onAccent
        )
    }
}

@Composable
private fun LevelMeter(level: String, accent: Color) {
    val colors = DarmixTheme.colors
    val score = levelScore(level)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = (4 + i * 3).dp)
                        .background(
                            when {
                                score == 0 -> accent
                                i <= score -> accent
                                else -> colors.hairlineStrong
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        template.days.forEachIndexed { index, day ->
            val training = day.exercises.isNotEmpty()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(CellShape)
                    .background(if (training) accent else colors.cardRaised),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = DayLetters[index],
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black),
                    color = if (training) onAccent else colors.textTertiary
                )
            }
        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.hairline, CardShape)
            .clickable(role = Role.Button, onClick = onView)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accent)
        )
        Column(modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Emblem(days = template.trainingDays, accent = accent)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    LevelMeter(level = template.level, accent = accent)
                }
            }
            Spacer(Modifier.height(14.dp))
            WeekStrip(template = template, accent = accent)
            StatBar(
                stats = listOf(
                    Stat("${template.trainingDays}", "DÍAS", colors.textPrimary),
                    Stat("$weeklySets", "SERIES / SEM", accent),
                    Stat(formatTime(template.restSeconds), "DESCANSO", colors.textPrimary)
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = template.summary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostPlate(text = "VER PLAN", icon = Icons.Rounded.Visibility, onClick = onView, modifier = Modifier.weight(1f))
                AccentPlate(
                    text = "AGREGAR",
                    icon = Icons.Rounded.Add,
                    accent = accent,
                    onClick = onAdd,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
private fun BlankCard(onCreate: () -> Unit) {
    val colors = DarmixTheme.colors
    val dash = colors.hairlineStrong
    val bracket = colors.voltText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRect(
                    color = dash,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
                )
                val inset = 4.dp.toPx()
                val arm = 10.dp.toPx()
                val w = 1.8.dp.toPx()
                listOf(
                    Offset(inset, inset) to Offset(1f, 1f),
                    Offset(size.width - inset, inset) to Offset(-1f, 1f),
                    Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f),
                    Offset(inset, size.height - inset) to Offset(1f, -1f)
                ).forEach { (p, d) ->
                    drawLine(bracket, p, Offset(p.x + arm * d.x, p.y), strokeWidth = w)
                    drawLine(bracket, p, Offset(p.x, p.y + arm * d.y), strokeWidth = w)
                }
            }
            .clickable(role = Role.Button, onClick = onCreate)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(EmblemShape)
                .background(colors.canvasDeep)
                .border(1.dp, colors.hairlineStrong, EmblemShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.voltText, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Desde cero",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.textPrimary
            )
            Text(
                text = "Un programa en blanco para armarlo a tu manera.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
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
            .clip(HeroShape)
            .background(colors.card)
            .border(1.dp, colors.hairlineStrong, HeroShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "SIN PROGRAMA ACTIVO", style = Display.copy(fontSize = 24.sp, lineHeight = 26.sp), color = colors.textPrimary)
        Text(
            text = "Elige una plantilla o crea uno desde cero.",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun AccentPlate(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    horizontalPadding: Dp = 16.dp,
    glowing: Boolean = false
) {
    val colors = DarmixTheme.colors
    val content = if (enabled) onColorFor(accent) else colors.textTertiary
    val fill = if (enabled) {
        Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.25f), accent, lerp(accent, Color.Black, 0.12f)))
    } else {
        SolidColor(colors.cardHigh)
    }
    Row(
        modifier = modifier
            .height(height)
            .then(if (glowing && enabled) Modifier.glow(PlateShape, accent, if (colors.isDark) 0.8f else 0.4f) else Modifier)
            .clip(PlateShape)
            .background(fill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black),
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun GhostPlate(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(PlateShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairlineStrong, PlateShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun SheetHeader(overline: String, overlineColor: Color, title: String, leading: (@Composable () -> Unit)? = null, below: (@Composable () -> Unit)? = null) {
    val colors = DarmixTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 2.dp)
                        .background(overlineColor)
                )
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.2.sp),
                    color = overlineColor
                )
            }
            Text(
                text = title,
                style = Display.copy(fontSize = 28.sp, lineHeight = 30.sp),
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            below?.invoke()
        }
    }
}

@Composable
private fun SheetDivider(color: Color) {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), colors.hairline, Color.Transparent)))
    )
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        targetValue = if (focused) colors.voltText else colors.hairline,
        animationSpec = tween(Motion.Quick),
        label = "name-border"
    )
    DarmixSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SheetHeader(overline = overline, overlineColor = colors.voltText, title = title)
            SheetDivider(colors.voltText)
            Column {
                Text(
                    text = "NOMBRE DEL PROGRAMA",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                    color = colors.textTertiary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    interactionSource = interaction,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(colors.voltText),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(FieldShape)
                                .background(colors.canvasDeep)
                                .border(1.dp, border, FieldShape)
                                .drawBehind {
                                    if (focused) drawRect(colors.voltText, size = Size(3.dp.toPx(), size.height))
                                }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            inner()
                        }
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostPlate(text = "CANCELAR", onClick = onDismiss, modifier = Modifier.width(120.dp))
                AccentPlate(
                    text = confirmLabel,
                    accent = colors.volt,
                    onClick = { onSave(text.trim()) },
                    enabled = text.isNotBlank(),
                    height = 48.dp,
                    glowing = true,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                overline = "PLANTILLA",
                overlineColor = accent,
                title = template.name.uppercase(),
                leading = { Emblem(days = template.trainingDays, accent = accent, size = 64.dp) },
                below = { LevelMeter(level = template.level, accent = accent) }
            )
            StatBar(
                stats = listOf(
                    Stat("${template.trainingDays}", "DÍAS", colors.textPrimary),
                    Stat("$weeklySets", "SERIES / SEM", accent),
                    Stat(formatTime(template.restSeconds), "DESCANSO", colors.tideText)
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = template.summary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                color = colors.textSecondary
            )
            SheetDivider(accent)

            template.days.forEachIndexed { index, day ->
                if (day.exercises.isEmpty()) {
                    RestLine(dayLabel = DayShort[index], title = day.title)
                } else {
                    PlanDay(dayLabel = DayShort[index], title = day.title, exercises = day.exercises, accent = accent)
                }
            }

            AccentPlate(
                text = "AGREGAR Y ACTIVAR",
                icon = Icons.Rounded.Add,
                accent = accent,
                onClick = onAdd,
                height = 52.dp,
                glowing = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RestLine(dayLabel: String, title: String) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val shape = CutCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(lerp(colors.card, colors.tide, if (colors.isDark) 0.05f else 0.06f))
            .border(1.dp, tide.copy(alpha = 0.3f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black),
            color = tide,
            modifier = Modifier.width(34.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        ChargingBattery(size = 20.dp, color = tide, bolt = colors.onTide)
    }
}

@Composable
private fun PlanDay(dayLabel: String, title: String, exercises: List<TemplateExercise>, accent: Color) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    val onTide = onColorFor(tide)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(DayCardShape)
            .background(colors.card)
            .border(1.dp, colors.hairline, DayCardShape)
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(accent)
        )
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black),
                    color = accent
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            exercises.forEach { exercise ->
                val grouped = exercise.group != null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                        if (grouped) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CutCornerShape(topEnd = 4.dp, bottomStart = 4.dp))
                                    .background(tide),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Repeat, contentDescription = "Superserie", tint = onTide, modifier = Modifier.size(11.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(colors.hairlineStrong)
                            )
                        }
                    }
                    Box(modifier = Modifier.clip(CutCornerShape(7.dp))) {
                        ExerciseImage(
                            imageName = remember(exercise.name) { ExerciseCatalog.imageRefFor(exercise.name) },
                            exerciseName = exercise.name,
                            size = 44.dp,
                            animated = false
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 17.sp),
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${exercise.sets} × ${exercise.reps}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (grouped) tide else accent
                        )
                    }
                }
            }
        }
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
