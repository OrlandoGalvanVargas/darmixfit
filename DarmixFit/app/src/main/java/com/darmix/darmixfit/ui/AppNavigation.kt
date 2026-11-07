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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarViewWeek
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.darmix.darmixfit.data.ProgramEntity
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.edit.EditRoutineScreen
import com.darmix.darmixfit.ui.history.HistoryScreen
import com.darmix.darmixfit.ui.programs.ProgramsScreen
import com.darmix.darmixfit.ui.programs.ProgramsViewModel
import com.darmix.darmixfit.ui.settings.SettingsOverlay
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.ReactorColors
import com.darmix.darmixfit.ui.theme.Spacing
import com.darmix.darmixfit.ui.today.TodayScreen
import com.darmix.darmixfit.ui.week.WeekScreen

private const val PROGRAMS_ROUTE = "programs"

private val HeaderControlHeight = 48.dp
private val DockHeight = 80.dp
private val MedallionSize = 50.dp
private val MedallionLift = (-10).dp
private val SelectorShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
private val MenuShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
private val BackShape = CutCornerShape(10.dp)

private const val GEAR_PATH =
    "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"

private val GearPath: Path by lazy {
    PathParser().parsePathString(GEAR_PATH).toPath().apply {
        addOval(Rect(Offset(12f, 12f), 3f))
    }
}

private val LitArcs = listOf(282f to 108f, 124f to 104f)

private val LightMetal = listOf(Color(0xFFFFFFFF), Color(0xFFDFE2DA), Color(0xFFBCC0B7))

private class GearColors(
    val glow: Color,
    val bezelTop: Color,
    val bezelBottom: Color,
    val bezelEdge: Color,
    val band: Color,
    val bandEdge: Color,
    val litStart: Color,
    val litEnd: Color,
    val innerLine: Color,
    val discTop: Color,
    val discMid: Color,
    val discBottom: Color,
    val glyph: Color
)

private fun gearColors(reactor: ReactorColors, colors: DarmixColors): GearColors =
    if (colors.isDark) {
        GearColors(
            glow = reactor.energy.copy(alpha = 0.05f),
            bezelTop = lerp(reactor.lens[1], reactor.steel[0], 0.35f),
            bezelBottom = reactor.lens[2],
            bezelEdge = lerp(reactor.lens[0], reactor.steel[0], 0.5f),
            band = lerp(reactor.full[0], Color.Black, 0.2f),
            bandEdge = reactor.steelEdge,
            litStart = reactor.waveBackCrest,
            litEnd = reactor.waveFront,
            innerLine = reactor.waveFront,
            discTop = lerp(reactor.lens[0], reactor.steel[0], 0.55f),
            discMid = reactor.lens[1],
            discBottom = reactor.lens[2],
            glyph = reactor.number
        )
    } else {
        GearColors(
            glow = reactor.energy.copy(alpha = 0.08f),
            bezelTop = Color.White,
            bezelBottom = Color(0xFFD3D6CE),
            bezelEdge = Color(0xFFB9BDB3),
            band = lerp(reactor.energy, Color(0xFFE9ECE4), 0.6f),
            bandEdge = Color(0xFFB9BDB3),
            litStart = reactor.energy,
            litEnd = lerp(reactor.energy, colors.voltText, 0.35f),
            innerLine = colors.voltText,
            discTop = Color.White,
            discMid = Color(0xFFEEF0EA),
            discBottom = Color(0xFFD6D9D1),
            glyph = colors.textPrimary
        )
    }

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
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onProgramsScreen = currentRoute == PROGRAMS_ROUTE
    val colors = DarmixTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onManage = { navController.navigate(PROGRAMS_ROUTE) },
                        onOpenSettings = { settingsOpen = true }
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
                composable(Destination.Today.route) { TodayScreen() }
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
        SettingsOverlay(visible = settingsOpen, onDismiss = { settingsOpen = false })
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(BackShape)
                .background(colors.canvasDeep)
                .border(1.dp, colors.hairlineStrong, BackShape)
                .clickable(role = Role.Button, onClickLabel = "Volver", onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        }
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
    onManage: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarmixTheme.colors.canvas)
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ProgramSelector(
                programs = programs,
                onSelect = onSelect,
                onManage = onManage
            )
        }
        SettingsButton(onClick = onOpenSettings)
    }
}

