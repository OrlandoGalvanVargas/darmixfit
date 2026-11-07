package com.darmix.darmixfit.ui.update

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.update.ApkUpdater
import com.darmix.darmixfit.data.update.ReleaseInfo
import com.darmix.darmixfit.data.update.UpdateChecker
import com.darmix.darmixfit.ui.common.emphasized
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.DisplayFamily
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing
import kotlin.math.abs

private val MarkdownLink = Regex("\\[(.*?)]\\((.*?)\\)")
private val MarkdownBold = Regex("\\*\\*(.+?)\\*\\*")

private val PanelShape = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp)
private val NotesShape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
private val PlateShape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
private val TagShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
private val CellShape = CutCornerShape(2.dp)
private val BulletShape = CutCornerShape(topEnd = 2.dp, bottomStart = 2.dp)

private const val RING_SEGMENTS = 20
private const val BAR_CELLS = 24

private val Display = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black)

@Composable
fun UpdatePrompt(viewModel: UpdateViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val minimized by viewModel.minimized.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.refresh()
    }
    val context = LocalContext.current
    val current = state

    LaunchedEffect(current) {
        if (current is UpdateState.Ready) launchInstall(context, current)
    }

    if (current is UpdateState.Hidden) return
    if (minimized && current is UpdateState.Downloading) return

    Dialog(
        onDismissRequest = {
            if (current is UpdateState.Downloading) viewModel.minimize() else viewModel.later()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        UpdateCard(
            state = current,
            currentVersion = viewModel.currentVersion,
            onLater = viewModel::later,
            onDownload = {
                val info = (current as? UpdateState.Available)?.info ?: (current as? UpdateState.Failed)?.info
                if (info?.apkUrl == null) {
                    info?.let { openUrl(context, it.pageUrl) }
                    viewModel.later()
                } else {
                    viewModel.download()
                }
            },
            onHide = viewModel::minimize,
            onInstall = { (current as? UpdateState.Ready)?.let { launchInstall(context, it) } }
        )
    }
}

private fun launchInstall(context: Context, state: UpdateState.Ready) {
    if (ApkUpdater.canInstall(context)) {
        if (!ApkUpdater.install(context, state.uri)) openUrl(context, state.info.pageUrl)
    } else {
        ApkUpdater.openInstallPermission(context)
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Modifier.glowOutline(shape: Shape, color: Color, glow: Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val halo = Stroke(width = 6.dp.toPx())
    val edge = Stroke(width = 1.2.dp.toPx())
    onDrawWithContent {
        drawContent()
        drawOutline(outline, color.copy(alpha = 0.16f * glow), style = halo)
        drawOutline(outline, color.copy(alpha = 0.85f), style = edge)
    }
}

@Composable
private fun UpdateCard(
    state: UpdateState,
    currentVersion: String,
    onLater: () -> Unit,
    onDownload: () -> Unit,
    onHide: () -> Unit,
    onInstall: () -> Unit
) {
    val colors = DarmixTheme.colors
    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Ready -> state.info
        is UpdateState.Failed -> state.info
        UpdateState.Hidden -> return
    }
    val failed = state is UpdateState.Failed
    val accent = if (failed) colors.ember else colors.volt
    val glowStrength = if (colors.isDark) 1f else 0.5f

    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.screen)
            .fillMaxWidth()
            .glowOutline(PanelShape, accent, glowStrength)
            .clip(PanelShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(colors.card, accent, if (colors.isDark) 0.06f else 0.08f),
                        colors.card
                    )
                )
            )
            .drawWithCache {
                val center = Offset(size.width * 0.18f, 0f)
                val radius = size.width * 0.75f
                val glow = Brush.radialGradient(
                    listOf(accent.copy(alpha = if (colors.isDark) 0.14f else 0.18f), Color.Transparent),
                    center = center,
                    radius = radius
                )
                onDrawBehind { drawCircle(glow, radius = radius, center = center) }
            }
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusRing(state = state)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HudTag(text = kickerFor(state), color = if (failed) colors.emberText else colors.voltText)
                Text(
                    text = "VERSIÓN ${info.version}",
                    style = Display.copy(fontSize = 34.sp, lineHeight = 34.sp),
                    color = colors.textPrimary
                )
                VersionPath(from = currentVersion, to = info.version)
            }
        }

        NotesPanel(info = info)

        AnimatedContent(
            targetState = state::class,
            transitionSpec = { fadeIn(Motion.enter()) togetherWith fadeOut(Motion.exit()) },
            label = "update-actions"
        ) { kind ->
            when (kind) {
                UpdateState.Downloading::class -> DownloadingActions(
                    progress = (state as? UpdateState.Downloading)?.progress,
                    onHide = onHide
                )

                UpdateState.Ready::class -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Note(text = "Descarga completa. Si Android te pide «permitir instalar apps» desde DarmixFit, actívalo y regresa.")
                    ActionRow(primary = "INSTALAR", icon = Icons.Rounded.InstallMobile, onLater = onLater, onPrimary = onInstall)
                }

                UpdateState.Failed::class -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Note(text = "La descarga se cortó. Revisa tu conexión y vuelve a intentarlo.", color = colors.emberText)
                    ActionRow(primary = "REINTENTAR", icon = Icons.Rounded.Refresh, onLater = onLater, onPrimary = onDownload)
                }

                else -> ActionRow(primary = "ACTUALIZAR", icon = Icons.Rounded.Download, onLater = onLater, onPrimary = onDownload)
            }
        }

        OfficialPageLink()
    }
}

