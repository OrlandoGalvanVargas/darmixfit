package com.darmix.darmixfit.ui.permissions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixfit.ui.common.emphasized
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlin.math.cos
import kotlin.math.sin

private enum class StepKey { NOTIFICATIONS, EXACT_ALARMS, BATTERY, VENDOR }

private data class SetupStepUi(
    val key: StepKey,
    val title: String,
    val why: String,
    val icon: ImageVector,
    val done: Boolean,
    val required: Boolean
)

private val RailWidth = 34.dp
private val NodeSize = 28.dp
private val NodeY = 30.dp

private val PanelShape = CutCornerShape(topEnd = 20.dp, bottomStart = 20.dp)
private val CardShape = CutCornerShape(topEnd = 14.dp)
private val NodeShape = CutCornerShape(7.dp)
private val TileShape = CutCornerShape(8.dp)
private val TagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val PlateShape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
private val HintShape = CutCornerShape(topEnd = 10.dp)

private val Display = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black)

private fun stepsFor(status: SetupStatus): List<SetupStepUi> = buildList {
    add(
        SetupStepUi(
            key = StepKey.NOTIFICATIONS,
            title = "Notificaciones",
            why = "Con ellas ves la cuenta regresiva en la barra de notificaciones y te llega el aviso cuando termina el descanso, aunque estés en otra app.",
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
                why = "Sin este permiso Android puede retrasar el aviso varios segundos o hasta minutos. Con él suena justo cuando el contador llega a 0:00.",
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
            why = "Para ahorrar batería, Android congela las apps que no estás viendo. Si congela DarmixFit, el contador se detiene a mitad del descanso.",
            icon = Icons.Rounded.BatteryChargingFull,
            done = status.batteryOk,
            required = false
        )
    )
    val vendor = status.vendor
    if (vendor != null) {
        val guide = guideFor(vendor)
        add(
            SetupStepUi(
                key = StepKey.VENDOR,
                title = guide.title,
                why = guide.why,
                icon = Icons.Rounded.RocketLaunch,
                done = status.vendorOk,
                required = false
            )
        )
    }
}

private class VendorGuide(
    val title: String,
    val why: String,
    val button: String,
    val steps: List<String>,
    val extra: String?
)

private const val LOCK_TIP = "Un extra que ayuda: abre las apps recientes, mantén presionada DarmixFit y toca el candado. Así el sistema no la cierra."

private val SwitchSteps = listOf("Busca DarmixFit en la lista.", "Activa el interruptor que está junto a DarmixFit.")

