package com.darmix.darmixfit.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.ExerciseEntity
import com.darmix.darmixfit.ui.components.ExerciseImage
import com.darmix.darmixfit.ui.components.FullscreenImageDialog
import com.darmix.darmixfit.ui.theme.SuccessGreen
import com.darmix.darmixfit.ui.timer.RestTimerDialog
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
    onSwitchProgram: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val restVisible by viewModel.restVisible.collectAsStateWithLifecycle()
    val timer by viewModel.timerState.collectAsStateWithLifecycle()
    val restDefault by viewModel.restDefaultSeconds.collectAsStateWithLifecycle()
    val allComplete by viewModel.allDayComplete.collectAsStateWithLifecycle()

    var enlarged by remember { mutableStateOf<ExerciseEntity?>(null) }

    // "Ocultado" NO persiste: es memoria normal de Compose, no rememberSaveable.
    // Se reinicia solo al salir de esta pantalla y volver (otra pestaña, o cerrar y
    // abrir la app), y también en cuanto el día deja de estar completo.
    var dismissed by remember { mutableStateOf(false) }
    LaunchedEffect(allComplete) {
        if (!allComplete) dismissed = false
    }
    val showSessionComplete = allComplete && !dismissed

    // Si la app estuvo abierta durante la noche, al volver se actualiza el día.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDate() }

    val dayName = state.date.dayOfWeek
        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-MX"))
        .replaceFirstChar { it.uppercase() }
    val dayNumber = remember(state.date) {
        state.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX")))
    }

    val doneCount = state.items.count { isItemFinished(it) }
    val totalCount = state.items.size
    val progressFraction = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(450),
        label = "today-progress",
    )
    val activeIndex = state.items.indexOfFirst { !isItemFinished(it) }

    // Box en vez de LazyColumn directo: así el aviso se superpone SOLO a esta pantalla
    // (no es un Dialog de sistema), y la barra inferior de navegación queda libre.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = dayNumber.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = dayName, style = MaterialTheme.typography.headlineMedium)
                    state.planTitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!state.loading && state.items.isNotEmpty()) {
                item(key = "progress") {
                    TodayProgressCard(done = doneCount, total = totalCount, progress = animatedProgress)
                }
            }

            when {
                state.loading -> item(key = "loading") {
                    Text(
                        text = "Cargando rutina…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.items.isEmpty() -> item(key = "rest-day") {
                    RestDayCard()
                }

                else -> itemsIndexed(
                    items = state.items,
                    key = { _, item -> item.key },
                ) { index, item ->
                    val status = when {
                        isItemFinished(item) -> CardStatus.DONE
                        index == activeIndex -> CardStatus.ACTIVE
                        else -> CardStatus.UPCOMING
                    }
                    when (item) {
                        is TodayItem.Single -> ExerciseCard(
                            entry = item.entry,
                            status = status,
                            onComplete = { viewModel.completeSet(item.entry.exercise) },
                            onUndo = { viewModel.undoSet(item.entry.exercise) },
                            onImageClick = { enlarged = it },
                        )

                        is TodayItem.Superset -> SupersetCard(
                            item = item,
                            status = status,
                            onComplete = { viewModel.completeRound(item.members.map { it.exercise }) },
                            onUndo = { viewModel.undoRound(item.members.map { it.exercise }) },
                            onImageClick = { enlarged = it },
                        )
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(8.dp)) }
        }

        AnimatedVisibility(
            visible = showSessionComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SessionCompleteOverlay(
                onStay = { dismissed = true },
                onSwitchProgram = {
                    dismissed = true
                    onSwitchProgram()
                },
            )
        }
    }

    if (restVisible) {
        RestTimerDialog(
            timer = timer,
            initialSeconds = restDefault,
            onStart = viewModel::startRest,
            onCancelRunning = viewModel::cancelRest,
            onClose = viewModel::dismissRest,
        )
    }

    enlarged?.let { exercise ->
        exercise.imageUri?.let { image ->
            FullscreenImageDialog(
                imageName = image,
                title = exercise.name,
                onDismiss = { enlarged = null },
            )
        }
    }
}

private fun isItemFinished(item: TodayItem): Boolean = when (item) {
    is TodayItem.Single -> item.entry.completed >= item.entry.exercise.sets
    is TodayItem.Superset -> item.completedRounds >= item.totalRounds
}

private enum class CardStatus { DONE, ACTIVE, UPCOMING }

