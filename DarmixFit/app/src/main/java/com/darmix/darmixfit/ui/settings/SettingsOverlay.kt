package com.darmix.darmixfit.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixfit.ui.theme.AccentPalette
import com.darmix.darmixfit.ui.theme.DarmixColors
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.tones

private const val APP_URL = "https://darmixista.dev/darmixfit/"
private const val OPEN_MILLIS = 720
private const val CLOSE_MILLIS = 420

private const val LINE_END = 0.24f
private const val OPEN_END = 0.68f
private const val CONTENT_START = 0.55f

private val PanelCut = 20.dp
private val PanelShape = CutCornerShape(topEnd = PanelCut, bottomStart = PanelCut)
private val Chamfer10 = CutCornerShape(10.dp)
private val LightMetal = listOf(Color(0xFFFFFFFF), Color(0xFFDFE2DA), Color(0xFFBCC0B7))

private fun span(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

private fun ease(t: Float): Float = Motion.Emphasized.transform(t)

@Composable
fun SettingsOverlay(visible: Boolean, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(if (visible) OPEN_MILLIS else CLOSE_MILLIS, easing = LinearEasing)
        )
    }
    val shown by remember { derivedStateOf { progress.value > 0f } }
    if (!visible && !shown) return

    BackHandler(enabled = visible, onBack = onDismiss)

    val colors = DarmixTheme.colors
    val accent = colors.volt
    val scrim = if (colors.isDark) Color(0xBD040503) else Color(0x6B1E241C)
    val t = { progress.value }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = span(t(), 0f, LINE_END) }
                .background(scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = visible,
                    onClick = onDismiss
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .drawWithContent {
                    val p = t()
                    val open = ease(span(p, LINE_END, OPEN_END))
                    val pad = 40.dp.toPx()
                    val half = size.height / 2f
                    if (open > 0f) {
                        val reach = (half + pad) * open
                        clipRect(left = -pad, top = half - reach, right = size.width + pad, bottom = half + reach) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    val grow = ease(span(p, 0f, LINE_END))
                    val fade = 1f - span(p, OPEN_END, 0.86f)
                    if (grow > 0f && fade > 0f) {
                        val width = size.width * grow
                        val left = (size.width - width) / 2f
                        val offset = half * open
                        listOf(half - offset, half + offset).forEach { y ->
                            drawLine(accent.copy(alpha = 0.18f * fade), Offset(left, y), Offset(left + width, y), strokeWidth = 10.dp.toPx())
                            drawLine(accent.copy(alpha = 0.4f * fade), Offset(left, y), Offset(left + width, y), strokeWidth = 4.dp.toPx())
                            drawLine(accent.copy(alpha = fade), Offset(left, y), Offset(left + width, y), strokeWidth = 2.dp.toPx())
                        }
                    }
                }
        ) {
            SettingsPanel(
                onClose = onDismiss,
                contentProgress = { ease(span(t(), CONTENT_START, 1f)) }
            )
        }
    }
}

