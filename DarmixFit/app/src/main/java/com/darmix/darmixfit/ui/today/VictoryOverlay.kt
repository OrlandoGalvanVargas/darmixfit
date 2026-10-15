package com.darmix.darmixfit.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun Modifier.reveal(progress: () -> Float, start: Float, span: Float = 0.35f): Modifier =
    graphicsLayer {
        val p = ((progress() - start) / span).coerceIn(0f, 1f)
        alpha = p
        translationY = (1f - p) * 24.dp.toPx()
    }

@Composable
internal fun SessionCompleteOverlay(
    planTitle: String?,
    totalSets: Int,
    totalExercises: Int,
    onStay: () -> Unit,
    onSwitchProgram: () -> Unit
) {
    val colors = DarmixTheme.colors
    val fill = colors.voltText
    val onFill = if (colors.isDark) colors.onVolt else Color.White

    val emblem = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        emblem.animateTo(1f, Motion.bouncy())
        check.animateTo(1f, tween(480, easing = Motion.Emphasized))
    }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(1100, delayMillis = 220, easing = LinearEasing))
    }

    val ambient = rememberInfiniteTransition(label = "victory")
    val spin = ambient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)),
        label = "beams"
    )
    val rise = ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "sparks"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.canvas.copy(alpha = 0.94f), colors.canvasDeep.copy(alpha = 0.98f))
                )
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = Spacing.xxl + 4.dp, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawWithCache {
                        val c = size.center
                        val r = size.minDimension / 2f
                        val beams = 12
                        val beam = Path().apply {
                            moveTo(c.x, c.y)
                            lineTo(c.x + r * 1.1f, c.y - r * 0.09f)
                            lineTo(c.x + r * 1.1f, c.y + r * 0.09f)
                            close()
                        }
                        val beamBrush = Brush.radialGradient(
                            listOf(fill.copy(alpha = 0.16f), Color.Transparent),
                            center = c,
                            radius = r
                        )
                        val halo = Brush.radialGradient(
                            listOf(fill.copy(alpha = 0.30f), Color.Transparent),
                            center = c,
                            radius = r * 0.8f
                        )
                        val w = r * 0.92f
                        val h = r * 1.08f
                        val left = c.x - w / 2f
                        val top = c.y - h / 2f
                        val shield = Path().apply {
                            moveTo(c.x, top)
                            cubicTo(c.x + w * 0.22f, top + h * 0.08f, left + w * 0.86f, top + h * 0.07f, left + w, top + h * 0.12f)
                            lineTo(left + w, top + h * 0.48f)
                            cubicTo(left + w, top + h * 0.76f, c.x + w * 0.20f, top + h * 0.92f, c.x, top + h)
                            cubicTo(c.x - w * 0.20f, top + h * 0.92f, left, top + h * 0.76f, left, top + h * 0.48f)
                            lineTo(left, top + h * 0.12f)
                            cubicTo(left + w * 0.14f, top + h * 0.07f, c.x - w * 0.22f, top + h * 0.08f, c.x, top)
                            close()
                        }
                        val shieldBrush = Brush.verticalGradient(
                            listOf(fill, if (colors.isDark) DarmixPalette.VoltDeep else DarmixPalette.VoltInk),
                            startY = top,
                            endY = top + h
                        )
                        val innerStroke = Stroke(width = 2.dp.toPx())
                        val checkPath = Path().apply {
                            val s = w * 0.34f
                            val cy = c.y + h * 0.02f
                            moveTo(c.x - s * 0.62f, cy)
                            lineTo(c.x - s * 0.16f, cy + s * 0.46f)
                            lineTo(c.x + s * 0.66f, cy - s * 0.44f)
                        }
                        val measure = PathMeasure().apply { setPath(checkPath, false) }
                        val partial = Path()
                        val checkStroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val sparkCount = 14
                        val sparkSeeds = List(sparkCount) { i ->
                            val a = (i * 137.5f) % 360f
                            Triple((cos(Math.toRadians(a.toDouble())).toFloat()) * 0.75f, i / sparkCount.toFloat(), 1.2f + (i % 3) * 0.6f)
                        }
                        onDrawBehind {
                            val e = emblem.value
                            rotate(spin.value, c) {
                                for (i in 0 until beams) {
                                    rotate(i * 360f / beams, c) {
                                        drawPath(beam, beamBrush)
                                    }
                                }
                            }
                            drawCircle(halo, radius = r * 0.8f, center = c)
                            val t = rise.value
                            sparkSeeds.forEach { (dx, offset, sz) ->
                                val phase = (t + offset) % 1f
                                val alpha = sin(phase * PI.toFloat()) * 0.8f * e.coerceIn(0f, 1f)
                                drawCircle(
                                    color = fill.copy(alpha = alpha),
                                    radius = sz.dp.toPx(),
                                    center = Offset(c.x + dx * r, c.y + r * 0.7f - phase * r * 1.5f)
                                )
                            }
                            scale(scale = 0.5f + 0.5f * e, pivot = c) {
                                drawPath(shield, shieldBrush)
                                scale(scale = 0.84f, pivot = Offset(c.x, c.y + h * 0.02f)) {
                                    drawPath(shield, onFill.copy(alpha = 0.28f), style = innerStroke)
                                }
                                partial.reset()
                                measure.getSegment(0f, measure.length * check.value, partial, true)
                                drawPath(partial, onFill, style = checkStroke)
                            }
                        }
                    }
            )

            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.reveal({ reveal.value }, 0f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(fill, CircleShape)
                )
                Overline(text = "Sesión completa", color = fill)
                Box(
                    Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(fill, CircleShape)
                )
            }

            Spacer(Modifier.height(Spacing.md))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal({ reveal.value }, 0.12f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "DÍA", style = MaterialTheme.typography.displaySmall, color = fill)
                BasicText(
                    text = "CONQUISTADO",
                    style = MaterialTheme.typography.displayLarge.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 72.sp, stepSize = 2.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (planTitle != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = planTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal({ reveal.value }, 0.28f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                VictoryPlate(value = totalExercises, label = "Ejercicios", accent = fill, modifier = Modifier.weight(1f))
                VictoryPlate(value = totalSets, label = "Series", accent = colors.tideText, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Terminaste todas las series de hoy.\nAhora toca recuperar.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.reveal({ reveal.value }, 0.42f)
            )

            Spacer(Modifier.height(Spacing.xl))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal({ reveal.value }, 0.55f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                DarmixButton(
                    text = "SEGUIR AQUÍ",
                    onClick = onStay,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                DarmixButton(
                    text = "Cambiar de programa",
                    onClick = onSwitchProgram,
                    style = DarmixButtonStyle.Ghost,
                    icon = Icons.Rounded.SwapHoriz,
                    height = 48.dp,
                    haptic = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VictoryPlate(value: Int, label: String, accent: Color, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    Column(
        modifier = modifier
            .clip(DarmixShapes.Plate)
            .background(colors.card)
            .border(1.dp, colors.hairline, DarmixShapes.Plate)
            .drawBehind {
                drawRect(accent, size = size.copy(height = 3.dp.toPx()))
            }
            .padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(text = "$value", style = DarmixTextStyles.Metric, color = accent)
        Overline(text = label, color = colors.textTertiary)
    }
}