@Composable
private fun TodayProgressCard(done: Int, total: Int, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Progreso de hoy",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$done/$total ejercicios",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun RestDayCard() {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    // Respiración lenta: la card se siente viva sin distraer.
    val infinite = rememberInfiniteTransition(label = "rest-breath")
    val glowScale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rest-glow-scale",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, outline.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Luna creciente + anillos + glow que respira ──
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Halo ambiental
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.22f),
                                    primary.copy(alpha = 0.06f),
                                    Color.Transparent,
                                )
                            ),
                            shape = CircleShape,
                        )
                )

                Canvas(modifier = Modifier.size(112.dp)) {
                    val c = center
                    val outerR = size.minDimension / 2f
                    val midR = outerR * 0.78f
                    val crescentR = outerR * 0.42f

                    // Anillos concéntricos
                    drawCircle(
                        color = primary.copy(alpha = 0.22f),
                        radius = outerR,
                        center = c,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.40f),
                        radius = midR,
                        center = c,
                        style = Stroke(width = 1.dp.toPx()),
                    )

                    // Creciente: círculo grande menos círculo desplazado arriba-derecha
                    val outer = Path().apply {
                        addOval(Rect(center = c, radius = crescentR))
                    }
                    val inner = Path().apply {
                        addOval(
                            Rect(
                                center = c + Offset(crescentR * 0.62f, -crescentR * 0.38f),
                                radius = crescentR * 0.98f,
                            )
                        )
                    }
                    val crescent = Path().apply {
                        op(outer, inner, PathOperation.Difference)
                    }
                    drawPath(path = crescent, color = primary)
                }
            }

            // Chip de categoría con borde sutil
            Box(
                modifier = Modifier
                    .background(
                        color = primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50),
                    )
                    .border(
                        width = 1.dp,
                        color = primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "RECUPERACIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                )
            }

            // Titular editorial en dos líneas
            Text(
                text = "Hoy entrenas\ndescansando",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = onSurface,
            )

            // Cuerpo
            Text(
                text = "El músculo no crece cuando levantas,\ncrece cuando sueltas.",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Divisor fino
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(48.dp)
                    .height(1.dp)
                    .background(outline.copy(alpha = 0.6f)),
            )

            // Micro-línea final
            Text(
                text = "VUELVE MAÑANA AL 100%",
                style = MaterialTheme.typography.labelLarge,
                color = primary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Aviso de rutina completada, dibujado DENTRO de esta pantalla (no como Dialog de sistema).
 * Por eso solo tapa el área de Hoy y la barra de navegación inferior sigue funcionando.
 * No se cierra al tocar fuera de la tarjeta: el fondo consume el toque sin hacer nada.
 */
@Composable
private fun SessionCompleteOverlay(
    onStay: () -> Unit,
    onSwitchProgram: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Scale-in de entrada (la opacidad ya la maneja AnimatedVisibility del padre).
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.88f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "complete-card-scale",
    )

    // Anillo expansivo continuo (efecto "radar").
    val infinite = rememberInfiniteTransition(label = "complete-rings")
    val ringPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring-phase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Check + anillos expansivos ──
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Glow radial detrás
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primary.copy(alpha = 0.32f),
                                        primary.copy(alpha = 0.08f),
                                        Color.Transparent,
                                    )
                                ),
                                shape = CircleShape,
                            )
                    )

                    // Anillos + check dibujados
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val c = center
                        val maxR = size.minDimension / 2f

                        // Anillo expansivo animado (radar)
                        val r = maxR * ringPhase
                        val alpha = (1f - ringPhase) * 0.55f
                        drawCircle(
                            color = primary.copy(alpha = alpha),
                            radius = r,
                            center = c,
                            style = Stroke(width = 2.dp.toPx()),
                        )

                        // Anillos estáticos concéntricos
                        drawCircle(
                            color = primary.copy(alpha = 0.18f),
                            radius = maxR * 0.92f,
                            center = c,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        drawCircle(
                            color = primary.copy(alpha = 0.34f),
                            radius = maxR * 0.76f,
                            center = c,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }

                    // Disco lima con check dibujado
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(36.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.18f, size.height * 0.52f)
                                lineTo(size.width * 0.42f, size.height * 0.76f)
                                lineTo(size.width * 0.84f, size.height * 0.26f)
                            }
                            drawPath(
                                path = path,
                                color = onPrimary,
                                style = Stroke(
                                    width = 4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                        }
                    }
                }

                // Chip de categoría
                Box(
                    modifier = Modifier
                        .background(
                            color = primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50),
                        )
                        .border(
                            width = 1.dp,
                            color = primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "SESIÓN COMPLETA",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                    )
                }

                // Titular
                Text(
                    text = "¡Rutina\ncompletada!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = onSurface,
                )

                // Cuerpo
                Text(
                    text = "Terminaste todas las series de hoy.\nAhora toca recuperar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // CTA principal
                Button(
                    onClick = onStay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = onPrimary,
                    ),
                ) {
                    Text(
                        text = "Seguir aquí",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // CTA secundario
                TextButton(
                    onClick = onSwitchProgram,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Cambiar de programa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    entry: ExerciseProgress,
    status: CardStatus,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit,
) {
    val exercise = entry.exercise
    StatusCardShell(status = status) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip(status = status)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseImage(
                    imageName = exercise.imageUri,
                    exerciseName = exercise.name,
                    size = if (status == CardStatus.ACTIVE) 76.dp else 56.dp,
                    onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = exercise.reps,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (status == CardStatus.ACTIVE && exercise.note.isNotBlank()) {
                TipBox(text = exercise.note)
            }
            if (status != CardStatus.UPCOMING || entry.completed > 0) {
                SetDots(total = exercise.sets, completed = entry.completed, status = status)
            }
            ActionRow(
                label = "Serie ${entry.completed} de ${exercise.sets}",
                finished = entry.completed >= exercise.sets,
                canUndo = entry.completed > 0,
                actionLabel = "Completar serie",
                emphasized = status == CardStatus.ACTIVE,
                onComplete = onComplete,
                onUndo = onUndo,
            )
        }
    }
}

@Composable
private fun SupersetCard(
    item: TodayItem.Superset,
    status: CardStatus,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
    onImageClick: (ExerciseEntity) -> Unit,
) {
    StatusCardShell(status = status) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SUPERSERIE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                StatusChip(status = status)
            }
            item.members.forEachIndexed { index, member ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                val exercise = member.exercise
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExerciseImage(
                        imageName = exercise.imageUri,
                        exerciseName = exercise.name,
                        size = 56.dp,
                        onClick = if (exercise.imageUri != null) ({ onImageClick(exercise) }) else null,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = exercise.reps,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (exercise.note.isNotBlank()) {
                            Text(
                                text = exercise.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            SetDots(total = item.totalRounds, completed = item.completedRounds, status = status)
            ActionRow(
                label = "Ronda ${item.completedRounds} de ${item.totalRounds}",
                finished = item.completedRounds >= item.totalRounds,
                canUndo = item.completedRounds > 0,
                actionLabel = "Completar ronda",
                emphasized = status == CardStatus.ACTIVE,
                onComplete = onComplete,
                onUndo = onUndo,
            )
        }
    }
}

/** Card base: borde/realce distinto según si el ejercicio está activo, próximo o completado. */
@Composable
private fun StatusCardShell(
    status: CardStatus,
    content: @Composable () -> Unit,
) {
    val containerColor = when (status) {
        CardStatus.ACTIVE -> MaterialTheme.colorScheme.surfaceContainerHigh
        CardStatus.DONE -> MaterialTheme.colorScheme.surfaceContainer
        CardStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = when (status) {
        CardStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val contentAlpha = if (status == CardStatus.UPCOMING) 0.7f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .border(
                width = if (status == CardStatus.ACTIVE) 1.5.dp else 1.dp,
                color = borderColor.copy(alpha = if (status == CardStatus.ACTIVE) 1f else 0.5f),
                shape = MaterialTheme.shapes.large,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (status == CardStatus.ACTIVE) 4.dp else 0.dp,
        ),
    ) { content() }
}

@Composable
private fun StatusChip(status: CardStatus) {
    val (label, bg, fg) = when (status) {
        CardStatus.ACTIVE -> Triple(
            "ACTIVO AHORA",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        CardStatus.DONE -> Triple(
            "COMPLETADO",
            SuccessGreen.copy(alpha = 0.18f),
            SuccessGreen,
        )
        CardStatus.UPCOMING -> Triple(
            "A CONTINUACIÓN",
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
private fun TipBox(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Text(text = "💡", style = MaterialTheme.typography.bodyMedium)
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.onSecondaryContainer, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SetDots(total: Int, completed: Int, status: CardStatus) {
    if (total <= 0) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val isDone = index < completed
            val isCurrent = index == completed && status == CardStatus.ACTIVE
            val color = when {
                isDone -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 10.dp)
                    .background(color, CircleShape)
                    .then(
                        if (isCurrent) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    finished: Boolean,
    canUndo: Boolean,
    actionLabel: String,
    emphasized: Boolean,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (finished) "$label ✓" else label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (finished) SuccessGreen else MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors( // ✅ Usa ButtonDefaults en lugar de OutlinedButtonDefaults
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.width(56.dp),
            ) { Text("−", fontWeight = FontWeight.Bold) }

            Button(
                onClick = onComplete,
                enabled = !finished,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (emphasized) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = SuccessGreen.copy(alpha = 0.18f),
                    disabledContentColor = SuccessGreen,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text(
                    text = if (finished) "Completado" else actionLabel,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}