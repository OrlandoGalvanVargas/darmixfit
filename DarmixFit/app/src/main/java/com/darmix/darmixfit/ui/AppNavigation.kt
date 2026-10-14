package com.darmix.darmixfit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.ui.edit.EditRoutineScreen
import com.darmix.darmixfit.ui.history.HistoryScreen
import com.darmix.darmixfit.ui.programs.ProgramsScreen
import com.darmix.darmixfit.ui.programs.ProgramsViewModel
import com.darmix.darmixfit.ui.today.TodayScreen
import com.darmix.darmixfit.ui.week.WeekScreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import com.darmix.darmixfit.ui.theme.LocalThemeToggle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PROGRAMS_ROUTE = "programs"

private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Today("today", "Hoy", Icons.Default.Home),
    Week("week", "Semana", Icons.Default.DateRange),
    History("history", "Historial", Icons.Default.CheckCircle),
    Edit("edit", "Rutina", Icons.Default.Edit),
}

@Composable
fun FitFlowNavHost() {
    val navController = rememberNavController()
    val programsViewModel: ProgramsViewModel = viewModel(factory = ProgramsViewModel.Factory)
    val programs by programsViewModel.programs.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onProgramsScreen = currentRoute == PROGRAMS_ROUTE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (onProgramsScreen) {
                FitFlowTopBar(
                    title = "Programas",
                    onBack = { navController.popBackStack() },
                )
            } else {
                FitFlowProgramHeader(
                    programs = programs,
                    onSelect = programsViewModel::activate,
                    onManage = { navController.navigate(PROGRAMS_ROUTE) },
                )
            }
        },
        bottomBar = {
            FitFlowBottomBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(160)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(160)) },
        ) {
            composable(Destination.Today.route) {
                TodayScreen(onSwitchProgram = { navController.navigate(PROGRAMS_ROUTE) })
            }
            composable(Destination.Week.route) { WeekScreen() }
            composable(Destination.History.route) { HistoryScreen() }
            composable(Destination.Edit.route) { EditRoutineScreen() }

            composable(
                route = PROGRAMS_ROUTE,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300),
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 5 },
                        animationSpec = tween(300),
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 5 },
                        animationSpec = tween(300),
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300),
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                ProgramsScreen(viewModel = programsViewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Top bar — título + botón de volver (solo en "Programas")
// ─────────────────────────────────────────────────────────────

@Composable
private fun FitFlowTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Header con selector de programa — chip redondeado con punto
// ─────────────────────────────────────────────────────────────

@Composable
private fun FitFlowProgramHeader(
    programs: List<ProgramEntity>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProgramSelector(
            programs = programs,
            onSelect = onSelect,
            onManage = onManage,
            modifier = Modifier.weight(1f),
        )
        ThemeToggleButton()
    }
}

@Composable
private fun ProgramSelector(
    programs: List<ProgramEntity>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,   // ← nuevo
) {
    var expanded by remember { mutableStateOf(false) }
    val active = programs.firstOrNull { it.isActive }

    Box(modifier = modifier) {   // ← pasar modifier al Box
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Punto lima indicando "programa activo"
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Text(
                text = active?.name ?: "Sin programa",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Cambiar programa",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            programs.forEach { program ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = program.name,
                            color = if (program.isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (program.isActive) FontWeight.SemiBold
                            else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (!program.isActive) onSelect(program.id)
                    },
                    trailingIcon = {
                        if (program.isActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Activo",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
            DropdownMenuItem(
                text = { Text("Administrar programas…") },
                onClick = {
                    expanded = false
                    onManage()
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Bottom bar custom con pill de acento detrás del icono activo
// ─────────────────────────────────────────────────────────────

@Composable
private fun FitFlowBottomBar(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.forEach { destination ->
                FitFlowNavItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FitFlowNavItem(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "nav-icon-color",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "nav-label-color",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 220),
        label = "nav-pill-color",
    )
    // Pequeño "pop" del icono activo.
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "nav-icon-scale",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!selected) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}

@Composable
private fun ThemeToggleButton() {
    val toggle = LocalThemeToggle.current
    val haptics = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()  // ← se usa solo para dibujar el ícono "actual"
    val actualDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = scheme.outline.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                toggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        ThemeToggleIcon(
            isDark = actualDark,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ThemeToggleIcon(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurface
    val stroke = 2.dp

    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension / 2f

        if (isDark) {
            // ── Luna creciente ──
            val outer = Path().apply {
                addOval(Rect(center = c, radius = r * 0.85f))
            }
            val inner = Path().apply {
                addOval(
                    Rect(
                        center = c + Offset(r * 0.55f, -r * 0.35f),
                        radius = r * 0.82f,
                    )
                )
            }
            val crescent = Path().apply {
                op(outer, inner, PathOperation.Difference)
            }
            drawPath(path = crescent, color = color)
        } else {
            // ── Sol con 8 rayos ──
            drawCircle(
                color = color,
                radius = r * 0.42f,
                center = c,
            )
            val rayStart = r * 0.62f
            val rayEnd = r * 0.92f
            for (i in 0 until 8) {
                val angle = i * (PI / 4.0)
                val start = Offset(
                    x = c.x + (cos(angle) * rayStart).toFloat(),
                    y = c.y + (sin(angle) * rayStart).toFloat(),
                )
                val end = Offset(
                    x = c.x + (cos(angle) * rayEnd).toFloat(),
                    y = c.y + (sin(angle) * rayEnd).toFloat(),
                )
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = stroke.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}