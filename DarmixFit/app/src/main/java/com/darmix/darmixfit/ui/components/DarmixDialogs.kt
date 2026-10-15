package com.darmix.darmixfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarmixSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = DarmixTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = DarmixShapes.Sheet,
        containerColor = colors.card,
        contentColor = colors.textPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.md, bottom = Spacing.sm)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.hairlineStrong)
            )
        },
        content = content
    )
}

@Composable
fun DarmixConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = DarmixTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DarmixShapes.Hero,
        containerColor = colors.card,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.emberSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.emberText, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            DarmixButton(
                text = confirmLabel.uppercase(),
                onClick = onConfirm,
                containerColor = colors.ember,
                contentColor = colors.onEmber,
                height = 48.dp
            )
        },
        dismissButton = {
            DarmixButton(
                text = "Cancelar",
                onClick = onDismiss,
                style = DarmixButtonStyle.Ghost,
                height = 48.dp,
                haptic = null
            )
        }
    )
}
