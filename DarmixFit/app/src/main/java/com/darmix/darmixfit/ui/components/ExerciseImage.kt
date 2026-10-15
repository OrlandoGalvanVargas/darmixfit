package com.darmix.darmixfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.darmix.darmixfit.data.ExerciseImageStore
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme

@Composable
fun ExerciseImage(
    imageName: String?,
    exerciseName: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    animated: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = DarmixTheme.colors
    val model = remember(imageName) { imageName?.let { ExerciseImageStore.model(context, it) } }
    val request = remember(model, animated) {
        model?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .apply {
                    if (!animated) {
                        decoderFactory(BitmapFactoryDecoder.Factory())
                        memoryCacheKeyExtra("frame", "static")
                    }
                }
                .build()
        }
    }
    val shape = remember(size) { RoundedCornerShape(if (size >= 72.dp) 18.dp else 14.dp) }
    val initial = remember(exerciseName) { exerciseName.firstOrNull { it.isLetter() }?.uppercase() ?: "?" }
    val stripe = if (colors.isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.04f)
    val initialSize = with(LocalDensity.current) { (size * 0.46f).toSp() }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.cardHigh)
            .drawWithCache {
                val step = 8.dp.toPx()
                val lines = Path().apply {
                    var x = -this@drawWithCache.size.height
                    while (x < this@drawWithCache.size.width) {
                        moveTo(x, this@drawWithCache.size.height)
                        lineTo(x + this@drawWithCache.size.height, 0f)
                        x += step
                    }
                }
                val stroke = Stroke(1.dp.toPx())
                onDrawBehind { drawPath(lines, stripe, style = stroke) }
            }
            .border(1.dp, colors.hairline, shape)
            .then(if (onClick != null && request != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = DarmixTextStyles.Metric.copy(fontSize = initialSize, lineHeight = initialSize),
            color = colors.voltText.copy(alpha = if (request == null) 1f else 0.35f)
        )
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = exerciseName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (onClick != null && size >= 80.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.OpenInFull,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