private fun guideFor(vendor: Vendor): VendorGuide = when (vendor) {
    Vendor.XIAOMI -> VendorGuide(
        title = "Inicio automático",
        why = "En teléfonos Xiaomi, Redmi y POCO el sistema cierra las apps en segundo plano aunque tengan todos los permisos. Si no lo activas, el aviso puede no sonar con la pantalla bloqueada.",
        button = "ABRIR INICIO AUTOMÁTICO",
        steps = listOf("Busca DarmixFit en la lista. Puedes usar la lupa de arriba.", "Activa el interruptor que está junto a DarmixFit."),
        extra = LOCK_TIP
    )

    Vendor.HUAWEI, Vendor.HONOR -> VendorGuide(
        title = "Inicio de apps",
        why = "En Huawei y Honor el sistema decide solo qué apps siguen vivas. Si lo deja en automático, puede cerrar DarmixFit a mitad del descanso y el aviso no suena.",
        button = "ABRIR INICIO DE APPS",
        steps = listOf(
            "Busca DarmixFit en la lista.",
            "Desactiva «Administrar automáticamente».",
            "En la ventana que sale, deja activadas las tres opciones y toca «Aceptar»."
        ),
        extra = LOCK_TIP
    )

    Vendor.OPPO, Vendor.ONEPLUS -> VendorGuide(
        title = "Inicio automático",
        why = "En OPPO, realme y OnePlus el sistema cierra las apps en segundo plano para ahorrar batería. Si cierra DarmixFit, el aviso puede no sonar.",
        button = "ABRIR INICIO AUTOMÁTICO",
        steps = SwitchSteps,
        extra = "También ayuda: en Ajustes entra a Batería, busca DarmixFit y activa «Permitir actividad en segundo plano»."
    )

    Vendor.VIVO -> VendorGuide(
        title = "Segundo plano",
        why = "En vivo e iQOO el sistema corta las apps que trabajan en segundo plano. Si corta DarmixFit, el aviso puede no sonar.",
        button = "ABRIR AJUSTE",
        steps = SwitchSteps,
        extra = "Si en Batería ves «Permitir alto consumo en segundo plano», actívalo también para DarmixFit."
    )

    Vendor.SAMSUNG -> VendorGuide(
        title = "Apps que nunca se suspenden",
        why = "Samsung suspende las apps que no abres seguido, aunque la batería esté sin restricciones. Si suspende DarmixFit, el aviso puede no sonar.",
        button = "ABRIR BATERÍA",
        steps = listOf(
            "Entra a «Límites de uso en segundo plano».",
            "Toca «Apps que nunca se suspenden».",
            "Toca el botón + y agrega DarmixFit."
        ),
        extra = null
    )

    Vendor.ASUS -> VendorGuide(
        title = "Inicio automático",
        why = "En ASUS el administrador del sistema puede cerrar las apps en segundo plano. Si cierra DarmixFit, el aviso puede no sonar.",
        button = "ABRIR INICIO AUTOMÁTICO",
        steps = SwitchSteps,
        extra = LOCK_TIP
    )

    Vendor.TRANSSION -> VendorGuide(
        title = "Inicio automático",
        why = "En Tecno, Infinix e itel el sistema cierra las apps en segundo plano. Si cierra DarmixFit, el aviso puede no sonar.",
        button = "ABRIR INICIO AUTOMÁTICO",
        steps = SwitchSteps,
        extra = LOCK_TIP
    )
}

private val FallbackSteps = listOf(
    "Se abrió la información de la app. Entra a «Batería».",
    "Elige «Sin restricciones» o activa «Permitir actividad en segundo plano».",
    "Si aparece «Inicio automático», actívalo también."
)

