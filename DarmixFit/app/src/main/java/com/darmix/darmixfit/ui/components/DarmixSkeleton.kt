package com.darmix.darmixfit.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTheme

@Composable
fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier, shape: Shape = DarmixShapes.Card) {
    val colors = DarmixTheme.colors
    val shimmer = rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmer"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(colors.card)
            .drawBehind {
                val x = size.width * shimmer.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, colors.cardHigh, Color.Transparent),
                        start = Offset(x - size.width * 0.4f, 0f),
                        end = Offset(x + size.width * 0.4f, size.height)
                    )
                )
            }
    )
}
