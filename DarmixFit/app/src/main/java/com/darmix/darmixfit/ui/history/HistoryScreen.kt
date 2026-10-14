package com.darmix.darmixfit.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.isComplete
import com.darmix.darmixfit.ui.theme.AmberAccent
import com.darmix.darmixfit.ui.theme.SuccessGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE: Locale = Locale.forLanguageTag("es-MX")

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            MonthHeader(
                month = state.month,
                showToday = state.month != YearMonth.from(state.today) || state.selected != state.today,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToday = viewModel::goToToday,
            )
        }
        item(key = "calendar") {
            CalendarGrid(state = state, onSelect = viewModel::select)
        }
        item(key = "legend") { Legend() }
        item(key = "stats-hero") { StreakHeroCard(streak = state.stats.weeklyStreak) }
        item(key = "stats-grid") { StatsGrid(state.stats) }
        item(key = "detail") { DayDetailCard(state) }
        item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// Header: pill con chevrons circulares + chip "Hoy"
// ─────────────────────────────────────────────────────────────

@Composable
private fun MonthHeader(
    month: YearMonth,
    showToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val title = month.month.getDisplayName(TextStyle.FULL, LOCALE)
        .replaceFirstChar { it.uppercase() } + " " + month.year

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ChevronButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Mes anterior",
                onClick = onPrevious,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            ChevronButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Mes siguiente",
                onClick = onNext,
            )
        }

        if (showToday) {
            TodayChip(onClick = onToday)
        }
    }
}

@Composable
private fun ChevronButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TodayChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Today,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Ir a hoy",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Calendario
// ─────────────────────────────────────────────────────────────

