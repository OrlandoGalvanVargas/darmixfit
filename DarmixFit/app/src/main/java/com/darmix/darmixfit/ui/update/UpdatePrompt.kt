package com.darmix.darmixfit.ui.update

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darmix.darmixfit.data.update.ApkUpdater
import com.darmix.darmixfit.data.update.ReleaseInfo
import com.darmix.darmixfit.data.update.UpdateChecker
import com.darmix.darmixfit.ui.components.DarmixButton
import com.darmix.darmixfit.ui.components.DarmixButtonStyle
import com.darmix.darmixfit.ui.components.Overline
import com.darmix.darmixfit.ui.theme.DarmixShapes
import com.darmix.darmixfit.ui.theme.DarmixTheme
import com.darmix.darmixfit.ui.theme.Motion
import com.darmix.darmixfit.ui.theme.Spacing

private val MarkdownLink = Regex("\\[(.*?)]\\((.*?)\\)")

@Composable
fun UpdatePrompt(viewModel: UpdateViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val minimized by viewModel.minimized.collectAsStateWithLifecycle()
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

    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.screen)
            .fillMaxWidth()
            .clip(DarmixShapes.Hero)
            .background(colors.card)
            .drawWithCache {
                val center = Offset(size.width / 2f, 0f)
                val radius = size.width * 0.8f
                val glow = Brush.radialGradient(
                    listOf(colors.volt.copy(alpha = if (colors.isDark) 0.18f else 0.28f), Color.Transparent),
                    center = center,
                    radius = radius
                )
                onDrawBehind { drawCircle(glow, radius = radius, center = center) }
            }
            .border(1.dp, colors.voltText.copy(alpha = 0.35f), DarmixShapes.Hero)
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        RocketBadge()

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Overline(text = "Nueva versión disponible", color = colors.voltText)
            Text(
                text = "DARMIXFIT ${info.version}",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }

        VersionPath(from = currentVersion, to = info.version)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DarmixShapes.Card)
                .background(colors.cardRaised)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Overline(text = "Novedades", color = colors.textTertiary)
            Column(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReleaseNotes(info = info)
            }
        }

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

                UpdateState.Ready::class -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "Descarga completa. Si Android te lo pide, permite instalar desde esta app y vuelve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                        DarmixButton(
                            text = "Más tarde",
                            onClick = onLater,
                            style = DarmixButtonStyle.Ghost,
                            haptic = null,
                            modifier = Modifier.width(120.dp)
                        )
                        DarmixButton(
                            text = "INSTALAR",
                            onClick = onInstall,
                            icon = Icons.Rounded.InstallMobile,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                UpdateState.Failed::class -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "No se pudo completar la descarga.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.emberText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                        DarmixButton(
                            text = "Más tarde",
                            onClick = onLater,
                            style = DarmixButtonStyle.Ghost,
                            haptic = null,
                            modifier = Modifier.width(120.dp)
                        )
                        DarmixButton(
                            text = "REINTENTAR",
                            onClick = onDownload,
                            icon = Icons.Rounded.Refresh,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                else -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
                    DarmixButton(
                        text = "Más tarde",
                        onClick = onLater,
                        style = DarmixButtonStyle.Ghost,
                        haptic = null,
                        modifier = Modifier.width(120.dp)
                    )
                    DarmixButton(
                        text = "ACTUALIZAR",
                        onClick = onDownload,
                        icon = Icons.Rounded.Download,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OfficialPageLink()
    }
}

@Composable
private fun RocketBadge() {
    val colors = DarmixTheme.colors
    val float = rememberInfiniteTransition(label = "rocket").animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "rocket-float"
    )
    Box(
        modifier = Modifier
            .size(84.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(colors.voltText.copy(alpha = 0.3f), Color.Transparent)),
                    radius = size.minDimension / 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(colors.voltText),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer { translationY = float.value.dp.toPx() }
            )
        }
    }
}

@Composable
private fun VersionPath(from: String, to: String) {
    val colors = DarmixTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "v$from",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textTertiary,
            modifier = Modifier
                .clip(DarmixShapes.Pill)
                .background(colors.cardHigh)
                .padding(horizontal = Spacing.md, vertical = 6.dp)
        )
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
        Text(
            text = "v$to",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(DarmixShapes.Pill)
                .background(colors.voltText)
                .padding(horizontal = Spacing.md, vertical = 6.dp)
        )
    }
}

@Composable
private fun ReleaseNotes(info: ReleaseInfo) {
    val colors = DarmixTheme.colors
    val lines = remember(info.notes) {
        info.notes.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(40)
    }
    if (lines.isEmpty()) {
        Text(text = "Mejoras de rendimiento y correcciones.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        return
    }
    lines.forEach { line ->
        when {
            line.startsWith("#") -> Text(
                text = clean(line.trimStart('#').trim()),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )

            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.voltText)
                )
                Text(text = clean(line.drop(2)), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }

            else -> Text(text = clean(line), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

private fun clean(text: String): String =
    text.replace(MarkdownLink, "$1").replace("**", "").replace("__", "").replace("`", "")

@Composable
private fun DownloadingActions(progress: Float?, onHide: () -> Unit) {
    val colors = DarmixTheme.colors
    val animated by animateFloatAsState(targetValue = progress ?: 0f, animationSpec = Motion.gentle(), label = "download")
    val sweep = rememberInfiniteTransition(label = "indeterminate").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100)),
        label = "sweep"
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Descargando…", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
            if (progress != null) {
                Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = colors.voltText)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(colors.cardHigh)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2f)
                    if (progress != null) {
                        drawRoundRect(colors.voltText, size = size.copy(width = size.width * animated), cornerRadius = radius)
                    } else {
                        val w = size.width * 0.3f
                        val x = (size.width + w) * sweep.value - w
                        drawRoundRect(colors.voltText, topLeft = Offset(x, 0f), size = size.copy(width = w), cornerRadius = radius)
                    }
                }
        )
        Text(
            text = "Puedes seguir usando la app; al terminar se abrirá el instalador.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary
        )
        DarmixButton(
            text = "Seguir en segundo plano",
            onClick = onHide,
            style = DarmixButtonStyle.Ghost,
            haptic = null,
            height = 46.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OfficialPageLink() {
    val colors = DarmixTheme.colors
    val text = remember(colors) {
        buildAnnotatedString {
            append("¿Problemas para descargar o instalar? Consigue la app en su ")
            withLink(
                LinkAnnotation.Url(
                    url = UpdateChecker.OFFICIAL_PAGE,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = colors.voltText,
                            fontWeight = FontWeight.Bold,
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
        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth()
    )
}
