package com.darmix.darmixfit.ui.today

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.darmix.darmixfit.ui.theme.ReactorColors
import com.darmix.darmixfit.ui.today.ReactorGeometry.CENTER
import com.darmix.darmixfit.ui.today.ReactorGeometry.CORE
import com.darmix.darmixfit.ui.today.ReactorGeometry.Center
import com.darmix.darmixfit.ui.today.ReactorGeometry.GLASS_OUTER
import com.darmix.darmixfit.ui.today.ReactorGeometry.GLASS_RIM
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class ReactorPainter(private val colors: ReactorColors, dark: Boolean) {

    private val layerBounds = Rect(-48f, -48f, ReactorGeometry.VIEWBOX + 48f, ReactorGeometry.VIEWBOX + 48f)

    private val grayLayer = Paint().apply {
        val w = 0.76f
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.2126f * w, 0.7152f * w, 0.0722f * w, 0f, 0f,
                    0.2126f * w, 0.7152f * w, 0.0722f * w, 0f, 0f,
                    0.2126f * w, 0.7152f * w, 0.0722f * w, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    private val flickerLayer = Paint()

    private val haloRadius = ReactorGeometry.BEZEL + 22f
    private val haloBrush = Brush.radialGradient(
        0f to colors.energy,
        ReactorGeometry.BEZEL / (ReactorGeometry.BEZEL + 22f) to colors.energy,
        1f to Color.Transparent,
        center = Center,
        radius = haloRadius
    )
    private val shadowCenter = Center
    private val haloScale = if (dark) 0.55f else 0.3f
    private val shadowStrength = if (dark) 0.5f else 0.12f
    private val shadowRadius = ReactorGeometry.BEZEL + if (dark) 7f else 4f
    private val shadowBrush = Brush.radialGradient(
        0f to Color.Black.copy(alpha = shadowStrength),
        (ReactorGeometry.BEZEL - 1f) / shadowRadius to Color.Black.copy(alpha = shadowStrength),
        1f to Color.Transparent,
        center = shadowCenter,
        radius = shadowRadius
    )
    private val baseHaloBrush = ringGlow(colors.baseHalo, ReactorGeometry.BASE_GLOW, 10f)
    private val ringShadowRadius = ReactorGeometry.RING + 18f
    private val ringShadowBrush = Brush.radialGradient(
        (ReactorGeometry.RING - 18f) / ringShadowRadius to Color.Transparent,
        ReactorGeometry.RING / ringShadowRadius to Color.Black,
        1f to Color.Transparent,
        center = Center,
        radius = ringShadowRadius
    )
    private val bandShadeBrush = bandGradient(
        ReactorGeometry.RING_INNER to Color.Black.copy(alpha = 0.72f),
        ReactorGeometry.RING_INNER + 3f to Color.Black.copy(alpha = 0.12f),
        ReactorGeometry.RING_INNER + 5.5f to Color.White.copy(alpha = 0.24f),
        ReactorGeometry.RING_INNER + 8f to Color.White.copy(alpha = 0f),
        ReactorGeometry.RING_OUTER - 7f to Color.Black.copy(alpha = 0f),
        ReactorGeometry.RING_OUTER - 3.5f to Color.Black.copy(alpha = 0.25f),
        ReactorGeometry.RING_OUTER to Color.Black.copy(alpha = 0.78f)
    )
    private val slotTubeBrush = Brush.radialGradient(
        0.868f to Color.Black.copy(alpha = 0.55f),
        0.888f to Color.Black.copy(alpha = 0.1f),
        0.91f to Color.White.copy(alpha = 0.42f),
        0.928f to Color.White.copy(alpha = 0.08f),
        0.965f to Color.Black.copy(alpha = 0.12f),
        1f to Color.Black.copy(alpha = 0.6f),
        center = Center,
        radius = ReactorGeometry.SLOT_OUTER
    )
    private val bandEdgeBrush = bandGradient(
        ReactorGeometry.RING_INNER to Color.Black.copy(alpha = 0.6f),
        ReactorGeometry.SLOT_INNER + 1.5f to Color.Black.copy(alpha = 0f),
        ReactorGeometry.SLOT_OUTER - 1.5f to Color.Black.copy(alpha = 0f),
        ReactorGeometry.RING_OUTER to Color.Black.copy(alpha = 0.65f)
    )
    private val coreGlowRadius = CORE + 34f
    private val coreGlowBrush = Brush.radialGradient(
        0f to colors.energy.copy(alpha = 0.55f),
        1f to colors.energy.copy(alpha = 0f),
        center = Center,
        radius = coreGlowRadius
    )
    private val lensBrush = Brush.radialGradient(
        0f to colors.lens[0],
        0.6f to colors.lens[1],
        1f to colors.lens[2],
        center = Offset(CENTER, CENTER + 16f),
        radius = CORE
    )
    private val centerDarkBrush = Brush.radialGradient(
        0f to colors.centerDark.copy(alpha = 0.75f),
        0.45f to colors.centerDark.copy(alpha = 0.5f),
        0.8f to colors.centerDark.copy(alpha = 0.15f),
        1f to colors.centerDark.copy(alpha = 0f),
        center = Center,
        radius = CORE
    )
    private val fullBrush = Brush.radialGradient(
        0f to colors.full[0],
        0.45f to colors.full[1],
        0.8f to colors.full[2],
        1f to colors.full[3],
        center = Center,
        radius = CORE
    )
    private val lensShadeBrush = Brush.radialGradient(
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.45f),
        center = Offset(CENTER, CENTER - CORE * 0.25f),
        radius = CORE
    )
    private val lensDarkBrush = Brush.radialGradient(
        0.86f to Color.Transparent,
        0.975f to Color.Black.copy(alpha = 0.35f),
        1f to Color.Black.copy(alpha = 0.7f),
        center = Center,
        radius = CORE
    )
    private val lensEdgeBrush = Brush.radialGradient(
        0.74f to colors.energy.copy(alpha = 0f),
        0.93f to colors.energy.copy(alpha = 0.16f),
        0.975f to Color.Black.copy(alpha = 0.35f),
        1f to Color.Black.copy(alpha = 0.7f),
        center = Center,
        radius = CORE
    )
    private val sheenCenterY = CENTER - CORE * 0.52f
    private val sheenRx = CORE * 0.7f
    private val sheenRy = CORE * 0.42f
    private val sheenBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.22f),
        0.7f to Color.White.copy(alpha = 0.03f),
        1f to Color.Transparent,
        startY = sheenCenterY - sheenRy,
        endY = sheenCenterY + sheenRy
    )
    private val specRadius = CORE - 7f
    private val specBrush = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0f),
        0.45f to Color.White.copy(alpha = 0.85f),
        1f to Color.White.copy(alpha = 0f),
        start = ReactorGeometry.polar(specRadius, SPEC_FROM),
        end = ReactorGeometry.polar(specRadius, SPEC_TO)
    )
    private val specSecondRadius = CORE - 6f
    private val specSecondBrush = Brush.linearGradient(
        0f to colors.specTint.copy(alpha = 0f),
        0.5f to colors.specTint.copy(alpha = 0.45f),
        1f to colors.specTint.copy(alpha = 0f),
        start = ReactorGeometry.polar(specSecondRadius, SPEC_SECOND_FROM),
        end = ReactorGeometry.polar(specSecondRadius, SPEC_SECOND_TO)
    )
    private val liquidRimGlow = colors.liquidRim.map { ringGlow(it, GLASS_RIM, 5f) }

    private val cellPath = Path().apply { addOval(Rect(Center, CORE)) }
    private val slotsPath = Path()
    private val slotsLowPath = Path()
    private val bandBrushPath = Path()
    private val tabsPath = Path()

    private val plainLayer = Paint()
    private val crestLayer = thresholdLayer(colors.waveBackCrest)
    private val bodyLayer = thresholdLayer(colors.waveFront)
    private val maskLayer = thresholdLayer(Color.White).apply { blendMode = BlendMode.DstIn }
    private var sprite: ImageBitmap? = null
    private var spriteUnit = 0f

    private val flowStrokeA = floatArrayOf(5f, 23f)
    private val flowStrokeB = floatArrayOf(2f, 26f)

    init {
        val step = 360f / ReactorGeometry.SLOT_COUNT
        val inner = ReactorGeometry.SLOT_INNER
        val outer = ReactorGeometry.SLOT_OUTER
        repeat(ReactorGeometry.SLOT_COUNT) { i ->
            val from = i * step + ReactorGeometry.SLOT_GAP
            val to = (i + 1) * step - ReactorGeometry.SLOT_GAP
            slotsPath.addSector(inner, outer, from, to)
            val glint = ReactorGeometry.SLOT_INNER + 4f
            slotsLowPath.addArc(Rect(Center, glint), from + 2.2f - 90f, (to - from) * 0.35f)
        }
        var r = ReactorGeometry.RING_INNER + 1f
        while (r < ReactorGeometry.RING_OUTER - 0.5f) {
            bandBrushPath.addOval(Rect(Center, r))
            r += 1.6f
        }
        val tabStep = 360f / ReactorGeometry.GLASS_TAB_COUNT
        repeat(ReactorGeometry.GLASS_TAB_COUNT) { i ->
            val a = i * tabStep + tabStep / 2f
            val p1 = ReactorGeometry.polar(CORE + 0.8f, a - 2.2f)
            val p2 = ReactorGeometry.polar(GLASS_OUTER - 0.8f, a - 2.2f)
            val p3 = ReactorGeometry.polar(GLASS_OUTER - 0.8f, a + 2.2f)
            val p4 = ReactorGeometry.polar(CORE + 0.8f, a + 2.2f)
            tabsPath.moveTo(p1.x, p1.y)
            tabsPath.lineTo(p2.x, p2.y)
            tabsPath.lineTo(p3.x, p3.y)
            tabsPath.lineTo(p4.x, p4.y)
            tabsPath.close()
        }
    }

    fun DrawScope.drawReactor(motion: ReactorMotion, drive: ReactorDrive, progress: Float, body: ReactorBody) {
        val unit = size.width / ReactorGeometry.VIEWBOX
        val layer = energyLayer(drive, motion.flicker)
        scale(unit, unit, pivot = Offset.Zero) {
            drawFoundation(layer, progress, motion)
        }
        drawImage(body.image, topLeft = Offset(-body.pad, -body.pad))
        scale(unit, unit, pivot = Offset.Zero) {
            drawCoreParts(layer, drive, progress, motion, unit)
        }
    }

    private fun energyLayer(drive: ReactorDrive, flicker: Float): Paint? = when (drive.phase) {
        ReactorPhase.FULL -> null
        ReactorPhase.OFF -> grayLayer
        ReactorPhase.RUN -> flickerLayer.also { it.alpha = flicker }
    }

    private inline fun DrawScope.energy(layer: Paint?, block: DrawScope.() -> Unit) {
        if (layer == null) {
            block()
            return
        }
        drawContext.canvas.saveLayer(layerBounds, layer)
        block()
        drawContext.canvas.restore()
    }

    private fun DrawScope.drawFoundation(layer: Paint?, progress: Float, motion: ReactorMotion) {
        energy(layer) {
            drawCircle(haloBrush, radius = haloRadius, center = Center, alpha = (0.04f + 0.22f * progress) * haloScale)
        }
        drawCircle(shadowBrush, radius = shadowRadius, center = shadowCenter)
        drawCircle(colors.bezelRim, radius = ReactorGeometry.BEZEL, center = Center)
        drawCircle(colors.bezelChamfer, radius = ReactorGeometry.CHAMFER, center = Center)
        drawCircle(colors.bezelFace, radius = ReactorGeometry.FACE, center = Center)
        drawCircle(colors.housingVoid, radius = ReactorGeometry.HOUSING, center = Center)
        drawCircle(colors.tubeDark, radius = ReactorGeometry.TUBE, center = Center, style = Stroke(ReactorGeometry.TUBE_WIDTH))
        energy(layer) {
            drawCircle(colors.energy, radius = ReactorGeometry.TUBE, center = Center, alpha = 0.8f, style = Stroke(ReactorGeometry.TUBE_WIDTH))
            drawCircle(colors.channelMid, radius = ReactorGeometry.TUBE, center = Center, style = Stroke(ReactorGeometry.TUBE_WIDTH - 3f))
            drawCircle(colors.energy, radius = ReactorGeometry.TUBE, center = Center, style = Stroke(ReactorGeometry.TUBE_WIDTH - 13f))
            drawCircle(
                colors.flowLight,
                radius = ReactorGeometry.TUBE,
                center = Center,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(flowStrokeA, FLOW_PERIOD * (1f - motion.flowA))
                )
            )
            drawCircle(
                Color.White,
                radius = ReactorGeometry.TUBE,
                center = Center,
                alpha = 0.7f,
                style = Stroke(
                    width = 1.4f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(flowStrokeB, (12f + FLOW_PERIOD * (1f - motion.flowB)) % FLOW_PERIOD)
                )
            )
        }
    }

    private fun DrawScope.drawCoreParts(layer: Paint?, drive: ReactorDrive, progress: Float, motion: ReactorMotion, unit: Float) {
        energy(layer) {
            drawCircle(baseHaloBrush, radius = ReactorGeometry.BASE_GLOW + 10f, center = Center, alpha = 0.35f)
            drawCircle(colors.baseGlow, radius = ReactorGeometry.BASE_GLOW, center = Center, style = Stroke(3f))
        }

        drawCircle(ringShadowBrush, radius = ringShadowRadius, center = Center, alpha = 0.35f)
        val bandWidth = ReactorGeometry.RING_OUTER - ReactorGeometry.RING_INNER
        drawCircle(colors.band, radius = ReactorGeometry.RING, center = Center, style = Stroke(bandWidth))
        drawCircle(bandShadeBrush, radius = ReactorGeometry.RING, center = Center, style = Stroke(bandWidth))
        drawPath(bandBrushPath, Color.White, alpha = 0.08f, style = Stroke(0.4f))
        rotate(motion.angle, pivot = Center) {
            energy(layer) {
                drawPath(slotsPath, colors.slot)
                drawPath(slotsPath, colors.slotLit, alpha = 0.08f + 0.85f * progress)
            }
            drawPath(slotsPath, slotTubeBrush)
            drawPath(slotsLowPath, Color.White, alpha = 0.55f, style = Stroke(1.2f, cap = StrokeCap.Round))
            drawPath(slotsPath, colors.slotOutline, style = Stroke(1.2f, join = StrokeJoin.Round))
        }
        drawCircle(bandEdgeBrush, radius = ReactorGeometry.RING, center = Center, style = Stroke(bandWidth))
        drawBandLip(ReactorGeometry.RING_OUTER, -1f)
        drawBandLip(ReactorGeometry.RING_INNER, 1f)

        energy(layer) {
            val breath = 0.875f - 0.125f * cos(2f * PI.toFloat() * motion.breath)
            drawCircle(coreGlowBrush, radius = coreGlowRadius, center = Center, alpha = (0.2f + 0.8f * progress) * breath)
        }

        drawLens(drive, motion, unit)
    }

    private fun DrawScope.drawBandLip(edge: Float, inward: Float) {
        drawCircle(colors.ringEdge, radius = edge, center = Center, style = Stroke(1.6f))
        drawCircle(colors.bevel, radius = edge + inward * 1.3f, center = Center, alpha = 0.55f, style = Stroke(1.1f))
        drawCircle(colors.bevelHighlight, radius = edge + inward * 1.3f, center = Center, alpha = 0.3f, style = Stroke(0.45f))
        drawCircle(Color.Black, radius = edge + inward * 2.4f, center = Center, alpha = 0.4f, style = Stroke(0.9f))
    }

    private fun DrawScope.drawLens(drive: ReactorDrive, motion: ReactorMotion, unit: Float) {
        val full = motion.full.coerceIn(0f, 1f)
        val waves = 1f - full
        val liquid = motion.liquid
        val sprite = spriteFor(unit)

        drawCircle(lensBrush, radius = CORE, center = Center)
        drawCircle(Color.Black, radius = CORE, center = Center, alpha = motion.dim.coerceIn(0f, 1f))
        if (waves > 0.001f && liquid.count > 0) {
            clipPath(cellPath) {
                crestLayer.alpha = waves
                bodyLayer.alpha = waves
                particles(liquid, sprite, unit, crestLayer, liquid.upX * CREST_LIFT, liquid.upY * CREST_LIFT)
                particles(liquid, sprite, unit, bodyLayer, 0f, 0f)
            }
            drawCircle(centerDarkBrush, radius = CORE, center = Center, alpha = waves)
        }
        if (full > 0.001f) drawCircle(fullBrush, radius = CORE, center = Center, alpha = full)

        val level = drive.rimLevel.coerceIn(0f, 3f)
        val low = level.toInt().coerceAtMost(2)
        val high = low + 1
        val blend = level - low
        val tint = lerp(LIQUID_TINT[low], LIQUID_TINT[high], blend)
        val rimColor = androidx.compose.ui.graphics.lerp(colors.liquidRim[low], colors.liquidRim[high], blend)
        val glassMid = (CORE + GLASS_OUTER) / 2f
        val glassWidth = GLASS_OUTER - CORE

        drawCircle(lensShadeBrush, radius = CORE, center = Center)
        drawCircle(lensDarkBrush, radius = CORE, center = Center)
        clipPath(cellPath) {
            drawOval(
                sheenBrush,
                topLeft = Offset(CENTER - sheenRx, sheenCenterY - sheenRy),
                size = Size(sheenRx * 2f, sheenRy * 2f),
                alpha = drive.sheen
            )
        }
        drawArcDegrees(specBrush, specRadius, SPEC_FROM, SPEC_TO, 3.2f)
        drawArcDegrees(specSecondBrush, specSecondRadius, SPEC_SECOND_FROM, SPEC_SECOND_TO, 1.6f)
        drawCircle(colors.lensRimDark, radius = CORE, center = Center, style = Stroke(2.4f))
        drawCircle(colors.glassRing, radius = glassMid, center = Center, alpha = 0.13f, style = Stroke(glassWidth))
        drawPath(tabsPath, colors.glassTab, alpha = 0.08f)
        drawPath(tabsPath, colors.glassTab, alpha = 0.28f, style = Stroke(0.6f))
        drawArc(
            color = Color.White,
            startAngle = 112f - 90f,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = Offset(CENTER - (CORE + 2.3f), CENTER - (CORE + 2.3f)),
            size = Size((CORE + 2.3f) * 2f, (CORE + 2.3f) * 2f),
            alpha = 0.18f,
            style = Stroke(1.2f, cap = StrokeCap.Round)
        )
        drawCircle(colors.lensRimDark, radius = GLASS_OUTER, center = Center, style = Stroke(1.2f))
        drawCircle(colors.rimOuterLight, radius = GLASS_OUTER, center = Center, alpha = 0.6f, style = Stroke(0.7f))
        drawCircle(colors.lensRimDark, radius = CORE, center = Center, style = Stroke(1f))
        drawCircle(colors.rimGray, radius = GLASS_RIM, center = Center, alpha = 0.85f, style = Stroke(1.4f))
        drawCircle(colors.rimLight, radius = GLASS_RIM, center = Center, alpha = 0.45f, style = Stroke(0.6f))

        val masked = full <= 0.5f
        if (masked && liquid.count == 0) return
        if (masked) drawContext.canvas.saveLayer(layerBounds, plainLayer)
        val glow = lerp(LIQUID_GLOW[low], LIQUID_GLOW[high], blend)
        drawCircle(lensEdgeBrush, radius = CORE, center = Center, alpha = tint)
        drawCircle(rimColor, radius = glassMid, center = Center, alpha = tint, style = Stroke(glassWidth))
        drawCircle(liquidRimGlow[low], radius = GLASS_RIM + 5f, center = Center, alpha = glow * (1f - blend))
        drawCircle(liquidRimGlow[high], radius = GLASS_RIM + 5f, center = Center, alpha = glow * blend)
        drawCircle(rimColor, radius = GLASS_RIM, center = Center, alpha = lerp(LIQUID_LINE[low], LIQUID_LINE[high], blend), style = Stroke(1.6f))
        if (masked) {
            particles(liquid, sprite, unit, maskLayer, 0f, 0f)
            drawContext.canvas.restore()
        }
    }

    private fun DrawScope.particles(liquid: LiquidSim, sprite: ImageBitmap, unit: Float, layer: Paint, offsetX: Float, offsetY: Float) {
        drawContext.canvas.saveLayer(layerBounds, layer)
        scale(1f / unit, 1f / unit, pivot = Offset.Zero) {
            val side = sprite.width
            val half = side / 2f
            val spriteSize = IntSize(side, side)
            for (i in 0 until liquid.count) {
                val x = ((liquid.x(i) + offsetX) * unit - half).roundToInt()
                val y = ((liquid.y(i) + offsetY) * unit - half).roundToInt()
                drawImage(sprite, dstOffset = IntOffset(x, y), dstSize = spriteSize)
            }
        }
        drawContext.canvas.restore()
    }

    private fun spriteFor(unit: Float): ImageBitmap {
        val cached = sprite
        if (cached != null && spriteUnit == unit) return cached
        val radius = LiquidSim.SPRITE_RADIUS * unit
        val side = ceil(radius * 2f).toInt().coerceAtLeast(4)
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(side * side)
        val c = side / 2f
        for (y in 0 until side) {
            for (x in 0 until side) {
                val d = (hypot(x + 0.5f - c, y + 0.5f - c) / radius).coerceIn(0f, 1f)
                val a = ((1f - d) * (1f - d) * 255f).roundToInt()
                pixels[y * side + x] = (a shl 24) or 0x00FFFFFF
            }
        }
        bitmap.setPixels(pixels, 0, side, 0, 0, side, side)
        return bitmap.asImageBitmap().also {
            sprite = it
            spriteUnit = unit
        }
    }

    private fun DrawScope.drawArcDegrees(brush: Brush, radius: Float, from: Float, to: Float, width: Float) {
        drawArc(
            brush = brush,
            startAngle = from - 90f,
            sweepAngle = to - from,
            useCenter = false,
            topLeft = Offset(CENTER - radius, CENTER - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width, cap = StrokeCap.Round)
        )
    }

    private fun bandGradient(vararg stops: Pair<Float, Color>): Brush {
        val outer = ReactorGeometry.RING_OUTER
        return Brush.radialGradient(
            *stops.map { (radius, color) -> radius / outer to color }.toTypedArray(),
            center = Center,
            radius = outer
        )
    }

    private fun thresholdLayer(color: Color): Paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, color.red * 255f,
                    0f, 0f, 0f, 0f, color.green * 255f,
                    0f, 0f, 0f, 0f, color.blue * 255f,
                    0f, 0f, 0f, THRESHOLD_GAIN, -THRESHOLD * THRESHOLD_GAIN * 255f
                )
            )
        )
    }

    private fun ringGlow(color: Color, radius: Float, spread: Float): Brush {
        val outer = radius + spread
        return Brush.radialGradient(
            (radius - spread) / outer to color.copy(alpha = 0f),
            radius / outer to color,
            1f to color.copy(alpha = 0f),
            center = Center,
            radius = outer
        )
    }

    private fun Path.addSector(inner: Float, outer: Float, from: Float, to: Float) {
        val start = ReactorGeometry.polar(outer, from)
        val end = ReactorGeometry.polar(inner, to)
        moveTo(start.x, start.y)
        arcTo(Rect(Center, outer), from - 90f, to - from, false)
        lineTo(end.x, end.y)
        arcTo(Rect(Center, inner), to - 90f, -(to - from), false)
        close()
    }

    private companion object {
        const val FLOW_PERIOD = 28f
        const val CREST_LIFT = 1.8f
        const val THRESHOLD = 0.42f
        const val THRESHOLD_GAIN = 12f
        const val SPEC_FROM = 284f
        const val SPEC_TO = 342f
        const val SPEC_SECOND_FROM = 118f
        const val SPEC_SECOND_TO = 152f

        val LIQUID_TINT = floatArrayOf(0.18f, 0.2f, 0.24f, 0.28f)
        val LIQUID_GLOW = floatArrayOf(0.12f, 0.16f, 0.22f, 0.3f)
        val LIQUID_LINE = floatArrayOf(0.7f, 0.75f, 0.85f, 0.9f)
    }
}
