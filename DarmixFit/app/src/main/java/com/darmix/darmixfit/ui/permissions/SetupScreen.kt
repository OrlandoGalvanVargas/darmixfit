package com.darmix.darmixfit.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    status: SetupStatus,
    onNotifications: () -> Unit,
    onExactAlarms: () -> Unit,
    onBattery: () -> Unit,
    onOpenAutostart: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onConfirmXiaomi: () -> Unit,
) {
    val totalCount = 4
    val doneCount = listOf(
        status.notificationsOk,
        status.exactAlarmsOk,
        status.batteryOk,
        status.xiaomiOk,
    ).count { it }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SetupHeader(done = doneCount, total = totalCount)

            if (!status.notificationsOk) {
                SetupCard(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    description = "Android obliga a mostrar una notificación mientras " +
                            "el temporizador corre en segundo plano.",
                ) {
                    PrimaryAction(
                        text = "Permitir notificaciones",
                        onClick = onNotifications,
                    )
                }
            }

            if (!status.exactAlarmsOk) {
                SetupCard(
                    icon = Icons.Default.Alarm,
                    title = "Alarmas exactas",
                    description = "Permite que el aviso suene justo al llegar a cero, " +
                            "incluso con el teléfono en suspensión.",
                ) {
                    PrimaryAction(
                        text = "Abrir ajuste de alarmas",
                        onClick = onExactAlarms,
                    )
                }
            }

            if (!status.batteryOk) {
                SetupCard(
                    icon = Icons.Default.BatterySaver,
                    title = "Optimización de batería",
                    description = "Evita que Android detenga el temporizador para ahorrar " +
                            "batería. Elige «Permitir» o «Sin restricciones».",
                ) {
                    PrimaryAction(
                        text = "Quitar restricción",
                        onClick = onBattery,
                    )
                }
            }

            if (!status.xiaomiOk) {
                SetupCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Ajustes de Xiaomi / HyperOS",
                    description = "HyperOS puede cerrar la app en segundo plano aunque tenga " +
                            "permisos. Configura esto a mano:",
                ) {
                    XiaomiSteps()
                    Spacer(Modifier.height(4.dp))
                    OutlinedAction(
                        text = "Abrir Inicio automático",
                        onClick = onOpenAutostart,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedAction(
                        text = "Abrir ajustes de la app",
                        onClick = onOpenAppSettings,
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryAction(
                        text = "Ya lo configuré",
                        onClick = onConfirmXiaomi,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Header con progreso
// ─────────────────────────────────────────────────────────────

@Composable
private fun SetupHeader(done: Int, total: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val progress = if (total == 0) 0f else done.toFloat() / total

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ya casi está",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Solo faltan unos ajustes rápidos para que el temporizador " +
                    "de descanso suene incluso con la pantalla apagada.",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$done de $total completados",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(50),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(primary, RoundedCornerShape(50)),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Card de permiso
// ─────────────────────────────────────────────────────────────

@Composable
private fun SetupCard(
    icon: ImageVector,
    title: String,
    description: String,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = outline.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.large,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icono + título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primary.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, primary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }

            // Descripción
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Acciones
            actions()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Pasos numerados de Xiaomi
// ─────────────────────────────────────────────────────────────

@Composable
private fun XiaomiSteps() {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val steps = listOf(
        "Inicio automático: activa FitFlow.",
        "Ajustes de la app › Ahorro de batería: «Sin restricciones».",
        "Opcional: bloquea la tarjeta de FitFlow en Recientes.",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, text ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(primary.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, primary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Botones
// ─────────────────────────────────────────────────────────────

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OutlinedAction(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}