private fun kickerFor(state: UpdateState): String = when (state) {
    is UpdateState.Available -> "HAY VERSIÓN NUEVA"
    is UpdateState.Downloading -> "DESCARGANDO"
    is UpdateState.Ready -> "LISTA PARA INSTALAR"
    is UpdateState.Failed -> "DESCARGA FALLIDA"
    UpdateState.Hidden -> ""
}

@Composable
private fun HudTag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        ),
        color = color,
        modifier = Modifier
            .clip(TagShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), TagShape)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun StatusRing(state: UpdateState) {
    val colors = DarmixTheme.colors
    val transition = rememberInfiniteTransition(label = "update-ring")
    val chase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "chase"
    )
    val bob by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "bob"
    )
    val progress = (state as? UpdateState.Downloading)?.progress
    val filled by animateFloatAsState(
        targetValue = when (state) {
            is UpdateState.Ready -> 1f
            is UpdateState.Downloading -> progress ?: 0f
            else -> 0f
        },
        animationSpec = Motion.gentle(),
        label = "ring-fill"
    )
    val failed = state is UpdateState.Failed
    val lit = if (failed) colors.ember else colors.volt
    val off = colors.cardHigh
    val chasing = state is UpdateState.Available || (state is UpdateState.Downloading && progress == null)
    val icon = when (state) {
        is UpdateState.Ready -> Icons.Rounded.InstallMobile
        is UpdateState.Failed -> Icons.Rounded.Refresh
        else -> Icons.Rounded.Download
    }

    Box(modifier = Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(78.dp)) {
            val stroke = 7.dp.toPx()
            val radius = size.minDimension / 2f - stroke
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)
            val step = 360f / RING_SEGMENTS
            drawCircle(
                brush = Brush.radialGradient(listOf(lit.copy(alpha = 0.22f), Color.Transparent)),
                radius = size.minDimension / 2f
            )
            for (i in 0 until RING_SEGMENTS) {
                val start = -90f + i * step + 2.5f
                val sweep = step - 5f
                val level = when {
                    failed -> 0.45f
                    chasing -> {
                        val head = chase * RING_SEGMENTS
                        val distance = ((head - i) % RING_SEGMENTS + RING_SEGMENTS) % RING_SEGMENTS
                        (1f - distance / 6f).coerceIn(0f, 1f)
                    }
                    else -> (filled * RING_SEGMENTS - i).coerceIn(0f, 1f)
                }
                drawArc(off, start, sweep, false, topLeft, arcSize, style = Stroke(width = stroke))
                if (level > 0f) {
                    drawArc(lit.copy(alpha = 0.25f * level), start - 1f, sweep + 2f, false, topLeft, arcSize, style = Stroke(width = stroke + 5.dp.toPx()))
                    drawArc(lit, start, sweep, false, topLeft, arcSize, alpha = level, style = Stroke(width = stroke))
                }
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CutCornerShape(10.dp))
                .background(colors.cardRaised)
                .border(1.dp, colors.hairlineStrong, CutCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (failed) colors.emberText else colors.voltText,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        translationY = if (state is UpdateState.Downloading) bob.dp.toPx() else 0f
                    }
            )
        }
    }
}

