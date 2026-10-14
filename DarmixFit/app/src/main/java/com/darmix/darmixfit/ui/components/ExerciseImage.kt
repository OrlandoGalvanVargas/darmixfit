package com.darmix.darmixfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.darmix.darmixfit.data.ExerciseImageStore

/** Miniatura del ejercicio; si no tiene imagen muestra un recuadro con su inicial. */
@Composable
fun ExerciseImage(
    imageName: String?,
    exerciseName: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .size(size)
        .clip(shape)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
    val shaped = if (onClick != null) base.clickable(onClick = onClick) else base

    if (imageName == null) {
        val gradient = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        )
        Box(
            modifier = shaped.background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = exerciseName.firstOrNull { it.isLetter() }?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    } else {
        val file = remember(imageName) { ExerciseImageStore.fileFor(context, imageName) }
        AsyncImage(
            model = file,
            contentDescription = exerciseName,
            contentScale = ContentScale.Crop,
            modifier = shaped,
        )
    }
}