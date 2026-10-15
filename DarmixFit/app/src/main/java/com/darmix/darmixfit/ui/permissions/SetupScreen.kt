package com.darmix.darmixfit.ui.permissions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.components.StatusTag
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing

private enum class StepKey { NOTIFICATIONS, EXACT_ALARMS, BATTERY, XIAOMI }

private data class SetupStepUi(
    val key: StepKey,
    val title: String,
    val why: String,
    val icon: ImageVector,
    val done: Boolean,
    val required: Boolean
)

private val RailWidth = 36.dp
private val NodeY = 30.dp

private fun stepsFor(status: SetupStatus): List<SetupStepUi> = buildList {
    add(
        SetupStepUi(
            key = StepKey.NOTIFICATIONS,
            title = "Notificaciones",
            why = "Para ver la cuenta regresiva y el aviso de «Descanso terminado» aunque estés en otra app.",
            icon = Icons.Rounded.NotificationsActive,
            done = status.notificationsOk,
            required = true
        )
    )
    if (status.exactAlarmsRelevant) {
        add(
            SetupStepUi(
                key = StepKey.EXACT_ALARMS,
                title = "Alarmas exactas",
                why = "Hace que el aviso llegue justo en 0:00, incluso con el teléfono en reposo.",
                icon = Icons.Rounded.Alarm,
                done = status.exactAlarmsOk,
                required = true
            )
        )
    }
    add(
        SetupStepUi(
            key = StepKey.BATTERY,
            title = "Batería sin restricciones",
            why = "Evita que Android pause el temporizador para ahorrar energía mientras descansas.",
            icon = Icons.Rounded.BatteryChargingFull,
            done = status.batteryOk,
            required = false
        )
    )
    if (status.xiaomiRequired) {
        add(
            SetupStepUi(
                key = StepKey.XIAOMI,
                title = "Inicio automático",
                why = "Tu teléfono Xiaomi cierra las apps en segundo plano aunque tengan permisos. Sin esto, el descanso podría no sonar con la pantalla bloqueada.",
                icon = Icons.Rounded.RocketLaunch,
                done = status.xiaomiOk,
                required = false
            )
        )
    }
}

@Composable
fun SetupScreen(
    status: SetupStatus,
    onNotifications: () -> Unit,
    onExactAlarms: () -> Unit,
    onBattery: () -> Unit,
    autostartFallback: Boolean,
    onOpenAutostart: () -> Unit,
    onConfirmXiaomi: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = DarmixTheme.colors
    AnimatedContent(
        targetState = status.allOk,
        transitionSpec = { (fadeIn(Motion.enter()) + scaleIn(initialScale = 0.94f)) togetherWith fadeOut(Motion.exit()) },
        label = "setup-state",
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) { allOk ->
        if (allOk) {
            AllSetCelebration()
        } else {
            SetupChecklist(
                status = status,
                onNotifications = onNotifications,
                onExactAlarms = onExactAlarms,
                onBattery = onBattery,
                autostartFallback = autostartFallback,
                onOpenAutostart = onOpenAutostart,
                onConfirmXiaomi = onConfirmXiaomi,
                onSkip = onSkip
            )
        }
    }
}