@Composable
private fun ProgramSelector(
    programs: List<ProgramEntity>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit
) {
    val colors = DarmixTheme.colors
    val accent = DarmixTheme.reactor.energy
    var expanded by remember { mutableStateOf(false) }
    val active = programs.firstOrNull { it.isActive }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Motion.snappy(),
        label = "selector-arrow"
    )
    val edge = if (colors.isDark) accent else colors.voltText
    val borderBrush = remember(edge, expanded) {
        val strong = if (expanded) 0.9f else 0.6f
        Brush.linearGradient(
            0f to edge.copy(alpha = strong),
            0.6f to edge.copy(alpha = 0.25f),
            1f to edge.copy(alpha = strong - 0.05f)
        )
    }
    val fill = remember(colors, accent) {
        if (colors.isDark) {
            Brush.verticalGradient(
                listOf(
                    lerp(colors.canvasDeep, accent, 0.05f).copy(alpha = 0.92f),
                    lerp(Color.Black, colors.canvasDeep, 0.4f).copy(alpha = 0.92f)
                )
            )
        } else {
            Brush.verticalGradient(listOf(colors.card, lerp(colors.card, accent, 0.06f)))
        }
    }
    val nameStyle = remember {
        TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.7.sp)
    }

    Box {
        Row(
            modifier = Modifier
                .height(HeaderControlHeight)
                .clip(SelectorShape)
                .background(fill)
                .border(1.2.dp, borderBrush, SelectorShape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 13.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "PROGRAMA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.2.sp,
                        lineHeight = 10.sp
                    ),
                    color = colors.voltText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = (active?.name ?: "Sin programa").uppercase(),
                    style = nameStyle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Cambiar programa",
                tint = colors.voltText,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MenuShape,
            containerColor = colors.cardRaised,
            border = BorderStroke(1.dp, edge.copy(alpha = 0.35f)),
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
                                .clip(CutCornerShape(3.dp))
                                .background(if (program.isActive) colors.voltText else colors.hairlineStrong)
                        )
                    },
                    onClick = {
                        expanded = false
                        if (!program.isActive) onSelect(program.id)
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
private fun SettingsButton(onClick: () -> Unit) {
    val reactor = DarmixTheme.reactor
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = Motion.snappy(),
        label = "settings-press"
    )
    val colors = DarmixTheme.colors
    val gear = remember(reactor, colors) { gearColors(reactor, colors) }

    Box(
        modifier = Modifier
            .size(HeaderControlHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = "Configuración",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val outer = size.minDimension / 2f - 0.5.dp.toPx()
            val band = 6.2.dp.toPx()
            val ring = outer - 3.3.dp.toPx() - band / 2f
            val inner = ring - band / 2f - 1.05.dp.toPx()

            drawCircle(gear.glow, radius = outer, center = c, style = Stroke(3.dp.toPx()))
            drawCircle(
                Brush.radialGradient(listOf(gear.bezelTop, gear.bezelBottom), center = Offset(c.x, c.y * 0.7f), radius = outer * 1.4f),
                radius = outer,
                center = c
            )
            drawCircle(gear.bezelEdge, radius = outer, center = c, style = Stroke(0.8.dp.toPx()))
            drawCircle(gear.band, radius = ring, center = c, style = Stroke(band))
            val litBrush = Brush.linearGradient(
                listOf(gear.litStart, gear.litEnd),
                start = Offset(c.x - ring, c.y - ring),
                end = Offset(c.x + ring, c.y + ring)
            )
            LitArcs.forEach { (start, sweep) ->
                drawArc(
                    brush = litBrush,
                    startAngle = start - 90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(c.x - ring, c.y - ring),
                    size = Size(ring * 2f, ring * 2f),
                    style = Stroke(band)
                )
            }
            drawCircle(gear.bandEdge, radius = ring + band / 2f, center = c, style = Stroke(0.8.dp.toPx()))
            drawCircle(gear.innerLine, radius = ring - band / 2f - 0.55.dp.toPx(), center = c, style = Stroke(1.3.dp.toPx()))
            drawCircle(
                Brush.radialGradient(
                    0f to gear.discTop,
                    0.6f to gear.discMid,
                    1f to gear.discBottom,
                    center = Offset(c.x, c.y - inner * 0.4f),
                    radius = inner * 1.5f
                ),
                radius = inner,
                center = c
            )

            val iconSize = 17.dp.toPx()
            val unit = iconSize / 24f
            withTransform({
                translate(c.x - iconSize / 2f, c.y - iconSize / 2f)
                scale(unit, unit, Offset.Zero)
            }) {
                drawPath(GearPath, gear.glyph, style = Stroke(width = 2.1f, join = StrokeJoin.Round, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun DockBar(
    selectedIndex: Int,
    onNavigate: (Destination) -> Unit
) {
    val colors = DarmixTheme.colors
    val barTop = if (colors.isDark) colors.card else colors.cardRaised
    val barBottom = if (colors.isDark) colors.canvasDeep else colors.card
    val shadow = if (colors.isDark) 0.45f else 0.07f
    val edge = colors.hairlineStrong
    val divider = colors.hairline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val lift = 10.dp.toPx()
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = shadow)), startY = -lift, endY = 0f),
                    topLeft = Offset(0f, -lift),
                    size = Size(size.width, lift)
                )
                drawRect(Brush.verticalGradient(listOf(barTop, barBottom)))
                drawLine(edge, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .navigationBarsPadding()
            .height(DockHeight)
            .padding(horizontal = Spacing.sm)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val step = size.width / Destination.entries.size
                    val inset = 22.dp.toPx()
                    for (i in 1 until Destination.entries.size) {
                        val x = step * i
                        drawLine(divider, Offset(x, inset), Offset(x, size.height - inset), strokeWidth = 1.dp.toPx())
                    }
                }
        ) {
            val itemWidth = maxWidth / Destination.entries.size
            val medallionX by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0) + (itemWidth - MedallionSize) / 2,
                animationSpec = Motion.slideDp(),
                label = "dock-medallion"
            )
            Row(modifier = Modifier.fillMaxSize()) {
                Destination.entries.forEachIndexed { index, destination ->
                    DockItem(
                        destination = destination,
                        selected = index == selectedIndex,
                        onClick = {
                            if (index != selectedIndex) onNavigate(destination)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            if (selectedIndex >= 0) {
                DockMedallion(
                    icon = Destination.entries[selectedIndex].icon,
                    modifier = Modifier.offset(x = medallionX, y = MedallionLift)
                )
            }
        }
    }
}

@Composable
private fun DockMedallion(icon: ImageVector, modifier: Modifier = Modifier) {
    val reactor = DarmixTheme.reactor
    val colors = DarmixTheme.colors
    val energy = reactor.energy
    val light = !colors.isDark
    val glow = energy.copy(alpha = if (light) 0.18f else 0.12f)
    val outerRing = if (light) colors.voltText else energy
    val gap = if (light) colors.card else reactor.tubeDark
    val metal = if (light) LightMetal else reactor.screw
    Box(
        modifier = modifier.size(MedallionSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2f
            drawCircle(glow, radius = r + 6.dp.toPx(), center = c, style = Stroke(5.dp.toPx()))
            drawCircle(outerRing, radius = r + 3.75.dp.toPx(), center = c, style = Stroke(1.5.dp.toPx()))
            drawCircle(gap, radius = r + 1.5.dp.toPx(), center = c, style = Stroke(3.dp.toPx()))
            drawCircle(
                Brush.radialGradient(
                    0f to metal[0],
                    0.55f to metal[1],
                    1f to metal[2],
                    center = Offset(size.width * 0.38f, size.height * 0.35f),
                    radius = r * 1.3f
                ),
                radius = r,
                center = c
            )
            drawCircle(energy, radius = 18.dp.toPx(), center = c)
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onVolt,
            modifier = Modifier.size(19.dp)
        )
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
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.voltText else colors.textTertiary,
        animationSpec = tween(Motion.Standard),
        label = "dock-label"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 0f else 1f,
        animationSpec = tween(Motion.Quick),
        label = "dock-icon"
    )
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = colors.textSecondary,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { alpha = iconAlpha }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = destination.label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
            ),
            color = labelColor,
            maxLines = 1
        )
    }
}