private fun Modifier.glow(shape: Shape, color: Color, strength: Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        drawOutline(outline, color, alpha = 0.07f * strength, style = Stroke(16.dp.toPx(), join = StrokeJoin.Miter))
        drawOutline(outline, color, alpha = 0.16f * strength, style = Stroke(7.dp.toPx(), join = StrokeJoin.Miter))
        drawOutline(outline, color, alpha = 0.4f * strength, style = Stroke(3.dp.toPx(), join = StrokeJoin.Miter))
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
    onConfirmVendor: () -> Unit,
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
                onConfirmVendor = onConfirmVendor,
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
    onConfirmVendor: () -> Unit,
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
            .padding(horizontal = Spacing.screen, vertical = Spacing.xl)
    ) {
        Kicker(text = "ANTES DE ENTRENAR")
        Text(
            text = "PREPARA TU TELÉFONO",
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "DarmixFit te avisa cuando termina cada descanso. Para que el aviso llegue a tiempo, aunque tengas el teléfono bloqueado o estés en otra app, activa estos ajustes. Es una sola vez.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 10.dp)
        )

        ProgressPanel(
            done = done,
            total = steps.size,
            requiredPending = requiredPending,
            modifier = Modifier.padding(top = 22.dp)
        )

        Column(modifier = Modifier.padding(top = 22.dp)) {
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
                            Hint(text = "Si no aparece la ventana para permitir, se abrirán los ajustes de la app. Ahí activa «Permitir notificaciones».")
                            PrimaryPlate(
                                text = "PERMITIR NOTIFICACIONES",
                                icon = Icons.Rounded.NotificationsActive,
                                onClick = onNotifications,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StepKey.EXACT_ALARMS -> {
                            Hint(text = "En la pantalla que se abre, activa «Permitir alarmas y recordatorios» y regresa a la app.")
                            PrimaryPlate(
                                text = "ABRIR AJUSTE",
                                icon = Icons.Rounded.Alarm,
                                onClick = onExactAlarms,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StepKey.BATTERY -> {
                            Hint(text = "Elige «Sin restricciones» o «Permitir». La app solo trabaja en segundo plano mientras corre un descanso, así que casi no gasta batería.")
                            PrimaryPlate(
                                text = "QUITAR RESTRICCIÓN",
                                icon = Icons.Rounded.BatteryChargingFull,
                                onClick = onBattery,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StepKey.VENDOR -> {
                            val vendor = status.vendor
                            if (vendor != null) {
                                val guide = guideFor(vendor)
                                VendorSteps(
                                    steps = if (autostartFallback) FallbackSteps else guide.steps,
                                    detectable = status.autostartDetectable
                                )
                                PrimaryPlate(
                                    text = guide.button,
                                    icon = Icons.Rounded.RocketLaunch,
                                    onClick = onOpenAutostart,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!status.autostartDetectable) {
                                    GhostPlate(
                                        text = "YA LO ACTIVÉ",
                                        icon = Icons.Rounded.Check,
                                        onClick = onConfirmVendor,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                guide.extra?.let { Hint(text = it) }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostPlate(
                text = if (requiredPending == 0) "CONTINUAR ASÍ" else "AHORA NO",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (requiredPending == 0) {
                    "Lo obligatorio ya está. Lo recomendado lo puedes activar después."
                } else {
                    "Puedes entrenar sin esto, pero el aviso del descanso podría no sonar. Te lo vamos a recordar la próxima vez que abras la app."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Kicker(text: String) {
    val colors = DarmixTheme.colors
    val line = colors.voltText
    Box(
        modifier = Modifier
            .padding(start = 10.dp)
            .drawBehind {
                val y = size.height - 0.8.dp.toPx()
                val path = Path().apply {
                    moveTo(-8.dp.toPx(), y - 7.4.dp.toPx())
                    lineTo(-0.5.dp.toPx(), y)
                    lineTo(size.width + 4.dp.toPx(), y)
                }
                drawPath(path, line, style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
            }
            .padding(start = 2.dp, bottom = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.4.sp),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ProgressPanel(done: Int, total: Int, requiredPending: Int, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 1f else done.toFloat() / total,
        animationSpec = Motion.gentle(),
        label = "setup-progress"
    )
    val track = if (colors.isDark) lerp(colors.card, colors.hairline, 0.7f) else colors.hairline
    val lit = colors.volt
    val tick = colors.hairlineStrong
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glow(PanelShape, colors.volt, if (colors.isDark) 1f else 0.45f)
            .clip(PanelShape)
            .background(
                Brush.verticalGradient(
                    0f to lerp(colors.card, colors.volt, if (colors.isDark) 0.07f else 0.08f),
                    0.5f to colors.card,
                    1f to colors.card
                )
            )
            .border(1.5.dp, colors.voltText, PanelShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                val outer = size.minDimension / 2f
                for (k in 0 until 40) {
                    val a = Math.toRadians(k * 9.0 - 90.0)
                    val r0 = outer - 4.dp.toPx()
                    val r1 = outer - 1.dp.toPx()
                    drawLine(
                        tick,
                        Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                        Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val ring = outer - 12.dp.toPx()
                val stroke = 9.dp.toPx()
                val segments = total.coerceAtLeast(1)
                val span = 360f / segments
                val gap = 6f
                val filled = progress * segments
                for (i in 0 until segments) {
                    val start = -90f + i * span + gap / 2f
                    val sweep = span - gap
                    drawArc(track, start, sweep, false, Offset(c.x - ring, c.y - ring), Size(ring * 2, ring * 2), style = Stroke(stroke))
                    val f = (filled - i).coerceIn(0f, 1f)
                    if (f > 0f) {
                        drawArc(lit.copy(alpha = 0.25f), start, sweep * f, false, Offset(c.x - ring, c.y - ring), Size(ring * 2, ring * 2), style = Stroke(stroke + 6.dp.toPx()))
                        drawArc(lit, start, sweep * f, false, Offset(c.x - ring, c.y - ring), Size(ring * 2, ring * 2), style = Stroke(stroke))
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$done", style = Display.copy(fontSize = 30.sp, lineHeight = 30.sp), color = colors.textPrimary)
                Text(
                    text = "/$total",
                    style = Display.copy(fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = when {
                    done == 0 -> "NADA ACTIVADO"
                    requiredPending > 0 -> "VAS A LA MITAD"
                    else -> "LO IMPORTANTE YA ESTÁ"
                },
                style = Display.copy(fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.textPrimary
            )
            Text(
                text = when {
                    requiredPending > 1 -> "Faltan $requiredPending ajustes obligatorios."
                    requiredPending == 1 -> "Falta 1 ajuste obligatorio."
                    else -> "Solo quedan los recomendados, para que el aviso nunca se pierda."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
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
    val border by animateColorAsState(
        targetValue = if (expanded) accent else colors.hairline,
        animationSpec = Motion.snappy(),
        label = "step-border"
    )
    val rail = colors.hairlineStrong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithCache {
                val x = RailWidth.toPx() / 2f
                val y = NodeY.toPx()
                val line = 2.dp.toPx()
                onDrawBehind {
                    if (!isFirst) drawLine(if (step.done) accent else rail, Offset(x, 0f), Offset(x, y), line)
                    if (!isLast) drawLine(if (step.done) accent else rail, Offset(x, y), Offset(x, size.height), line)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(RailWidth)
                .padding(top = NodeY - NodeSize / 2),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = step.done,
                transitionSpec = { scaleIn(initialScale = 0.5f) + fadeIn() togetherWith fadeOut() },
                label = "node"
            ) { isDone ->
                Box(
                    modifier = Modifier
                        .size(NodeSize)
                        .clip(NodeShape)
                        .background(
                            if (isDone) Brush.verticalGradient(listOf(colors.voltBright, colors.volt))
                            else Brush.verticalGradient(listOf(colors.canvas, colors.canvas))
                        )
                        .then(if (isDone) Modifier else Modifier.border(1.5.dp, if (expanded) accent else colors.hairlineStrong, NodeShape)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Rounded.Check, contentDescription = "Listo", tint = colors.onVolt, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            text = "$number",
                            style = Display.copy(fontSize = 16.sp, lineHeight = 16.sp),
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
                .padding(bottom = 12.dp)
                .then(if (expanded) Modifier.glow(CardShape, colors.volt, if (colors.isDark) 0.6f else 0.3f) else Modifier)
                .clip(CardShape)
                .background(if (step.done) colors.canvasDeep else colors.card)
                .border(if (expanded) 1.5.dp else 1.dp, border, CardShape)
                .clickable(enabled = !step.done && !expanded, role = Role.Button, onClick = onToggle)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(TileShape)
                        .background(if (step.done) colors.leafSoft else if (expanded) colors.volt.copy(alpha = 0.14f) else colors.cardRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = when {
                            step.done -> colors.leafText
                            expanded -> colors.voltText
                            else -> colors.textPrimary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (step.done) colors.textSecondary else colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                when {
                    step.done -> HudTag(text = "LISTO", color = colors.leafText)
                    step.required -> HudTag(text = "OBLIGATORIO", color = colors.voltText)
                    else -> HudTag(text = "RECOMENDADO", color = colors.tideText)
                }
            }
            AnimatedVisibility(
                visible = expanded && !step.done,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = step.why,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                        color = colors.textSecondary
                    )
                    actions()
                }
            }
        }
    }
}

@Composable
private fun HudTag(text: String, color: Color) {
    val colors = DarmixTheme.colors
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(TagShape)
            .background(colors.card)
            .border(1.dp, color, TagShape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.3.sp),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun Hint(text: String) {
    val colors = DarmixTheme.colors
    val tide = colors.tideText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HintShape)
            .background(lerp(colors.canvasDeep, colors.tide, if (colors.isDark) 0.05f else 0.06f))
            .drawBehind { drawRect(tide, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = tide,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Text(
            text = emphasized(text, SpanStyle(fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, lineHeight = 18.sp),
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VendorSteps(steps: List<String>, detectable: Boolean) {
    val colors = DarmixTheme.colors
    val items = buildList {
        add(Icons.Rounded.RocketLaunch to "Toca el botón de abajo para abrir el ajuste.")
        steps.forEachIndexed { index, text ->
            add((if (index == steps.lastIndex) Icons.Rounded.ToggleOn else Icons.Rounded.Search) to text)
        }
        add(
            Icons.AutoMirrored.Rounded.KeyboardReturn to if (detectable) {
                "Regresa a DarmixFit. Este paso se marca solo."
            } else {
                "Regresa a DarmixFit y toca «Ya lo activé»."
            }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, (icon, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(TagShape)
                        .background(colors.cardRaised)
                        .border(1.dp, colors.hairlineStrong, TagShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = Display.copy(fontSize = 14.sp, lineHeight = 14.sp),
                        color = colors.voltText
                    )
                }
                Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                Text(
                    text = emphasized(text, SpanStyle(fontWeight = FontWeight.ExtraBold)),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrimaryPlate(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 48.dp) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier
            .height(height)
            .glow(PlateShape, colors.volt, if (colors.isDark) 0.7f else 0.35f)
            .clip(PlateShape)
            .background(Brush.verticalGradient(listOf(colors.voltBright, colors.volt, lerp(colors.volt, colors.voltDeep, 0.3f))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.onVolt, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Black),
            color = colors.onVolt,
            maxLines = 1
        )
    }
}

@Composable
private fun GhostPlate(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(PlateShape)
            .background(colors.canvasDeep)
            .border(1.dp, colors.hairlineStrong, PlateShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun AllSetCelebration() {
    val colors = DarmixTheme.colors
    val volt = colors.volt
    val tick = colors.hairlineStrong
    val ink = colors.onVolt
    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val spin = rememberInfiniteTransition(label = "all-set")
    val orbit by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "orbit"
    )
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(700, easing = Motion.Emphasized))
        check.animateTo(1f, tween(420, easing = Motion.Emphasized))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val c = center
            val outer = size.minDimension / 2f
            val r = ring.value
            drawCircle(
                Brush.radialGradient(listOf(volt.copy(alpha = 0.22f * r), Color.Transparent), center = c, radius = outer),
                radius = outer,
                center = c
            )
            rotate(orbit, pivot = c) {
                for (k in 0 until 60) {
                    val a = Math.toRadians(k * 6.0)
                    val major = k % 5 == 0
                    val r0 = outer - 10.dp.toPx()
                    val r1 = outer - (if (major) 2.dp else 5.dp).toPx()
                    drawLine(
                        if (major) volt.copy(alpha = 0.8f * r) else tick,
                        Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                        Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                        strokeWidth = (if (major) 2.dp else 1.dp).toPx()
                    )
                }
            }
            val arcR = outer - 24.dp.toPx()
            val stroke = 10.dp.toPx()
            for (i in 0 until 4) {
                val start = -90f + i * 90f + 4f
                val sweep = 82f * ((r * 4f - i).coerceIn(0f, 1f))
                if (sweep > 0f) {
                    drawArc(volt.copy(alpha = 0.25f), start, sweep, false, Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2), style = Stroke(stroke + 8.dp.toPx()))
                    drawArc(volt, start, sweep, false, Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2), style = Stroke(stroke))
                }
            }
            val tile = 64.dp.toPx() * r
            if (tile > 0f) {
                val cut = 14.dp.toPx() * r
                val left = c.x - tile / 2f
                val top = c.y - tile / 2f
                val shape = Path().apply {
                    moveTo(left + cut, top)
                    lineTo(left + tile - cut, top)
                    lineTo(left + tile, top + cut)
                    lineTo(left + tile, top + tile - cut)
                    lineTo(left + tile - cut, top + tile)
                    lineTo(left + cut, top + tile)
                    lineTo(left, top + tile - cut)
                    lineTo(left, top + cut)
                    close()
                }
                drawPath(shape, volt)
            }
            val s = 15.dp.toPx()
            val path = Path().apply {
                moveTo(c.x - s, c.y + s * 0.05f)
                lineTo(c.x - s * 0.25f, c.y + s * 0.8f)
                lineTo(c.x + s * 1.1f, c.y - s * 0.7f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * check.value, partial, true)
            drawPath(partial, ink, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(text = "TODO LISTO", style = MaterialTheme.typography.displayMedium, color = colors.textPrimary)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "El aviso del descanso va a sonar a tiempo.\nA entrenar.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
