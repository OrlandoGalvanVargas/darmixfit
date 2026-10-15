package com.darmix.darmixfit.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.ui.components.DarmixIconButton
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.edit.EditRoutineScreen
import com.darmix.darmixfit.ui.history.HistoryScreen
import com.darmix.darmixfit.ui.programs.ProgramsScreen
import com.darmix.darmixfit.ui.programs.ProgramsViewModel
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.LocalThemeToggle
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import com.darmix.darmixfit.ui.today.TodayScreen
import com.darmix.darmixfit.ui.week.WeekScreen
import kotlin.math.cos
import kotlin.math.sin

private const val PROGRAMS_ROUTE = "programs"

private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Today("today", "Hoy", Icons.Rounded.FitnessCenter),
    Week("week", "Semana", Icons.Rounded.CalendarViewWeek),
    History("history", "Historial", Icons.Rounded.Insights),
    Edit("edit", "Rutina", Icons.Rounded.EditNote);

    companion object {
        fun indexOf(route: String?): Int = entries.indexOfFirst { it.route == route }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = Destination.indexOf(initialState.destination.route)
    val to = Destination.indexOf(targetState.destination.route)
    return if (from < 0 || to < 0 || from == to) 0 else if (to > from) 1 else -1
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition {
    val direction = tabDirection()
    return fadeIn(tween(Motion.Standard, easing = Motion.Decelerate)) +
            slideInHorizontally(tween(Motion.Standard, easing = Motion.Decelerate)) { direction * it / 12 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition {
    val direction = tabDirection()
    return fadeOut(tween(Motion.Quick, easing = Motion.Accelerate)) +
            slideOutHorizontally(tween(Motion.Quick, easing = Motion.Accelerate)) { -direction * it / 16 }
}

@Composable
fun DarmixNavHost() {
    val navController = rememberNavController()
    val programsViewModel: ProgramsViewModel = viewModel(factory = ProgramsViewModel.Factory)
    val programs by programsViewModel.programs.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onProgramsScreen = currentRoute == PROGRAMS_ROUTE
    val colors = DarmixTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.canvas,
        topBar = {
            if (onProgramsScreen) {
                SectionTopBar(
                    overline = "Administrar",
                    title = "PROGRAMAS",
                    onBack = { navController.popBackStack() }
                )
            } else {
                ProgramHeader(
                    programs = programs,
                    onSelect = programsViewModel::activate,
                    onManage = { navController.navigate(PROGRAMS_ROUTE) }
                )
            }
        },
        bottomBar = {
            if (!onProgramsScreen) {
                DockBar(
                    selectedIndex = Destination.indexOf(currentRoute),
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { tabEnter() },
            exitTransition = { tabExit() },
            popEnterTransition = { tabEnter() },
            popExitTransition = { tabExit() }
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
                    slideInHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { it } +
                            fadeIn(tween(Motion.Standard))
                },
                exitTransition = {
                    slideOutHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { -it / 5 } +
                            fadeOut(tween(Motion.Standard))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { -it / 5 } +
                            fadeIn(tween(Motion.Standard))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { it } +
                            fadeOut(tween(Motion.Standard))
                }
            ) {
                ProgramsScreen(viewModel = programsViewModel)
            }
        }
    }
}

@Composable
private fun SectionTopBar(
    overline: String,
    title: String,
    onBack: () -> Unit
) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .statusBarsPadding()
            .padding(start = Spacing.md, end = Spacing.screen, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        DarmixIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Volver",
            onClick = onBack,
            size = 44.dp,
            shape = CircleShape
        )
        Column {
            Overline(text = overline, color = colors.voltText)
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        }
    }
}

@Composable
private fun ProgramHeader(
    programs: List<ProgramEntity>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarmixTheme.colors.canvas)
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
    ) {
        ProgramSelector(
            programs = programs,
            onSelect = onSelect,
            onManage = onManage,
            modifier = Modifier.weight(1f)
        )
        ThemeToggleButton()
    }
}

