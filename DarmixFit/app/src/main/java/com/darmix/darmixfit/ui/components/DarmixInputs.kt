package com.darmix.darmixfit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTextStyles
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing

@Composable
fun DarmixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val colors = DarmixTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = colors.textTertiary) } },
        isError = isError,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        shape = DarmixShapes.Control,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.cardRaised,
            unfocusedContainerColor = colors.cardRaised,
            cursorColor = colors.voltText,
            focusedBorderColor = colors.voltText,
            unfocusedBorderColor = colors.hairline,
            focusedLabelColor = colors.voltText,
            unfocusedLabelColor = colors.textSecondary,
            errorBorderColor = colors.ember,
            errorLabelColor = colors.ember,
            errorCursorColor = colors.ember
        )
    )
}

@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val background by animateColorAsState(
        targetValue = if (selected) colors.voltText else colors.cardRaised,
        animationSpec = Motion.snappy(),
        label = "chip-bg"
    )
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
        maxLines = 1,
        modifier = modifier
            .clip(DarmixShapes.Pill)
            .background(background)
            .border(1.dp, if (selected) colors.voltText else colors.hairline, DarmixShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DarmixShapes.Control)
            .background(colors.cardRaised)
            .border(1.dp, colors.hairline, DarmixShapes.Control)
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Overline(text = label, color = colors.textSecondary)
            if (hint != null) {
                Text(text = hint, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            }
        }
        DarmixIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Menos",
            onClick = { onValueChange((value - 1).coerceIn(range)) },
            enabled = value > range.first,
            style = DarmixButtonStyle.Secondary,
            size = 44.dp,
            shape = CircleShape,
            haptic = HapticFeedbackType.SegmentTick
        )
        Box(modifier = Modifier.width(52.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "$value",
                style = DarmixTextStyles.MetricSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
        DarmixIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Más",
            onClick = { onValueChange((value + 1).coerceIn(range)) },
            enabled = value < range.last,
            style = DarmixButtonStyle.Secondary,
            size = 44.dp,
            shape = CircleShape,
            haptic = HapticFeedbackType.SegmentTick
        )
    }
}