@Composable
private fun SettingsPanel(onClose: () -> Unit, contentProgress: () -> Float) {
    val colors = DarmixTheme.colors
    val actions = LocalSettingsActions.current
    val hapticsOn = LocalHapticsEnabled.current
    val dark = colors.isDark
    val currentAccent = DarmixTheme.accent
    val tone = panelTone(colors)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .panelFrame(colors)
            .clip(PanelShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                val c = contentProgress()
                alpha = c
                translationY = (1f - c) * 6.dp.toPx()
            }
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                PanelTitle(text = "CONFIGURACIÓN", color = colors.voltText)
                Spacer(Modifier.height(18.dp))

                SectionLabel("TEMA")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOption(
                        label = "CLARO",
                        icon = Icons.Rounded.LightMode,
                        selected = !dark,
                        tone = tone,
                        onClick = { actions.setDark(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        label = "OSCURO",
                        icon = Icons.Rounded.DarkMode,
                        selected = dark,
                        tone = tone,
                        onClick = { actions.setDark(true) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("PALETA DE COLOR")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccentPalette.entries.forEach { palette ->
                        PaletteSwatch(
                            palette = palette,
                            selected = palette == currentAccent,
                            tone = tone,
                            onClick = { actions.setAccent(palette) }
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("RESPUESTA TÁCTIL")
                VibrationRow(
                    enabled = hapticsOn,
                    tone = tone,
                    onToggle = { actions.setHaptics(!hapticsOn) }
                )
            }
            Spacer(Modifier.height(22.dp))
            PanelFooter(tone = tone)
        }

        CloseButton(
            tone = tone,
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
        )
    }
}

private class PanelTone(
    val well: Color,
    val wellBorder: Color,
    val divider: Color,
    val footer: Color,
    val gap: Color,
    val metal: List<Color>,
    val glow: Float,
    val dark: Boolean
)

@Composable
private fun panelTone(colors: DarmixColors): PanelTone {
    val reactor = DarmixTheme.reactor
    return remember(colors, reactor) {
        if (colors.isDark) {
            PanelTone(
                well = colors.canvasDeep,
                wellBorder = colors.hairlineStrong,
                divider = colors.hairline,
                footer = colors.volt.copy(alpha = 0.04f),
                gap = reactor.tubeDark,
                metal = reactor.screw,
                glow = 1f,
                dark = true
            )
        } else {
            PanelTone(
                well = colors.canvas,
                wellBorder = colors.hairlineStrong,
                divider = colors.hairline,
                footer = colors.voltText.copy(alpha = 0.05f),
                gap = Color.White,
                metal = LightMetal,
                glow = 0.4f,
                dark = false
            )
        }
    }
}

private fun Modifier.panelFrame(colors: DarmixColors): Modifier = drawWithCache {
    val cut = PanelCut.toPx()
    val outline = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width - cut, 0f)
        lineTo(size.width, cut)
        lineTo(size.width, size.height)
        lineTo(cut, size.height)
        lineTo(0f, size.height - cut)
        close()
    }
    val dark = colors.isDark
    val edge = if (dark) colors.volt else lerp(colors.volt, colors.voltDeep, 0.3f)
    val fill = Brush.verticalGradient(
        0f to if (dark) lerp(colors.card, colors.volt, 0.07f) else lerp(Color.White, colors.volt, 0.08f),
        0.32f to if (dark) colors.card else Color.White,
        1f to if (dark) colors.card else Color.White
    )
    val glow = if (dark) 1f else 0.4f
    onDrawBehind {
        drawPath(outline, edge, alpha = 0.07f * glow, style = Stroke(18.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outline, edge, alpha = 0.16f * glow, style = Stroke(8.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outline, edge, alpha = 0.45f * glow, style = Stroke(3.5.dp.toPx(), join = StrokeJoin.Miter))
        drawPath(outline, fill)
        drawPath(outline, edge, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Miter))
    }
}

@Composable
private fun PanelTitle(text: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .drawWithCache {
                    val reach = 34.dp.toPx()
                    val left = -reach
                    val width = size.width + reach * 2f
                    val low = size.height - 1.dp.toPx()
                    val high = low - 7.5.dp.toPx()
                    val path = Path().apply {
                        moveTo(left, low)
                        lineTo(left + width * 0.24f, low)
                        lineTo(left + width * 0.30f, high)
                        lineTo(left + width * 0.70f, high)
                        lineTo(left + width * 0.76f, low)
                        lineTo(left + width, low)
                    }
                    val brush = Brush.horizontalGradient(
                        0f to color.copy(alpha = 0f),
                        0.22f to color,
                        0.78f to color,
                        1f to color.copy(alpha = 0f),
                        startX = left,
                        endX = left + width
                    )
                    val stroke = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
                    onDrawBehind { drawPath(path, brush, style = stroke) }
                }
                .padding(start = 6.dp, end = 6.dp, bottom = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 2.8.sp),
                color = color,
                modifier = Modifier.padding(start = 2.8.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = DarmixTheme.colors
    val line = colors.voltText
    Box(
        modifier = Modifier
            .padding(start = 10.dp, bottom = 14.dp)
            .drawBehind {
                val y = size.height - 0.8.dp.toPx()
                val path = Path().apply {
                    moveTo(-8.dp.toPx(), y - 7.4.dp.toPx())
                    lineTo(-0.5.dp.toPx(), y)
                    lineTo(size.width + 4.dp.toPx(), y)
                }
                drawPath(path, line, style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
            }
            .padding(start = 2.dp, bottom = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 2.4.sp),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tone: PanelTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DarmixTheme.colors
    val content = if (selected) colors.onVolt else colors.textSecondary
    val base = Modifier
        .height(46.dp)
        .clip(Chamfer10)
    val styled = if (selected) {
        base.background(Brush.verticalGradient(listOf(colors.voltBright, colors.volt, lerp(colors.volt, colors.voltDeep, 0.35f))))
    } else {
        base
            .background(tone.well)
            .border(1.dp, tone.wellBorder, Chamfer10)
    }
    Row(
        modifier = modifier
            .then(styled)
            .clickable(role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                letterSpacing = 1.6.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
            ),
            color = content
        )
    }
}

@Composable
private fun PaletteSwatch(
    palette: AccentPalette,
    selected: Boolean,
    tone: PanelTone,
    onClick: () -> Unit
) {
    val colors = DarmixTheme.colors
    val tones = palette.tones
    val label by animateColorAsState(
        targetValue = if (selected) (if (tone.dark) tones.main else tones.deep) else colors.textTertiary,
        animationSpec = tween(Motion.Standard),
        label = "swatch-label"
    )
    val border = tone.wellBorder
    Column(
        modifier = Modifier
            .width(62.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2f
                if (selected) {
                    drawCircle(tones.main.copy(alpha = 0.22f * (0.5f + tone.glow / 2f)), radius = r + 6.dp.toPx(), center = c, style = Stroke(5.dp.toPx()))
                    drawCircle(tones.main, radius = r + 3.25.dp.toPx(), center = c, style = Stroke(1.5.dp.toPx()))
                    drawCircle(tone.gap, radius = r + 1.25.dp.toPx(), center = c, style = Stroke(2.5.dp.toPx()))
                } else {
                    drawCircle(border, radius = r + 0.5.dp.toPx(), center = c, style = Stroke(1.dp.toPx()))
                }
                drawCircle(
                    Brush.radialGradient(
                        0f to tone.metal[0],
                        0.55f to tone.metal[1],
                        1f to tone.metal[2],
                        center = Offset(size.width * 0.38f, size.height * 0.35f),
                        radius = r * 1.3f
                    ),
                    radius = r,
                    center = c
                )
                val inner = 15.dp.toPx()
                drawCircle(
                    Brush.radialGradient(
                        0f to tones.bright,
                        0.55f to tones.main,
                        1f to lerp(tones.main, tones.deep, 0.6f),
                        center = Offset(c.x - inner * 0.2f, c.y - inner * 0.3f),
                        radius = inner * 1.3f
                    ),
                    radius = inner,
                    center = c
                )
            }
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = tones.ink, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.3.sp),
            color = label
        )
    }
}

@Composable
private fun VibrationRow(enabled: Boolean, tone: PanelTone, onToggle: () -> Unit) {
    val colors = DarmixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Chamfer10)
            .background(tone.well)
            .border(1.dp, tone.wellBorder, Chamfer10)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CutCornerShape(7.dp))
                .background(colors.volt.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Vibration, contentDescription = null, tint = colors.voltText, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vibración",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.textPrimary
            )
            Text(
                text = "Al girar el anillo del reactor",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                maxLines = 1
            )
        }
        ChamferSwitch(on = enabled)
    }
}