@Composable
private fun SetupChecklist(
    status: SetupStatus,
    onNotifications: () -> Unit,
    onExactAlarms: () -> Unit,
    onBattery: () -> Unit,
    autostartFallback: Boolean,
    onOpenAutostart: () -> Unit,
    onConfirmXiaomi: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = DarmixTheme.colors
    val steps = remember(status) { stepsFor(status) }
    val done = steps.count { it.done }
    val requiredPending = steps.count { it.required && !it.done }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val firstPending = steps.firstOrNull { !it.done }?.key
    val expanded = chosen?.let { name -> steps.firstOrNull { it.key.name == name && !it.done }?.key } ?: firstPending

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Box(
                    Modifier
                        .size(width = 16.dp, height = 3.dp)
                        .background(colors.voltText, CircleShape)
                )
                Overline(text = "Calentamiento · 1 minuto", color = colors.textSecondary)
            }
            Text(text = "QUE TU DESCANSO NUNCA FALLE", style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
            Text(
                text = "Activa estos permisos para que DarmixFit te avise al terminar cada descanso, aunque el teléfono esté bloqueado o estés en otra app.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        ProgressPanel(done = done, total = steps.size, requiredPending = requiredPending)

        Column {
            steps.forEachIndexed { index, step ->
                StepRow(
                    number = index + 1,
                    step = step,
                    isFirst = index == 0,
                    isLast = index == steps.lastIndex,
                    expanded = step.key == expanded,
                    onToggle = { if (!step.done) chosen = step.key.name }
                ) {
                    when (step.key) {
                        StepKey.NOTIFICATIONS -> {
                            Hint(text = "Si el sistema no muestra el aviso, se abrirán los ajustes: activa «Permitir notificaciones».")
                            DarmixButton(
                                text = "PERMITIR NOTIFICACIONES",
                                onClick = onNotifications,
                                icon = Icons.Rounded.NotificationsActive,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StepKey.EXACT_ALARMS -> {
                            Hint(text = "En la pantalla que se abre, activa «Permitir alarmas y recordatorios» y vuelve.")
                            DarmixButton(
                                text = "ABRIR AJUSTE",
                                onClick = onExactAlarms,
                                icon = Icons.Rounded.Alarm,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StepKey.BATTERY -> {
                            Hint(text = "Elige «Permitir» o «Sin restricciones». No afecta la duración de tu batería en el día a día.")
                            DarmixButton(
                                text = "QUITAR RESTRICCIÓN",
                                onClick = onBattery,
                                icon = Icons.Rounded.BatteryChargingFull,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StepKey.XIAOMI -> {
                            XiaomiSteps(detectable = status.autostartDetectable, fallback = autostartFallback)
                            DarmixButton(
                                text = "ABRIR INICIO AUTOMÁTICO",
                                onClick = onOpenAutostart,
                                icon = Icons.Rounded.RocketLaunch,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!status.autostartDetectable) {
                                DarmixButton(
                                    text = "Ya lo activé",
                                    onClick = onConfirmXiaomi,
                                    style = DarmixButtonStyle.Secondary,
                                    icon = Icons.Rounded.Check,
                                    height = 48.dp,
                                    haptic = null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Hint(text = "Extra: en Recientes, mantén presionada DarmixFit y toca el candado para que el sistema no la cierre.")
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            DarmixButton(
                text = if (requiredPending == 0) "Continuar sin lo recomendado" else "Omitir por ahora",
                onClick = onSkip,
                style = DarmixButtonStyle.Ghost,
                height = 48.dp,
                haptic = null,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Puedes entrenar igual; te lo recordaremos la próxima vez que abras la app.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProgressPanel(done: Int, total: Int, requiredPending: Int) {
    val colors = DarmixTheme.colors
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 1f else done.toFloat() / total,
        animationSpec = Motion.gentle(),
        label = "setup-progress"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Hero)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2f
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(colors.cardHigh, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                if (progress > 0f) {
                    drawArc(colors.voltText, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$done", style = DarmixTextStyles.MetricSmall, color = colors.textPrimary)
                Text(
                    text = "/$total",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = when {
                    requiredPending > 0 && done == 0 -> "Empecemos por lo necesario"
                    requiredPending > 0 -> "Vas por buen camino"
                    else -> "Lo esencial ya está"
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                text = when {
                    requiredPending > 1 -> "Faltan $requiredPending ajustes necesarios."
                    requiredPending == 1 -> "Falta 1 ajuste necesario."
                    else -> "Solo quedan ajustes recomendados para máxima fiabilidad."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    step: SetupStepUi,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit
) {
    val colors = DarmixTheme.colors
    val accent = colors.voltText
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val border by animateColorAsState(
        targetValue = when {
            expanded -> accent.copy(alpha = 0.7f)
            else -> colors.hairline
        },
        animationSpec = Motion.snappy(),
        label = "step-border"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithCache {
                val x = RailWidth.toPx() / 2f
                val y = NodeY.toPx()
                val line = 2.dp.toPx()
                onDrawBehind {
                    if (!isFirst) drawLine(if (step.done) accent.copy(alpha = 0.8f) else colors.hairline, Offset(x, 0f), Offset(x, y), line)
                    if (!isLast) drawLine(if (step.done) accent.copy(alpha = 0.8f) else colors.hairline, Offset(x, y), Offset(x, size.height), line)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(RailWidth)
                .padding(top = NodeY - 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = step.done,
                transitionSpec = { scaleIn(initialScale = 0.5f) + fadeIn() togetherWith fadeOut() },
                label = "node"
            ) { isDone ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDone) accent else colors.canvas)
                        .border(2.dp, if (isDone || expanded) accent else colors.hairlineStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Rounded.Check, contentDescription = "Listo", tint = onAccent, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            text = "$number",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (expanded) accent else colors.textSecondary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.md)
                .clip(DarmixShapes.Card)
                .background(if (step.done) colors.canvasDeep else colors.card)
                .border(if (expanded) 1.5.dp else 1.dp, border, DarmixShapes.Card)
                .clickable(enabled = !step.done && !expanded, role = Role.Button, onClick = onToggle)
                .padding(Spacing.md + 2.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (step.done) colors.leafSoft else colors.cardHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = if (step.done) colors.leafText else colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (step.done) colors.textSecondary else colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                when {
                    step.done -> StatusTag(text = "Listo", container = colors.leafSoft, content = colors.leafText)
                    step.required -> StatusTag(text = "Necesario", container = accent.copy(alpha = 0.14f), content = accent)
                    else -> StatusTag(text = "Recomendado", container = colors.tideText.copy(alpha = 0.14f), content = colors.tideText)
                }
            }
            AnimatedVisibility(
                visible = expanded && !step.done,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(text = step.why, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    actions()
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(colors.cardRaised)
            .drawBehind { drawRect(colors.tideText, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = Spacing.md + 3.dp, end = Spacing.md, top = Spacing.sm + 2.dp, bottom = Spacing.sm + 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = colors.tideText,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun XiaomiSteps(detectable: Boolean, fallback: Boolean) {
    val colors = DarmixTheme.colors
    val items = buildList {
        add(Icons.Rounded.RocketLaunch to "Toca «Abrir inicio automático».")
        if (fallback) {
            add(Icons.Rounded.Search to "Se abrió la información de la app: busca la opción «Inicio automático».")
            add(Icons.Rounded.ToggleOn to "Activa el interruptor.")
        } else {
            add(Icons.Rounded.Search to "En la lista de apps, busca DarmixFit. Puedes usar la lupa de arriba.")
            add(Icons.Rounded.ToggleOn to "Activa el interruptor que está junto a DarmixFit.")
        }
        add(
            Icons.AutoMirrored.Rounded.KeyboardReturn to if (detectable) {
                "Regresa a DarmixFit: este paso se marcará solo."
            } else {
                "Regresa a DarmixFit y toca «Ya lo activé»."
            }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
        items.forEachIndexed { index, (icon, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(DarmixShapes.Tag)
                        .background(colors.cardHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${index + 1}", style = MaterialTheme.typography.labelLarge, color = colors.voltText)
                }
                Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AllSetCelebration() {
    val colors = DarmixTheme.colors
    val accent = colors.voltText
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val disc = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        disc.animateTo(1f, Motion.bouncy())
        check.animateTo(1f, tween(420, easing = Motion.Emphasized))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val r = size.minDimension / 2f
            val d = disc.value
            drawCircle(
                brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.3f * d), Color.Transparent), center = center, radius = r),
                radius = r,
                center = center
            )
            drawCircle(accent, radius = r * 0.5f * d, center = center)
            val s = r * 0.22f
            val path = Path().apply {
                moveTo(center.x - s, center.y + s * 0.05f)
                lineTo(center.x - s * 0.25f, center.y + s * 0.8f)
                lineTo(center.x + s * 1.1f, center.y - s * 0.7f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * check.value, partial, true)
            drawPath(partial, onAccent, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(text = "TODO LISTO", style = MaterialTheme.typography.displayMedium, color = colors.textPrimary)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Tu descanso sonará a tiempo. A entrenar.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
