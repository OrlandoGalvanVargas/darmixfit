package com.darmix.darmixfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

private val MexicoLocale: Locale = Locale.forLanguageTag("es-MX")
private const val SET_WORK_SECONDS = 40
private val ReactorMaxSize = 420.dp
private val HudHeight = 38.dp

private data class DayTitle(val main: String, val detail: String?)

private fun splitTitle(raw: String): DayTitle {
    val text = raw.trim()
    val open = text.indexOf('(')
    if (open <= 0 || !text.endsWith(")")) return DayTitle(text, null)
    val main = text.substring(0, open).trim()
    val parts = text.substring(open + 1, text.length - 1)
        .split(",", " y ", " e ", "/")
        .map { part -> part.trim().replaceFirstChar { it.titlecase(MexicoLocale) } }
        .filter { it.isNotEmpty() }
    if (main.isEmpty() || parts.isEmpty()) return DayTitle(text, null)
    return DayTitle(main, parts.joinToString(" · "))
}

@Composable
internal fun TodayTitle(date: LocalDate, planTitle: String?) {
    val colors = DarmixTheme.colors
    val title = remember(date, planTitle) {
        splitTitle(planTitle ?: date.dayOfWeek.getDisplayName(TextStyle.FULL, MexicoLocale))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.sm, bottom = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = title.main.uppercase(MexicoLocale),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 40.sp),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        title.detail?.let { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp
                ),
                color = colors.textPrimary.copy(alpha = 0.86f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TodayReactorSection(state: TodayUiState, restSeconds: Int) {
    val finished = state.totalSets > 0 && state.completedSets >= state.totalSets
    val currentBlock = when {
        state.totalCount == 0 -> 0
        state.activeIndex >= 0 -> state.activeIndex + 1
        else -> state.totalCount
    }
    val remainingSets = (state.totalSets - state.completedSets).coerceAtLeast(0)
    val minutes = ceil(remainingSets * (SET_WORK_SECONDS + restSeconds) / 60f).toInt().coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen),
            contentAlignment = Alignment.Center
        ) {
            val side = min(maxWidth * 0.97f, ReactorMaxSize)
            ReactorCore(
                completed = state.completedSets,
                total = state.totalSets,
                modifier = Modifier.size(side)
            )
        }
        HudPanels(
            leftIcon = if (finished) Icons.Outlined.CheckCircle else Icons.Outlined.Layers,
            leftText = if (finished) "Bloques ${state.totalCount}/${state.totalCount}" else "Bloque $currentBlock/${state.totalCount}",
            rightIcon = if (finished) Icons.Outlined.EmojiEvents else Icons.Outlined.Schedule,
            rightText = if (finished) "Día completado" else "Tiempo $minutes min",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen + 3.dp)
                .height(HudHeight)
        )
    }
}

@Composable
private fun HudPanels(
    leftIcon: ImageVector,
    leftText: String,
    rightIcon: ImageVector,
    rightText: String,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val accent = colors.voltText
    Row(
        modifier = modifier.drawWithCache {
            val k = 1.dp.toPx()
            val w = size.width
            val h = size.height
            val mid = w / 2f
            val gap = 1.5f * k
            val left = Path().apply {
                moveTo(14f * k, 1f * k)
                lineTo(mid - gap, 1f * k)
                lineTo(mid - gap, h - 1f * k)
                lineTo(1f * k, h - 1f * k)
                lineTo(1f * k, h - 14f * k)
                lineTo(8f * k, h - 14f * k)
                lineTo(8f * k, 8f * k)
                close()
            }
            val right = Path().apply {
                moveTo(w - 14f * k, 1f * k)
                lineTo(mid + gap, 1f * k)
                lineTo(mid + gap, h - 1f * k)
                lineTo(w - 1f * k, h - 1f * k)
                lineTo(w - 1f * k, h - 14f * k)
                lineTo(w - 8f * k, h - 14f * k)
                lineTo(w - 8f * k, 8f * k)
                close()
            }
            val fill = Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.04f)))
            val glowWide = Stroke(width = 7f * k, join = StrokeJoin.Round)
            val glowNear = Stroke(width = 3.5f * k, join = StrokeJoin.Round)
            val line = Stroke(width = 1.2f * k, join = StrokeJoin.Round)
            onDrawBehind {
                listOf(left, right).forEach { panel ->
                    drawPath(panel, accent, alpha = 0.07f, style = glowWide)
                    drawPath(panel, accent, alpha = 0.14f, style = glowNear)
                    drawPath(panel, fill)
                    drawPath(panel, accent, alpha = 0.75f, style = line)
                }
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudCell(icon = leftIcon, text = leftText, modifier = Modifier.weight(1f).padding(start = 10.dp))
        HudCell(icon = rightIcon, text = rightText, modifier = Modifier.weight(1f).padding(end = 10.dp))
    }
}

@Composable
private fun HudCell(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.voltText, modifier = Modifier.size(17.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RoutineLabel() {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.xl, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md + 2.dp)
    ) {
        Text(
            text = "RUTINA DE HOY",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 30.sp),
            color = colors.textPrimary
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.hairline)
        )
    }
}