@Composable
private fun ProgramSelector(
    programs: List<ProgramEntity>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val active = programs.firstOrNull { it.isActive }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Motion.snappy(),
        label = "selector-arrow"
    )

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(DarmixShapes.Pill)
                .background(colors.card)
                .border(1.dp, if (expanded) colors.voltText.copy(alpha = 0.6f) else colors.hairline, DarmixShapes.Pill)
                .clickable(role = Role.DropdownList) {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    expanded = true
                }
                .padding(start = 6.dp, end = Spacing.md, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.volt),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FitnessCenter,
                    contentDescription = null,
                    tint = colors.onVolt,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Overline(text = "Programa", color = colors.textTertiary)
                Text(
                    text = active?.name ?: "Sin programa",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Cambiar programa",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = DarmixShapes.Card,
            containerColor = colors.cardRaised,
            border = BorderStroke(1.dp, colors.hairline),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            Overline(
                text = "Cambiar programa",
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            )
            programs.forEach { program ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = program.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (program.isActive) colors.voltText else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (program.isActive) colors.voltText else colors.hairlineStrong)
                        )
                    },
                    trailingIcon = {
                        if (program.isActive) {
                            Icon(Icons.Rounded.Check, contentDescription = "Activo", tint = colors.voltText)
                        }
                    },
                    onClick = {
                        expanded = false
                        if (!program.isActive) {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelect(program.id)
                        }
                    }
                )
            }
            HorizontalDivider(
                color = colors.hairline,
                modifier = Modifier.padding(vertical = Spacing.xs)
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Administrar programas",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.textSecondary)
                },
                onClick = {
                    expanded = false
                    onManage()
                }
            )
        }
    }
}

@Composable
private fun DockBar(
    selectedIndex: Int,
    onNavigate: (Destination) -> Unit
) {
    val colors = DarmixTheme.colors
    val haptic = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .navigationBarsPadding()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xs, bottom = Spacing.sm)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(DarmixShapes.Hero)
                .background(colors.card)
                .border(1.dp, colors.hairline, DarmixShapes.Hero)
                .padding(6.dp)
        ) {
            val itemWidth = maxWidth / Destination.entries.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0),
                animationSpec = Motion.slideDp(),
                label = "dock-indicator"
            )
            if (selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(DarmixShapes.Card)
                        .background(accent)
                )
            }
            Row(modifier = Modifier.fillMaxSize()) {
                Destination.entries.forEachIndexed { index, destination ->
                    DockItem(
                        destination = destination,
                        selected = index == selectedIndex,
                        onClick = {
                            if (index != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onNavigate(destination)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
        animationSpec = tween(Motion.Standard),
        label = "dock-content"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = Motion.bouncy(),
        label = "dock-scale"
    )
    Column(
        modifier = modifier
            .clip(DarmixShapes.Card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = content,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun ThemeToggleButton() {
    val colors = DarmixTheme.colors
    val toggle = LocalThemeToggle.current
    val haptic = LocalHapticFeedback.current
    val night by animateFloatAsState(
        targetValue = if (colors.isDark) 1f else 0f,
        animationSpec = tween(Motion.Slow, easing = Motion.Emphasized),
        label = "theme-morph"
    )
    val iconColor = colors.textPrimary

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                toggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val n = night
            val c = center
            val r = size.minDimension / 2f
            rotate(degrees = -40f * n, pivot = c) {
                val bodyRadius = r * (0.42f + 0.40f * n)
                val body = Path().apply { addOval(Rect(c, bodyRadius)) }
                val bite = Path().apply {
                    val travel = 1f - n
                    addOval(
                        Rect(
                            c + Offset(r * (0.55f + travel * 1.2f), -r * (0.35f + travel * 0.8f)),
                            r * 0.80f
                        )
                    )
                }
                drawPath(Path().apply { op(body, bite, PathOperation.Difference) }, iconColor)
            }
            val rayAlpha = (1f - n * 1.6f).coerceIn(0f, 1f)
            if (rayAlpha > 0f) {
                val start = r * (0.62f + 0.12f * n)
                val end = r * (0.95f - 0.15f * n)
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4.0 + n * 0.6
                    val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(
                        color = iconColor.copy(alpha = rayAlpha),
                        start = c + dir * start,
                        end = c + dir * end,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
