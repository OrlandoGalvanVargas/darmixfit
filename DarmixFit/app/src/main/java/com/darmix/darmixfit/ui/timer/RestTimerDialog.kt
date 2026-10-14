package com.darmix.darmixfit.ui.timer

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixfit.service.RestTimer
import com.darmix.darmixfit.service.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

private val QuickPresets = listOf(30, 60, 90, 120)

@Composable
fun RestTimerDialog(
    timer: TimerState,
    initialSeconds: Int,
    onStart: (Int) -> Unit,
    onCancelRunning: () -> Unit,
    onClose: () -> Unit,
) {
    var seconds by rememberSaveable {
        mutableStateOf(initialSeconds.coerceIn(RestTimer.MIN_SECONDS, RestTimer.MAX_SECONDS))
    }
    val running = timer.active

    // Mismo comportamiento que antes: mientras corre, no se cierra con atrás ni tocando fuera.
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = if (running) "Descansando…" else "Descanso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                if (running) {
                    val remainingMs = rememberRemainingMs(timer)
                    val remainingSeconds = ((remainingMs + 999) / 1000).toInt()
                    val progress = if (timer.totalSeconds > 0) {
                        (remainingMs / (timer.totalSeconds * 1000f)).coerceIn(0f, 1f)
                    } else 0f

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(180.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeWidth = 10.dp,
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            text = formatTime(remainingSeconds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedButton(
                        onClick = onCancelRunning,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Text("Cancelar descanso") }
                } else {
                    Text(
                        text = formatTime(seconds),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        QuickPresets.forEach { preset ->
                            val selected = seconds == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    )
                                    .clickable { seconds = preset },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${preset}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Slider(
                        value = seconds.toFloat(),
                        onValueChange = { seconds = it.roundToInt() },
                        valueRange = RestTimer.MIN_SECONDS.toFloat()..RestTimer.MAX_SECONDS.toFloat(),
                        steps = 58, // saltos de 5 s entre 0:05 y 5:00
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    )
                    Text(
                        text = "Ajustable de 0:05 a 5:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.width(110.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) { Text("Cancelar") }
                        Button(
                            onClick = { onStart(seconds) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) { Text("Iniciar", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberRemainingMs(timer: TimerState): Long {
    var now by remember(timer) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(timer) {
        while (isActive) {
            now = SystemClock.elapsedRealtime()
            delay(200)
        }
    }
    return (timer.endAtElapsed - now).coerceAtLeast(0L)
}

private fun formatTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)