@Composable
private fun ChamferSwitch(on: Boolean) {
    val colors = DarmixTheme.colors
    val knobX by animateDpAsState(
        targetValue = if (on) 20.dp else 0.dp,
        animationSpec = Motion.snappy(),
        label = "switch-knob"
    )
    val off = colors.hairlineStrong
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(CutCornerShape(6.dp))
            .background(
                if (on) Brush.verticalGradient(listOf(colors.voltBright, colors.volt))
                else Brush.verticalGradient(listOf(off, off))
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX)
                .padding(3.dp)
                .size(20.dp)
                .clip(CutCornerShape(5.dp))
                .background(if (on) colors.onVolt else colors.textTertiary)
        )
    }
}

@Composable
private fun CloseButton(tone: PanelTone, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val shape = CutCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(shape)
            .background(tone.well)
            .border(1.dp, tone.wellBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Button,
                onClickLabel = "Cerrar",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun PanelFooter(tone: PanelTone) {
    val colors = DarmixTheme.colors
    val uriHandler = LocalUriHandler.current
    val brand = remember(colors) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = colors.textPrimary)) { append("DARMIX") }
            withStyle(SpanStyle(color = colors.voltText)) { append("FIT") }
        }
    }
    val brandStyle = remember {
        TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 17.sp, lineHeight = 17.sp, letterSpacing = 2.sp)
    }
    val linkShape = CutCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(tone.footer)
                drawLine(tone.divider, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 20.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = brand, style = brandStyle)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "POR DARMIXISTA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold),
                color = colors.textTertiary
            )
        }
        Row(
            modifier = Modifier
                .clip(linkShape)
                .background(tone.well)
                .border(1.dp, colors.voltText, linkShape)
                .clickable(role = Role.Button) { runCatching { uriHandler.openUri(APP_URL) } }
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "CONOCE LA APP",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.4.sp),
                color = colors.voltText
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.voltText, modifier = Modifier.size(16.dp))
        }
    }
}
