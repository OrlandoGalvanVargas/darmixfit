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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.darmix.darmixfit.ui.theme.ReactorColors
import com.darmix.darmixfit.ui.today.ReactorGeometry.CENTER
import com.darmix.darmixfit.ui.today.ReactorGeometry.CORE
import com.darmix.darmixfit.ui.today.ReactorGeometry.Center
import com.darmix.darmixfit.ui.today.ReactorGeometry.MESH
import com.darmix.darmixfit.ui.today.ReactorGeometry.RIM_INNER
import com.darmix.darmixfit.ui.today.ReactorGeometry.RIM_OUTER
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
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
        radius = ReactorGeometry.SLOT_SHADE
    )
    private val bandEdgeBrush = bandGradient(
        ReactorGeometry.RING_INNER to Color.Black.copy(alpha = 0.6f),
        ReactorGeometry.RING_INNER + 5.5f to Color.Black.copy(alpha = 0f),
        ReactorGeometry.RING_OUTER - 5.5f to Color.Black.copy(alpha = 0f),
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
        center = Center,
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
    private val tubeShadeBrush = Brush.radialGradient(
        0.79f to Color.Black.copy(alpha = 0.7f),
        0.83f to Color.Black.copy(alpha = 0.1f),
        0.86f to Color.White.copy(alpha = 0.32f),
        0.88f to Color.White.copy(alpha = 0.04f),
        0.96f to Color.Black.copy(alpha = 0.2f),
        1f to Color.Black.copy(alpha = 0.75f),
        center = Center,
        radius = ReactorGeometry.TUBE + ReactorGeometry.TUBE_WIDTH / 2f
    )
    private val ringShadeBrush = Brush.radialGradient(
        0.9f to Color.Black.copy(alpha = 0f),
        0.955f to Color.Black.copy(alpha = 0.4f),
        0.98f to Color.Black.copy(alpha = 0.9f),
        1f to Color.Black,
        center = Center,
        radius = ReactorGeometry.RING_SHADE_REACH
    )
    private val meshCoreBrush = Brush.radialGradient(
        0f to colors.glowCore.copy(alpha = 0.85f),
        0.45f to colors.energy.copy(alpha = 0.35f),
        1f to colors.energy.copy(alpha = 0f),
        center = Center,
        radius = MESH
    )
    private val meshShadeBrush = Brush.radialGradient(
        0.6f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.65f),
        center = Center,
        radius = MESH
    )
    private val glowInner = MESH + 5f
    private val glowRadius = (glowInner + RIM_INNER) / 2f
    private val glowWidth = RIM_INNER - glowInner
    private val liquidGlowBrush = Brush.radialGradient(
        0f to colors.energy,
        glowInner / RIM_INNER to colors.energy,
        glowRadius / RIM_INNER to colors.glowCore,
        1f to colors.energy,
        center = Center,
        radius = RIM_INNER
    )
    private val hubBrush = steelBrush(ReactorGeometry.HUB + 3.2f)
    private val meshRimBrush = steelBrush(ReactorGeometry.MESH_RIM + 2.6f)
    private val spokeBrush = Brush.linearGradient(
        0f to colors.spoke[0],
        0.5f to colors.spoke[1],
        1f to colors.spoke[2],
        start = Offset(CENTER - 73f, CENTER - 81f),
        end = Offset(CENTER + 73f, CENTER + 43f)
    )
    private val rimShadeRadius = RIM_INNER + 0.6f + (RIM_OUTER - RIM_INNER) * 0.12f
    private val rimShadeWidth = (RIM_OUTER - RIM_INNER) * 0.24f

    private val cellPath = Path().apply { addOval(Rect(Center, CORE)) }
    private val meshClip = Path().apply { addOval(Rect(Center, MESH)) }
    private val rimPath = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(Rect(Center, RIM_OUTER))
        addOval(Rect(Center, RIM_INNER))
    }
    private val slotPaths = List(ReactorGeometry.SLOT_COUNT) { Path() }
    private val slotsPath = Path()
    private val slotsLowPath = Path()
    private val bandBrushPath = Path()
    private val spillPaths = List(ReactorGeometry.SPILL_BANDS) { Path() }
    private val meshDarkPath = Path()
    private val meshLinePath = Path()
    private val spokesPath = Path()
    private val spokesHighlightPath = Path()
    private val platesPath = Path()
    private val plateGroovePath = Path()
    private val plateHighPath = Path()
    private val plateLowPath = Path()
    private val screws = List(3) { ReactorGeometry.polar(ReactorGeometry.SCREW, PLATE_ANGLES[it]) }
    private val screwSlotPath = Path()
    private val screwBrushes = screws.map { p ->
        Brush.radialGradient(
            0f to colors.screw[0],
            0.5f to colors.screw[1],
            1f to colors.screw[2],
            center = Offset(p.x - 2f, p.y - 2.5f),
            radius = 12.6f
        )
    }

    private val plainLayer = Paint()
    private val crestLayer = thresholdLayer(colors.waveBackCrest)
    private val bodyLayer = thresholdLayer(colors.waveFront)
    private val maskLayer = thresholdLayer(Color.White).apply { blendMode = BlendMode.DstIn }
    private var sprite: ImageBitmap? = null
    private var spriteUnit = 0f

    private val flowStrokeA = floatArrayOf(5f, 23f)
    private val flowStrokeB = floatArrayOf(2f, 26f)

    init {
        val slotStep = 360f / ReactorGeometry.SLOT_COUNT
        repeat(ReactorGeometry.SLOT_COUNT) { i ->
            val mid = (i + 0.5f) * slotStep
            val from = mid - ReactorGeometry.SLOT_WIDTH / 2f
            val to = mid + ReactorGeometry.SLOT_WIDTH / 2f
            slotPaths[i].addSector(ReactorGeometry.SLOT_INNER, ReactorGeometry.SLOT_OUTER, from, to)
            slotsPath.addSector(ReactorGeometry.SLOT_INNER, ReactorGeometry.SLOT_OUTER, from, to)
            slotsLowPath.addArc(Rect(Center, ReactorGeometry.RING_INNER + 8f), from + 2.2f - 90f, ReactorGeometry.SLOT_WIDTH * 0.35f)
        }
        var r = ReactorGeometry.RING_INNER + 1f
        while (r < ReactorGeometry.RING_OUTER - 0.5f) {
            bandBrushPath.addOval(Rect(Center, r))
            r += 1.6f
        }
        val windowStep = 360f / ReactorGeometry.WINDOW_COUNT
        val windowHalf = ReactorGeometry.WINDOW_SEPARATION / 2f
        repeat(ReactorGeometry.WINDOW_COUNT) { i ->
            val start = i * windowStep + windowHalf
            val end = (i + 1) * windowStep - windowHalf
            repeat(ReactorGeometry.SPILL_BANDS) { k ->
                val near = k * SPILL_STEP * SPILL_SCALE
                val far = ((k + 1) * SPILL_STEP + 0.05f) * SPILL_SCALE
                spillPaths[k].addSector(ReactorGeometry.SPILL_INNER, ReactorGeometry.SPILL_OUTER, start + near, start + far)
                spillPaths[k].addSector(ReactorGeometry.SPILL_INNER, ReactorGeometry.SPILL_OUTER, end - far, end - near)
            }
        }
        var y = CENTER - 100f
        while (y < CENTER + 100f) {
            meshDarkPath.addRect(Rect(CENTER - 100f, y, CENTER + 100f, y + 2.1f))
            meshLinePath.addRect(Rect(CENTER - 100f, y + 2.1f, CENTER + 100f, y + 2.6f))
            y += ReactorGeometry.MESH_PERIOD
        }
        val half = ReactorGeometry.SPOKE_HALF
        SPOKE_ANGLES.forEach { a ->
            polygon(
                spokesPath,
                listOf(
                ReactorGeometry.radial(ReactorGeometry.HUB + 1f, -half, a),
                ReactorGeometry.radial(RIM_INNER + 1.5f, -half, a),
                ReactorGeometry.radial(RIM_INNER + 1.5f, half, a),
                ReactorGeometry.radial(ReactorGeometry.HUB + 1f, half, a)
                )
            )
            line(spokesHighlightPath, ReactorGeometry.radial(ReactorGeometry.HUB + 3f, -half + 0.6f, a), ReactorGeometry.radial(RIM_INNER, -half + 0.6f, a))
        }
        val r0 = ReactorGeometry.PLATE_INNER
        val r1 = ReactorGeometry.PLATE_OUTER
        val hw = ReactorGeometry.PLATE_HALF
        val ch = ReactorGeometry.PLATE_CUT
        PLATE_ANGLES.forEachIndexed { index, a ->
            polygon(
                platesPath,
                listOf(
                ReactorGeometry.radial(r0, -hw + ch, a),
                ReactorGeometry.radial(r0 + ch, -hw, a),
                ReactorGeometry.radial(r1 - ch, -hw, a),
                ReactorGeometry.radial(r1, -hw + ch, a),
                ReactorGeometry.radial(r1, hw - ch, a),
                ReactorGeometry.radial(r1 - ch, hw, a),
                ReactorGeometry.radial(r0 + ch, hw, a),
                ReactorGeometry.radial(r0, hw - ch, a)
                )
            )
            line(plateHighPath, ReactorGeometry.radial(r0 + ch + 0.8f, -hw + 0.8f, a), ReactorGeometry.radial(r1 - ch - 0.8f, -hw + 0.8f, a))
            line(plateLowPath, ReactorGeometry.radial(r0 + ch + 0.8f, hw - 0.8f, a), ReactorGeometry.radial(r1 - ch - 0.8f, hw - 0.8f, a))
            line(plateGroovePath, ReactorGeometry.radial(r0 + 9f, -hw + 4f, a), ReactorGeometry.radial(r0 + 9f, hw - 4f, a))
            line(plateGroovePath, ReactorGeometry.radial(r0 + 12f, -hw + 4f, a), ReactorGeometry.radial(r0 + 12f, hw - 4f, a))
            val c = screws[index]
            val turn = Math.toRadians((a + 35f - 90f).toDouble())
            val ex = (SCREW_CROSS * cos(turn)).toFloat()
            val ey = (SCREW_CROSS * kotlin.math.sin(turn)).toFloat()
            line(screwSlotPath, Offset(c.x - ex, c.y - ey), Offset(c.x + ex, c.y + ey))
            line(screwSlotPath, Offset(c.x + ey, c.y - ex), Offset(c.x - ey, c.y + ex))
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
            drawHardware()
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
        val haloRadius = ReactorGeometry.BEZEL + 1f + 21f * progress.pow(1.3f)
        energy(layer) {
            drawCircle(
                Brush.radialGradient(
                    0f to colors.energy,
                    ReactorGeometry.BEZEL / haloRadius to colors.energy,
                    1f to Color.Transparent,
                    center = Center,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = Center,
                alpha = (0.02f + 0.24f * progress.pow(1.6f)) * haloScale
            )
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
        drawCircle(tubeShadeBrush, radius = ReactorGeometry.TUBE, center = Center, style = Stroke(ReactorGeometry.TUBE_WIDTH))
        val spill = ((progress - SPILL_START) / (1f - SPILL_START)).coerceIn(0f, 1f).pow(1.4f)
        if (spill > 0.001f) {
            energy(layer) {
                spillPaths.forEachIndexed { k, path ->
                    drawPath(path, colors.spill, alpha = spill * 0.8f * exp(-k / 2.6f), blendMode = BlendMode.Screen)
                }
                drawPath(spillPaths[0], colors.energy, alpha = spill * 0.45f, style = Stroke(3f), blendMode = BlendMode.Screen)
                drawPath(spillPaths[1], colors.energy, alpha = spill * 0.3f, style = Stroke(3f), blendMode = BlendMode.Screen)
            }
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
        val lit = progress * ReactorGeometry.SLOT_COUNT
        rotate(motion.angle, pivot = Center) {
            energy(layer) {
                drawPath(slotsPath, colors.slot)
                slotPaths.forEachIndexed { i, path ->
                    val level = (lit - i).coerceIn(0f, 1f)
                    if (level <= 0f) return@forEachIndexed
                    val strength = if (level >= 0.999f) 1f else level
                    drawPath(path, colors.slotGlow, alpha = 0.12f * strength, style = Stroke(9f, join = StrokeJoin.Round))
                    drawPath(path, colors.slotGlow, alpha = 0.22f * strength, style = Stroke(5f, join = StrokeJoin.Round))
                    drawPath(path, colors.slotLit, alpha = if (level >= 0.999f) 0.97f else level * 0.93f)
                }
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

        drawLens(layer, drive, progress, motion, unit)
    }

    private fun DrawScope.drawBandLip(edge: Float, inward: Float) {
        drawCircle(colors.ringEdge, radius = edge, center = Center, style = Stroke(1.6f))
        drawCircle(colors.bevel, radius = edge + inward * 1.3f, center = Center, alpha = 0.55f, style = Stroke(1.1f))
        drawCircle(colors.bevelHighlight, radius = edge + inward * 1.3f, center = Center, alpha = 0.3f, style = Stroke(0.45f))
        drawCircle(Color.Black, radius = edge + inward * 2.4f, center = Center, alpha = 0.4f, style = Stroke(0.9f))
    }

    private fun DrawScope.drawLens(layer: Paint?, drive: ReactorDrive, progress: Float, motion: ReactorMotion, unit: Float) {
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
        val blend = level - low
        val tint = lerp(LIQUID_TINT[low], LIQUID_TINT[low + 1], blend)

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
        drawCircle(colors.lensRimDark, radius = CORE, center = Center, style = Stroke(2.4f))
        energy(layer) {
            drawCircle(meshCoreBrush, radius = MESH, center = Center, alpha = 0.2f + 0.8f * progress)
        }

        val masked = full <= 0.5f
        if (!masked || liquid.count > 0) {
            if (masked) drawContext.canvas.saveLayer(layerBounds, plainLayer)
            drawCircle(lensEdgeBrush, radius = CORE, center = Center, alpha = tint)
            energy(layer) {
                drawCircle(colors.liquidGlow, radius = glowRadius, center = Center, alpha = 0.18f, style = Stroke(glowWidth + 8f))
                drawCircle(colors.liquidGlow, radius = glowRadius, center = Center, alpha = 0.3f, style = Stroke(glowWidth + 3f))
                drawCircle(liquidGlowBrush, radius = glowRadius, center = Center, alpha = 0.95f, style = Stroke(glowWidth))
            }
            if (masked) {
                particles(liquid, sprite, unit, maskLayer, 0f, 0f)
                drawContext.canvas.restore()
            }
        }

        drawPath(spokesPath, Color.Black, alpha = 0.3f, style = Stroke(3f, join = StrokeJoin.Round))
        drawCircle(Color.Black, radius = ReactorGeometry.HUB, center = Center, alpha = 0.3f, style = Stroke(9f))
        drawPath(spokesPath, spokeBrush)
        drawPath(spokesPath, colors.steelEdge, style = Stroke(0.9f, join = StrokeJoin.Round))
        drawPath(spokesHighlightPath, colors.steelLight, alpha = 0.45f, style = Stroke(0.6f))
        drawSteelRing(ReactorGeometry.HUB, 6.4f, hubBrush, ReactorGeometry.HUB + 2.8f, 0.6f, ReactorGeometry.HUB - 2.8f, 0.8f)

        clipPath(meshClip) {
            MESH_ANGLES.forEachIndexed { index, angle ->
                rotate(angle, pivot = Center) {
                    drawPath(meshDarkPath, colors.meshDark)
                    drawPath(meshLinePath, colors.meshLine, alpha = MESH_LINE_ALPHA[index])
                }
            }
            drawCircle(meshShadeBrush, radius = MESH, center = Center)
        }
        drawCircle(Color.Black, radius = ReactorGeometry.MESH_RIM, center = Center, alpha = 0.3f, style = Stroke(7f))
        drawSteelRing(ReactorGeometry.MESH_RIM, 5.2f, meshRimBrush, MESH + 5f, 0.7f, MESH + 0.4f, 1f)

        drawPath(rimPath, colors.glassRim)
        clipPath(rimPath) {
            drawCircle(Color.Black, radius = rimShadeRadius, center = Center, alpha = 0.72f, style = Stroke(rimShadeWidth))
        }
        drawCircle(colors.lensRimDark, radius = RIM_INNER, center = Center, style = Stroke(1.2f))
        drawCircle(colors.lensRimDark, radius = RIM_OUTER, center = Center, style = Stroke(1.2f))
    }

    private fun DrawScope.drawSteelRing(
        radius: Float,
        width: Float,
        brush: Brush,
        lightRadius: Float,
        lightWidth: Float,
        edgeRadius: Float,
        edgeWidth: Float
    ) {
        drawCircle(colors.steelBase, radius = radius, center = Center, style = Stroke(width))
        drawCircle(brush, radius = radius, center = Center, alpha = 0.8f, style = Stroke(width))
        drawCircle(colors.steelLight, radius = lightRadius, center = Center, alpha = 0.45f, style = Stroke(lightWidth))
        drawCircle(colors.steelEdge, radius = edgeRadius, center = Center, style = Stroke(edgeWidth))
    }

    private fun DrawScope.drawHardware() {
        drawCircle(Color.Black, radius = ReactorGeometry.GROOVE, center = Center, style = Stroke(4f))
        drawCircle(ringShadeBrush, radius = ReactorGeometry.RING_SHADE, center = Center, style = Stroke(12f))
        drawCircle(Color.Black, radius = ReactorGeometry.RING_INNER + 1f, center = Center, alpha = 0.85f, style = Stroke(4f))
        drawCircle(Color.Black, radius = ReactorGeometry.RING_OUTER - 1f, center = Center, alpha = 0.85f, style = Stroke(4.5f))

        translate(2.2f, 3f) {
            drawPath(platesPath, Color.Black, alpha = 0.32f)
            drawPath(platesPath, Color.Black, alpha = 0.14f, style = Stroke(4f, join = StrokeJoin.Round))
        }
        drawPath(platesPath, colors.plate)
        drawPath(platesPath, colors.steelEdge, style = Stroke(1.3f, join = StrokeJoin.Round))
        drawPath(plateGroovePath, colors.steelEdge, alpha = 0.7f, style = Stroke(1f))
        drawPath(plateHighPath, colors.plateHighlight, alpha = 0.25f, style = Stroke(0.8f, cap = StrokeCap.Round))
        drawPath(plateLowPath, Color.Black, alpha = 0.6f, style = Stroke(1f, cap = StrokeCap.Round))
        screws.forEachIndexed { i, p ->
            drawCircle(colors.steelEdge, radius = 9.3f, center = p)
            drawCircle(screwBrushes[i], radius = 8.4f, center = p)
            drawCircle(colors.screwRim, radius = 8.4f, center = p, alpha = 0.3f, style = Stroke(0.5f))
        }
        drawPath(screwSlotPath, colors.screwSlot, style = Stroke(1.7f, cap = StrokeCap.Round))
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

    private fun steelBrush(extent: Float): Brush = Brush.linearGradient(
        0f to colors.steel[0],
        0.45f to colors.steel[1],
        1f to colors.steel[2],
        start = Offset(CENTER - extent, CENTER - extent),
        end = Offset(CENTER + extent, CENTER + extent)
    )

    private fun polygon(path: Path, points: List<Offset>) {
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        path.close()
    }

    private fun line(path: Path, from: Offset, to: Offset) {
        path.moveTo(from.x, from.y)
        path.lineTo(to.x, to.y)
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
        const val SPILL_START = 0.45f
        const val SPILL_STEP = 0.42f
        const val SPILL_SCALE = 1.6f
        const val SCREW_CROSS = 4.9f

        val PLATE_ANGLES = floatArrayOf(60f, 180f, 300f)
        val SPOKE_ANGLES = floatArrayOf(0f, 120f, 240f)
        val MESH_ANGLES = floatArrayOf(0f, 60f, 120f)
        val MESH_LINE_ALPHA = floatArrayOf(0.35f, 0.3f, 0.25f)
        val LIQUID_TINT = floatArrayOf(0.18f, 0.2f, 0.24f, 0.28f)
    }
}