@Composable
private fun VersionPath(from: String, to: String) {
    val colors = DarmixTheme.colors
    val chip = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "v$from",
            style = chip,
            color = colors.textTertiary,
            modifier = Modifier
                .clip(TagShape)
                .background(colors.cardHigh)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "v$to",
            style = chip,
            color = colors.onVolt,
            modifier = Modifier
                .clip(TagShape)
                .background(colors.volt)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun NotesPanel(info: ReleaseInfo) {
    val colors = DarmixTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NotesShape)
            .background(colors.canvasDeep.copy(alpha = if (colors.isDark) 0.7f else 0.5f))
            .border(1.dp, colors.hairline, NotesShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "QUÉ CAMBIA",
                style = Display.copy(fontSize = 18.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
                color = colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.hairline)
            )
        }
        Column(
            modifier = Modifier
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReleaseNotes(info = info)
        }
    }
}

@Composable
private fun ReleaseNotes(info: ReleaseInfo) {
    val colors = DarmixTheme.colors
    val body = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    val bold = SpanStyle(fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
    val lines = remember(info.notes) {
        info.notes.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(40)
    }
    if (lines.isEmpty()) {
        Text(text = "Correcciones y ajustes menores.", style = body, color = colors.textSecondary)
        return
    }
    lines.forEach { line ->
        when {
            line.startsWith("#") -> Text(
                text = clean(line.trimStart('#').trim()).replace("«", "").replace("»", "").uppercase(),
                style = Display.copy(fontSize = 15.sp, lineHeight = 17.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.voltText,
                modifier = Modifier.padding(top = 4.dp)
            )

            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(7.dp)
                        .clip(BulletShape)
                        .background(colors.volt)
                )
                Text(text = emphasized(clean(line.drop(2)), bold), style = body, color = colors.textSecondary)
            }

            else -> Text(text = emphasized(clean(line), bold), style = body, color = colors.textSecondary)
        }
    }
}

private fun clean(text: String): String =
    text.replace(MarkdownLink, "$1")
        .replace(MarkdownBold, "«$1»")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")

@Composable
private fun Note(text: String, color: Color = DarmixTheme.colors.textSecondary) {
    Text(
        text = emphasized(text, SpanStyle(fontWeight = FontWeight.ExtraBold)),
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionRow(primary: String, icon: ImageVector, onLater: () -> Unit, onPrimary: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Plate(text = "DESPUÉS", onClick = onLater, primary = false, modifier = Modifier.width(118.dp))
        Plate(text = primary, icon = icon, onClick = onPrimary, primary = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Plate(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = DarmixTheme.colors
    val content = if (primary) colors.onVolt else colors.textPrimary
    val base = modifier
        .height(50.dp)
        .clip(PlateShape)
    val styled = if (primary) {
        base.background(Brush.verticalGradient(listOf(colors.voltBright, colors.volt)))
    } else {
        base
            .background(colors.cardRaised)
            .border(1.dp, colors.hairlineStrong, PlateShape)
    }
    Row(
        modifier = styled
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = content),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp
            ),
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun DownloadingActions(progress: Float?, onHide: () -> Unit) {
    val colors = DarmixTheme.colors
    val animated by animateFloatAsState(targetValue = progress ?: 0f, animationSpec = Motion.gentle(), label = "download")
    val sweep by rememberInfiniteTransition(label = "indeterminate").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "sweep"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (progress == null) "PREPARANDO" else "DESCARGA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = colors.textTertiary,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 4.dp)
            )
            if (progress != null) {
                Text(
                    text = "${(progress * 100).toInt()}",
                    style = Display.copy(fontSize = 32.sp, lineHeight = 32.sp),
                    color = colors.voltText
                )
                Text(
                    text = "%",
                    style = Display.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    color = colors.textTertiary
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(BAR_CELLS) { index ->
                val level = if (progress != null) {
                    (animated * BAR_CELLS - index).coerceIn(0f, 1f)
                } else {
                    val head = sweep * (BAR_CELLS + 6) - 3f
                    (1f - abs(head - index) / 3f).coerceIn(0f, 1f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(CellShape)
                        .background(colors.cardHigh)
                ) {
                    if (level > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = level }
                                .background(Brush.verticalGradient(listOf(colors.voltBright, colors.volt)))
                        )
                    }
                }
            }
        }
        Note(text = "Puedes seguir usando la app. Cuando termine, se abre el instalador.")
        Plate(text = "SEGUIR EN SEGUNDO PLANO", onClick = onHide, primary = false, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OfficialPageLink() {
    val colors = DarmixTheme.colors
    val text = remember(colors) {
        buildAnnotatedString {
            append("¿No se instala? Descárgala desde la ")
            withLink(
                LinkAnnotation.Url(
                    url = UpdateChecker.OFFICIAL_PAGE,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = colors.voltText,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            ) {
                append("página oficial")
            }
            append(".")
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(color = colors.textTertiary, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth()
    )
}
