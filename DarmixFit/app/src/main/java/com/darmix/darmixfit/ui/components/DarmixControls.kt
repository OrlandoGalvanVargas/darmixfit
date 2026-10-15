package com.darmix.darmixfit.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion

enum class DarmixButtonStyle { Primary, Secondary, Ghost, Success }

@Immutable
private data class ButtonPalette(val container: Color, val content: Color, val border: Color?)

@Composable
private fun buttonPalette(style: DarmixButtonStyle, container: Color, content: Color): ButtonPalette {
    val colors = DarmixTheme.colors
    val base = when (style) {
        DarmixButtonStyle.Primary -> ButtonPalette(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            null
        )
        DarmixButtonStyle.Secondary -> ButtonPalette(colors.cardHigh, colors.textPrimary, null)
        DarmixButtonStyle.Ghost -> ButtonPalette(Color.Transparent, colors.textSecondary, colors.hairlineStrong)
        DarmixButtonStyle.Success -> ButtonPalette(colors.leafSoft, colors.leafText, null)
    }
    return base.copy(
        container = if (container.isSpecified) container else base.container,
        content = if (content.isSpecified) content else base.content
    )
}

@Composable
private fun Modifier.pressable(
    enabled: Boolean,
    shape: Shape,
    rippleColor: Color,
    haptic: HapticFeedbackType?,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = Motion.snappy(),
        label = "press-scale"
    )
    val hapticFeedback = LocalHapticFeedback.current
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = ripple(color = rippleColor),
            enabled = enabled,
            role = Role.Button
        ) {
            haptic?.let(hapticFeedback::performHapticFeedback)
            onClick()
        }
}

@Composable
fun DarmixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DarmixButtonStyle = DarmixButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    shape: Shape = DarmixShapes.Control,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    haptic: HapticFeedbackType? = HapticFeedbackType.Confirm
) {
    val palette = buttonPalette(style, containerColor, contentColor)
    val dim = !enabled && style != DarmixButtonStyle.Success
    Row(
        modifier = modifier
            .height(height)
            .pressable(enabled, shape, palette.content, haptic, onClick)
            .background(palette.container)
            .then(if (palette.border != null) Modifier.border(1.dp, palette.border, shape) else Modifier)
            .graphicsLayer { alpha = if (dim) 0.4f else 1f }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = palette.content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = palette.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DarmixIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DarmixButtonStyle = DarmixButtonStyle.Ghost,
    enabled: Boolean = true,
    size: Dp = 54.dp,
    shape: Shape = DarmixShapes.Control,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    haptic: HapticFeedbackType? = HapticFeedbackType.ContextClick
) {
    val palette = buttonPalette(style, containerColor, contentColor)
    Box(
        modifier = modifier
            .size(size)
            .pressable(enabled, shape, palette.content, haptic, onClick)
            .background(palette.container)
            .then(if (palette.border != null) Modifier.border(1.dp, palette.border, shape) else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = palette.content, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
fun StatusTag(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    pulse: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(DarmixShapes.Tag)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            pulse -> PulseDot(color = content)
            icon != null -> Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        }
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
    }
}

@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier, size: Dp = 7.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "pulse-phase"
    )
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val p = phase.value
                drawCircle(color.copy(alpha = (1f - p) * 0.5f), radius = this.size.minDimension / 2f * (1f + p * 1.2f))
                drawCircle(color, radius = this.size.minDimension / 2f)
            }
    )
}

@Composable
fun SetMeter(
    total: Int,
    completed: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    highlightNext: Boolean = false,
    height: Dp = 8.dp
) {
    if (total <= 0) return
    val track = DarmixTheme.colors.cardHigh
    if (total > 12) {
        val fraction by animateFloatAsState(
            targetValue = (completed.toFloat() / total).coerceIn(0f, 1f),
            animationSpec = Motion.gentle(),
            label = "meter-continuous"
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(CircleShape)
                .background(track)
                .drawBehind {
                    drawRoundRect(
                        color = accent,
                        size = this.size.copy(width = this.size.width * fraction),
                        cornerRadius = CornerRadius(this.size.height / 2f)
                    )
                }
        )
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(total) { index ->
            MeterSegment(
                filled = index < completed,
                next = highlightNext && index == completed,
                accent = accent,
                track = track
            )
        }
    }
}

@Composable
private fun RowScope.MeterSegment(filled: Boolean, next: Boolean, accent: Color, track: Color) {
    val fill by animateFloatAsState(
        targetValue = if (filled) 1f else 0f,
        animationSpec = Motion.snappy(),
        label = "segment-fill"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (next) accent.copy(alpha = 0.28f) else track)
            .drawBehind {
                if (fill > 0f) {
                    drawRoundRect(
                        color = accent,
                        size = size.copy(width = size.width * fill),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                }
            }
    )
}

@Composable
fun SetBoxes(
    total: Int,
    completed: Int,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier
) {
    if (total <= 0) return
    if (total > 8) {
        SetMeter(total = total, completed = completed, accent = accent, highlightNext = true, modifier = modifier, height = 10.dp)
        return
    }
    val colors = DarmixTheme.colors
    val transition = rememberInfiniteTransition(label = "set-boxes")
    val glow = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "set-glow"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { index ->
            val done = index < completed
            val current = index == completed
            val shape = RoundedCornerShape(12.dp)
            val background by animateColorAsState(
                targetValue = when {
                    done -> accent
                    current -> accent.copy(alpha = 0.12f)
                    else -> colors.cardHigh
                },
                animationSpec = Motion.snappy(),
                label = "set-box-bg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(background)
                    .then(
                        if (current) {
                            Modifier.drawBehind {
                                val w = 2.dp.toPx()
                                drawRoundRect(
                                    color = accent.copy(alpha = glow.value),
                                    topLeft = Offset(w / 2f, w / 2f),
                                    size = size.copy(width = size.width - w, height = size.height - w),
                                    cornerRadius = CornerRadius(12.dp.toPx() - w / 2f),
                                    style = Stroke(w)
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = done,
                    transitionSpec = { (scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith fadeOut() },
                    label = "set-box-content"
                ) { isDone ->
                    if (isDone) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = DarmixTextStyles.MetricSmall.copy(fontSize = DarmixTextStyles.MetricSmall.fontSize * 0.78f),
                            color = if (current) accent else colors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, content: Color = DarmixTheme.colors.textSecondary) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(DarmixShapes.Chip)
            .border(1.dp, DarmixTheme.colors.hairline, DarmixShapes.Chip)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
fun Overline(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text = text.uppercase(), style = DarmixTextStyles.Overline, color = color, modifier = modifier, maxLines = 1)
}