private val WeekLabels = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
private fun CalendarGrid(state: HistoryUiState, onSelect: (LocalDate) -> Unit) {
    val month = state.month
    val offset = month.atDay(1).dayOfWeek.value - 1
    val length = month.lengthOfMonth()
    val weeks = (offset + length + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (week in 0 until weeks) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (column in 0 until 7) {
                    val dayNumber = week * 7 + column - offset + 1
                    if (dayNumber in 1..length) {
                        val date = month.atDay(dayNumber)
                        DayCell(
                            date = date,
                            status = state.statuses[date],
                            isToday = date == state.today,
                            isSelected = date == state.selected,
                            isFuture = date.isAfter(state.today),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus?,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary

    // Fondo según estado (prioridad: complete > partial > selected > transparente)
    val bg = when {
        status == DayStatus.COMPLETE -> primary
        status == DayStatus.PARTIAL -> primary.copy(alpha = 0.15f)
        isSelected -> scheme.surfaceContainerHighest
        else -> Color.Transparent
    }
    // Borde según prioridad (seleccionado manda sobre hoy, que manda sobre parcial)
    val (borderWidth, borderColor) = when {
        isSelected -> 2.dp to scheme.onSurface
        isToday -> 1.5.dp to primary
        status == DayStatus.PARTIAL -> 1.5.dp to primary.copy(alpha = 0.6f)
        else -> 1.dp to scheme.outline.copy(alpha = 0.25f)
    }
    // Texto
    val fg = when {
        status == DayStatus.COMPLETE -> scheme.onPrimary
        status == DayStatus.PARTIAL -> primary
        isFuture -> scheme.onSurface.copy(alpha = 0.35f)
        else -> scheme.onSurface
    }

    Column(
        modifier = modifier.aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
                .background(bg)
                .border(borderWidth, borderColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isFuture,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected || status != null) FontWeight.Bold
                else FontWeight.Normal,
                color = fg,
            )
        }
    }
}

@Composable
private fun Legend() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = scheme.primary, label = "Completa")
        LegendItem(
            color = scheme.primary.copy(alpha = 0.15f),
            label = "Parcial",
            borderColor = scheme.primary.copy(alpha = 0.6f),
            borderWidth = 1.5.dp,
        )
        LegendItem(
            color = Color.Transparent,
            label = "Hoy",
            borderColor = scheme.primary,
            borderWidth = 1.5.dp,
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    borderColor: Color = Color.Transparent,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
                .border(borderWidth, borderColor, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Racha (stat héroe)
// ─────────────────────────────────────────────────────────────

@Composable
private fun StreakHeroCard(streak: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, primary.copy(alpha = 0.5f), MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Número gigante con decoración circular
            Box(
                modifier = Modifier.size(88.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(1.5.dp, primary.copy(alpha = 0.25f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(1.dp, primary.copy(alpha = 0.5f), CircleShape),
                )
                Text(
                    text = streak.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (streak == 1) "Semana seguida" else "Semanas seguidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (streak == 0) {
                        "Completa una rutina esta semana para empezar tu racha."
                    } else {
                        "Has completado al menos una rutina completa por semana."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Grid de stats secundarios (2x2)
// ─────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid(stats: HistoryStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = stats.monthComplete.toString(),
                label = "Completas este mes",
                sub = "${stats.monthPartial} parciales",
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary,
            )
            StatCard(
                value = stats.monthSets.toString(),
                label = "Series hechas",
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = stats.weekComplete.toString(),
                label = "Completas esta semana",
                modifier = Modifier.weight(1f),
                accent = SuccessGreen,
            )
            StatCard(
                value = (stats.monthComplete + stats.monthPartial).toString(),
                label = "Días con registro",
                modifier = Modifier.weight(1f),
                accent = AmberAccent,
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        modifier = modifier
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Detalle del día seleccionado
// ─────────────────────────────────────────────────────────────

@Composable
private fun DayDetailCard(state: HistoryUiState) {
    val title = state.selected
        .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE))
        .replaceFirstChar { it.uppercase() }

    val isFuture = state.selected.isAfter(state.today)
    val hasLogs = state.dayLogs.isNotEmpty()

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                DayDetailChip(
                    isFuture = isFuture,
                    logs = state.dayLogs.map { it.log.isComplete },
                )
            }

            if (!hasLogs) {
                EmptyDayState(isFuture = isFuture)
            } else {
                state.dayLogs.forEachIndexed { index, row ->
                    DayLogBlock(
                        programName = row.programName,
                        isComplete = row.log.isComplete,
                        doneSets = row.log.doneSets,
                        plannedSets = row.log.plannedSets,
                        doneExercises = row.log.doneExercises,
                        plannedExercises = row.log.plannedExercises,
                        exercises = state.dayExercises
                            .filter { it.programId == row.log.programId }
                            .map { Triple(it.name, it.completedSets, it.sets) },
                    )
                    if (index < state.dayLogs.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailChip(
    isFuture: Boolean,
    logs: List<Boolean>,
) {
    val (bg, fg, text) = when {
        isFuture -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "FUTURO",
        )
        logs.isEmpty() -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "SIN REGISTRO",
        )
        logs.all { it } -> Triple(
            SuccessGreen.copy(alpha = 0.18f),
            SuccessGreen,
            "COMPLETA",
        )
        else -> Triple(
            AmberAccent.copy(alpha = 0.18f),
            AmberAccent,
            "PARCIAL",
        )
    }
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

@Composable
private fun EmptyDayState(isFuture: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = if (isFuture) "Este día aún no llega."
            else "Sin entrenamiento registrado este día.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayLogBlock(
    programName: String,
    isComplete: Boolean,
    doneSets: Int,
    plannedSets: Int,
    doneExercises: Int,
    plannedExercises: Int,
    exercises: List<Triple<String, Int, Int>>,
) {
    val progress = if (plannedSets == 0) 0f else doneSets.toFloat() / plannedSets
    val accent = if (isComplete) SuccessGreen else AmberAccent

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header del programa
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isComplete) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = programName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$doneSets / $plannedSets series · $doneExercises / $plannedExercises ejercicios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Barra de progreso
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent),
            )
        }

        // Ejercicios
        if (exercises.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                exercises.forEach { (name, done, total) ->
                    ExerciseRow(name = name, done = done, total = total)
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(name: String, done: Int, total: Int) {
    val complete = done >= total
    val primary = MaterialTheme.colorScheme.primary
    val success = SuccessGreen

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (complete) success.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                .border(
                    width = 1.dp,
                    color = if (complete) success.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (complete) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (complete) success
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(11.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$done / $total",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (complete) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}