package com.darmix.darmixfit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.darmix.darmixfit.data.ExerciseImageStore
import com.darmix.darmixfit.ui.theme.DarmixPalette
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
fun FullscreenImageDialog(
    imageName: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val model = remember(imageName) { ExerciseImageStore.model(context, imageName) }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var showHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2800)
        showHint = false
    }

    fun bounded(target: Offset, zoom: Float): Offset {
        val maxX = boxSize.width * (zoom - 1f) / 2f
        val maxY = boxSize.height * (zoom - 1f) / 2f
        return Offset(target.x.coerceIn(-maxX, maxX), target.y.coerceIn(-maxY, maxY))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarmixPalette.Ink950)
                .onSizeChanged { boxSize = it }
        ) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { tap ->
                                showHint = false
                                scope.launch {
                                    if (scale.value > 1.05f) {
                                        launch { scale.animateTo(1f, Motion.snappy()) }
                                        offset.animateTo(Offset.Zero, Motion.snappy())
                                    } else {
                                        val center = Offset(boxSize.width / 2f, boxSize.height / 2f)
                                        val target = bounded((center - tap) * (DOUBLE_TAP_ZOOM - 1f), DOUBLE_TAP_ZOOM)
                                        launch { scale.animateTo(DOUBLE_TAP_ZOOM, Motion.snappy()) }
                                        offset.animateTo(target, Motion.snappy())
                                    }
                                }
                            })
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                showHint = false
                                scope.launch {
                                    val next = (scale.value * zoom).coerceIn(1f, MAX_ZOOM)
                                    scale.snapTo(next)
                                    offset.snapTo(bounded(offset.value + pan, next))
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            translationX = offset.value.x
                            translationY = offset.value.y
                        }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(start = Spacing.screen, end = Spacing.md, top = Spacing.sm, bottom = Spacing.xxl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Overline(text = "Técnica", color = DarmixPalette.Volt)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DarmixIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Cerrar",
                    onClick = onDismiss,
                    style = DarmixButtonStyle.Secondary,
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                    size = 44.dp,
                    shape = CircleShape
                )
            }

            AnimatedVisibility(
                visible = showHint && model != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.xxl)
            ) {
                Text(
                    text = "Pellizca para acercar · doble toque para zoom",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(DarmixShapes.Pill)
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                )
            }
        }
    }
}
