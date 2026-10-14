package com.darmix.darmixfit.ui.week

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekScreen(
    modifier: Modifier = Modifier,
    viewModel: WeekViewModel = viewModel(factory = WeekViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var todayDow by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        todayDow = LocalDate.now().dayOfWeek.value
    }

    // Orden estable por día (presentación, no toca datos).
    val sortedDays = remember(state.days) {
        state.days.sortedBy { it.dayOfWeek }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var positioned by rememberSaveable { mutableStateOf(false) }

    // Header (item 0) + WeekStrip (item 1) → los días empiezan en index 2.
    val firstDayIndex = 2

    LaunchedEffect(sortedDays.size) {
        if (!positioned && sortedDays.isNotEmpty()) {
            val index = sortedDays.indexOfFirst { it.dayOfWeek == todayDow }
            if (index >= 0) listState.scrollToItem(index + firstDayIndex)
            positioned = true
        }
    }

    if (state.loading) {
        Text(
            text = "Cargando rutina…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp),
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            WeekHeader(dayCount = sortedDays.size)
        }

        item(key = "strip") {
            WeekStrip(
                days = sortedDays,
                todayDow = todayDow,
                onDayClick = { dow ->
                    val index = sortedDays.indexOfFirst { it.dayOfWeek == dow }
                    if (index >= 0) {
                        scope.launch {
                            listState.animateScrollToItem(index + firstDayIndex)
                        }
                    }
                },
            )
        }

        items(sortedDays, key = { it.dayOfWeek }) { day ->
            DayCard(
                day = day,
                todayDow = todayDow,
                isToday = day.dayOfWeek == todayDow,
            )
        }

        item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeekHeader(dayCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Semana",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "$dayCount días",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "Toca un día para saltar a su rutina.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Week strip — 7 celdas compactas con dot indicador
// ─────────────────────────────────────────────────────────────

private val ShortDayNames = listOf("L", "M", "M", "J", "V", "S", "D")

@Composable
private fun WeekStrip(
    days: List<WeekDay>,
    todayDow: Int,
    onDayClick: (Int) -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .border(1.dp, outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (dow in 1..7) {
            val day = days.firstOrNull { it.dayOfWeek == dow }
            val hasExercises = day?.entries?.isNotEmpty() == true
            val isToday = dow == todayDow

            WeekDayCell(
                shortLabel = ShortDayNames[dow - 1],
                hasExercises = hasExercises,
                isToday = isToday,
                onClick = { onDayClick(dow) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    shortLabel: String,
    hasExercises: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = shortLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isToday) primary else onSurfaceVariant,
        )

        // Dot: lleno si entrena, hueco si descansa; anillo si es hoy.
        Box(
            modifier = Modifier
                .size(10.dp)
                .then(
                    if (isToday) {
                        Modifier
                            .border(2.dp, primary, CircleShape)
                            .padding(2.dp)
                            .background(
                                if (hasExercises) primary else primary.copy(alpha = 0.3f),
                                CircleShape,
                            )
                    } else if (hasExercises) {
                        Modifier.background(primary, CircleShape)
                    } else {
                        Modifier
                            .border(1.dp, outline.copy(alpha = 0.8f), CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    }
                ),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Day card
// ─────────────────────────────────────────────────────────────

@Composable
private fun DayCard(
    day: WeekDay,
    todayDow: Int,
    isToday: Boolean,
) {
    val dayName = DayOfWeek.of(day.dayOfWeek)
        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-MX"))
        .replaceFirstChar { it.uppercase() }

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val isRest = day.entries.isEmpty()
    val isPast = day.dayOfWeek < todayDow

    // Contenedor, borde y elevación según estado.
    val containerColor = when {
        isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = when {
        isToday -> primary
        isRest -> outline.copy(alpha = 0.3f)
        else -> outline.copy(alpha = 0.4f)
    }
    val borderWidth = if (isToday) 1.5.dp else 1.dp
    val elevation = if (isToday) 4.dp else 0.dp
    val contentAlpha = if (isPast && !isToday) 0.72f else 1f

    val exerciseCount = day.entries.sumOf { if (it.size == 1) 1 else it.size }
    val blockCount = day.entries.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .border(borderWidth, borderColor, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Fila superior: nombre + chip de estado ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                DayStatusChip(
                    isToday = isToday,
                    isRest = isRest,
                    blockCount = blockCount,
                    exerciseCount = exerciseCount,
                )
            }

            // ── Título del plan ──
            Text(
                text = day.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )

            // ── Contenido ──
            if (isRest) {
                RestDayMini()
            } else {
                Column(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    day.entries.forEachIndexed { index, entry ->
                        if (entry.size == 1) {
                            SingleRow(entry.first())
                        } else {
                            SupersetBlock(entry)
                        }
                        if (index < day.entries.lastIndex) {
                            BulletDivider()
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Chips
// ─────────────────────────────────────────────────────────────

@Composable
private fun DayStatusChip(
    isToday: Boolean,
    isRest: Boolean,
    blockCount: Int,
    exerciseCount: Int,
) {
    val (bg, fg, text) = when {
        isToday -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "HOY",
        )
        isRest -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "DESCANSO",
        )
        else -> {
            val count = if (blockCount == exerciseCount) {
                "$exerciseCount EJERCICIOS"
            } else {
                "$blockCount BLOQUES"
            }
            Triple(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.primary,
                count,
            )
        }
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

// ─────────────────────────────────────────────────────────────
// Filas / bloques
// ─────────────────────────────────────────────────────────────

@Composable
private fun SingleRow(exercise: ExerciseEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Punto de acento
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${exercise.sets} series · ${exercise.reps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SupersetBlock(members: List<ExerciseEntity>) {
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
            )
            .border(
                width = 1.dp,
                color = secondary.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
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
                text = "· ${members.minOf { it.sets }} rondas",
                style = MaterialTheme.typography.labelMedium,
                color = secondary.copy(alpha = 0.7f),
            )
        }
        members.forEach { exercise ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "↔",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = exercise.reps,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    CircleShape,
                ),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Descanso mini — dialoga con RestDayCard de Hoy
// ─────────────────────────────────────────────────────────────

@Composable
private fun RestDayMini() {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = primary.copy(alpha = 0.06f),
                shape = MaterialTheme.shapes.medium,
            )
            .border(
                width = 1.dp,
                color = primary.copy(alpha = 0.18f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(primary.copy(alpha = 0.14f), CircleShape)
                .border(1.dp, primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🌙",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Día de descanso",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Recuperación activa.",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant,
            )
        }
    }
}