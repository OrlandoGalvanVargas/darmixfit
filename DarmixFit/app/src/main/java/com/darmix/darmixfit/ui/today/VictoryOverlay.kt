package com.darmix.darmixfit.ui.today

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixfit.R
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing

private const val TIMELINE_MILLIS = 2200

private val PoseFrames = listOf(
    R.drawable.darmix_pose_1,
    R.drawable.darmix_pose_2,
    R.drawable.darmix_pose_3,
    R.drawable.darmix_pose_4
)

private val GlowFrames = listOf(
    R.drawable.darmix_pose_glow_1,
    R.drawable.darmix_pose_glow_2,
    R.drawable.darmix_pose_glow_3,
    R.drawable.darmix_pose_glow_4
)

private val FrameWindows = listOf(
    floatArrayOf(0f, 0.15f, 0.37f, 0.44f),
    floatArrayOf(0.37f, 0.44f, 0.50f, 0.56f),
    floatArrayOf(0.50f, 0.56f, 0.62f, 0.67f),
    floatArrayOf(0.62f, 0.67f, 2f, 2f)
)

private fun window(t: Float, w: FloatArray): Float = when {
    t <= w[0] -> 0f
    t < w[1] -> (t - w[0]) / (w[1] - w[0])
    t <= w[2] -> 1f
    t < w[3] -> 1f - (t - w[2]) / (w[3] - w[2])
    else -> 0f
}

private fun span(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

private fun Modifier.reveal(progress: () -> Float, start: Float, length: Float = 0.16f): Modifier =
    graphicsLayer {
        val p = span(progress(), start, start + length)
        val eased = 1f - (1f - p) * (1f - p)
        alpha = eased
        translationY = (1f - eased) * 12.dp.toPx()
    }

@Composable
internal fun SessionCompleteOverlay(
    planTitle: String?,
    totalSets: Int,
    totalExercises: Int,
    onStay: () -> Unit
) {
    val colors = DarmixTheme.colors
    val fill = colors.voltText
    val onFill = if (colors.isDark) colors.onVolt else Color.White
    val timeline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeline.animateTo(1f, tween(TIMELINE_MILLIS, easing = LinearEasing))
    }
    val progress = { timeline.value }

    val tint = remember(fill, colors.isDark) {
        val shadow = if (colors.isDark) Color.Black else lerp(colors.volt, Color.Black, 0.82f)
        val light = if (colors.isDark) fill else lerp(colors.volt, Color.White, 0.12f)
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    light.red - shadow.red, 0f, 0f, 0f, shadow.red * 255f,
                    light.green - shadow.green, 0f, 0f, 0f, shadow.green * 255f,
                    light.blue - shadow.blue, 0f, 0f, 0f, shadow.blue * 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    val glowTint = remember(fill) { ColorFilter.tint(fill) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.canvas.copy(alpha = 0.95f), colors.canvasDeep.copy(alpha = 0.985f))
                )
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        val figureHeight = (maxHeight * 0.5f).coerceIn(200.dp, 400.dp)
        val figureWidth = figureHeight * 0.75f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = figureWidth, height = figureHeight)
                    .drawBehind {
                        val t = progress()
                        val floorBoost = 0.35f + 0.65f * span(t, 0.62f, 0.70f) - 0.3f * span(t, 0.70f, 1f)
                        val floorCenter = Offset(size.width / 2f, size.height - 4.dp.toPx())
                        val floorRadius = size.width * 0.42f
                        drawOval(
                            brush = Brush.radialGradient(
                                listOf(fill.copy(alpha = 0.55f * floorBoost), Color.Transparent),
                                center = floorCenter,
                                radius = floorRadius
                            ),
                            topLeft = Offset(floorCenter.x - floorRadius, floorCenter.y - 13.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(floorRadius * 2f, 26.dp.toPx())
                        )
                        val flash = span(t, 0.64f, 1f)
                        if (flash > 0f && flash < 1f) {
                            val ringAlpha = if (flash < 0.17f) flash / 0.17f * 0.9f else 0.9f * (1f - (flash - 0.17f) / 0.83f)
                            val radius = size.width * (0.25f + 0.55f * flash)
                            val ringCenter = Offset(size.width / 2f, size.height * 0.42f)
                            drawCircle(fill.copy(alpha = ringAlpha * 0.25f), radius = radius, center = ringCenter, style = Stroke(10.dp.toPx()))
                            drawCircle(fill.copy(alpha = ringAlpha), radius = radius, center = ringCenter, style = Stroke(2.dp.toPx()))
                        }
                    }
                    .graphicsLayer {
                        val t = progress()
                        val enter = span(t, 0f, 0.15f)
                        val pop = when {
                            t < 0.62f -> 0f
                            t < 0.69f -> span(t, 0.62f, 0.69f)
                            else -> 1f - span(t, 0.69f, 0.78f)
                        }
                        val scale = 0.93f + 0.07f * enter + 0.045f * pop
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - enter) * 10.dp.toPx()
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.92f)
                    }
            ) {
                PoseFrames.forEachIndexed { index, frame ->
                    PoseLayer(
                        glow = GlowFrames[index],
                        pose = frame,
                        tint = tint,
                        glowTint = glowTint,
                        glowStrength = if (colors.isDark) 0.6f else 0.35f,
                        alpha = { window(progress(), FrameWindows[index]) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            BasicText(
                text = "DÍA COMPLETO",
                style = MaterialTheme.typography.displayMedium.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 30.sp, maxFontSize = 52.sp, stepSize = 2.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal(progress, 0.72f)
            )
            Spacer(Modifier.height(Spacing.sm))
            if (!planTitle.isNullOrBlank()) {
                Text(
                    text = planTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.reveal(progress, 0.76f)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = "$totalSets series · $totalExercises ejercicios",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = fill,
                textAlign = TextAlign.Center,
                modifier = Modifier.reveal(progress, 0.78f)
            )

            Spacer(Modifier.height(Spacing.xl))

            CloseButton(
                fill = fill,
                onFill = onFill,
                onClick = onStay,
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal(progress, 0.84f)
            )
        }
    }
}

@Composable
private fun PoseLayer(
    @DrawableRes glow: Int,
    @DrawableRes pose: Int,
    tint: ColorFilter,
    glowTint: ColorFilter,
    glowStrength: Float,
    alpha: () -> Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha() }
    ) {
        Image(
            painter = painterResource(glow),
            contentDescription = null,
            colorFilter = glowTint,
            alpha = glowStrength,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(pose),
            contentDescription = null,
            colorFilter = tint,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CloseButton(fill: Color, onFill: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = remember { CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = Motion.snappy(),
        label = "close-press"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(54.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(fill, Color.White, 0.35f), fill, lerp(fill, Color.Black, 0.18f))))
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CERRAR",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
            color = onFill
        )
    }
}
