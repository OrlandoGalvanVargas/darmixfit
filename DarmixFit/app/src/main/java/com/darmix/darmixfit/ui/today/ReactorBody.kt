package com.darmix.darmixfit.ui.today

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.darmix.darmixfit.ui.theme.ReactorColors
import com.darmix.darmixfit.ui.today.ReactorGeometry.CENTER
import kotlin.math.ceil

internal class ReactorBody(val image: ImageBitmap, val pad: Float)

private const val BODY_PAD = 12f
private const val HOUSING_SHADOW_BLUR = 3.6f
private const val HOUSING_SHADOW_DROP = 1.5f
private const val BRUSH_STEP = 0.85f

internal fun buildReactorBody(sizePx: Int, colors: ReactorColors): ReactorBody {
    val unit = sizePx / ReactorGeometry.VIEWBOX
    val pad = ceil(BODY_PAD * unit).toInt()
    val side = sizePx + pad * 2
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.translate(pad.toFloat(), pad.toFloat())
    canvas.scale(unit, unit)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    drawTubeShade(canvas, paint)

    val windows = windowsPath()
    val housing = Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addCircle(CENTER, CENTER, ReactorGeometry.HOUSING, Path.Direction.CW)
        addCircle(CENTER, CENTER, ReactorGeometry.HOUSING_INNER, Path.Direction.CW)
        addPath(windows)
    }

    paint.style = Paint.Style.FILL
    paint.color = Color.Black.argb(0.85f)
    paint.maskFilter = BlurMaskFilter(HOUSING_SHADOW_BLUR, BlurMaskFilter.Blur.NORMAL)
    canvas.save()
    canvas.translate(0f, HOUSING_SHADOW_DROP)
    canvas.drawPath(housing, paint)
    canvas.restore()
    paint.maskFilter = null

    paint.color = android.graphics.Color.BLACK
    paint.shader = RadialGradient(
        CENTER,
        CENTER,
        ReactorGeometry.HOUSING,
        colors.metal.map { it.toArgb() }.toIntArray(),
        floatArrayOf(0.73f, 0.8f, 0.9f, 0.97f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(housing, paint)
    paint.shader = null

    canvas.save()
    canvas.clipPath(housing)
    paint.style = Paint.Style.STROKE
    var seed = 7
    var radius = ReactorGeometry.HOUSING_INNER + 0.6f
    while (radius < ReactorGeometry.HOUSING) {
        seed = (seed * 9301 + 49297) % 233280
        if (seed / 233280f < 0.5f) {
            paint.strokeWidth = 0.5f
            paint.color = colors.brushLight.argb(0.10f)
        } else {
            paint.strokeWidth = 0.6f
            paint.color = Color.Black.argb(0.22f)
        }
        canvas.drawCircle(CENTER, CENTER, radius, paint)
        radius += BRUSH_STEP
    }
    paint.strokeJoin = Paint.Join.ROUND
    paint.strokeWidth = 2.6f
    paint.color = colors.bevel.argb(0.5f)
    canvas.drawPath(housing, paint)
    paint.strokeWidth = 0.9f
    paint.color = colors.bevelHighlight.argb(0.35f)
    canvas.drawPath(housing, paint)
    canvas.restore()

    paint.strokeWidth = 1.6f
    paint.color = colors.windowRim.toArgb()
    canvas.drawPath(windows, paint)

    paint.strokeWidth = 1.8f
    paint.color = colors.housingEdge.toArgb()
    canvas.drawCircle(CENTER, CENTER, ReactorGeometry.HOUSING_EDGE, paint)
    canvas.drawCircle(CENTER, CENTER, ReactorGeometry.HOUSING_INNER_EDGE, paint)
    paint.strokeWidth = 1.4f
    paint.color = colors.lip.toArgb()
    canvas.drawCircle(CENTER, CENTER, ReactorGeometry.HOUSING, paint)

    paint.style = Paint.Style.FILL
    paint.color = colors.wellEdge.toArgb()
    canvas.drawCircle(CENTER, CENTER, ReactorGeometry.HOUSING_INNER, paint)
    paint.shader = RadialGradient(
        CENTER,
        CENTER,
        ReactorGeometry.WELL,
        colors.well.map { it.toArgb() }.toIntArray(),
        floatArrayOf(0f, 0.7f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(CENTER, CENTER, ReactorGeometry.WELL, paint)
    paint.shader = null

    return ReactorBody(bitmap.asImageBitmap(), pad.toFloat())
}

private fun drawTubeShade(canvas: Canvas, paint: Paint) {
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = ReactorGeometry.TUBE_WIDTH
    paint.shader = RadialGradient(
        CENTER,
        CENTER,
        ReactorGeometry.TUBE + ReactorGeometry.TUBE_WIDTH / 2f,
        intArrayOf(
            Color.Black.argb(0.7f),
            Color.Black.argb(0.1f),
            Color.White.argb(0.32f),
            Color.White.argb(0.04f),
            Color.Black.argb(0.2f),
            Color.Black.argb(0.75f)
        ),
        floatArrayOf(0.79f, 0.83f, 0.86f, 0.88f, 0.96f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(CENTER, CENTER, ReactorGeometry.TUBE, paint)
    paint.shader = null
}

private fun windowsPath(): Path {
    val path = Path()
    val step = 360f / ReactorGeometry.WINDOW_COUNT
    val half = ReactorGeometry.WINDOW_SEPARATION / 2f
    repeat(ReactorGeometry.WINDOW_COUNT) { i ->
        path.addSector(
            ReactorGeometry.WINDOW_INNER,
            ReactorGeometry.WINDOW_OUTER,
            i * step + half,
            (i + 1) * step - half
        )
    }
    return path
}

private fun Path.addSector(inner: Float, outer: Float, from: Float, to: Float) {
    val outerOval = RectF(CENTER - outer, CENTER - outer, CENTER + outer, CENTER + outer)
    val innerOval = RectF(CENTER - inner, CENTER - inner, CENTER + inner, CENTER + inner)
    val start = ReactorGeometry.polar(outer, from)
    val end = ReactorGeometry.polar(inner, to)
    moveTo(start.x, start.y)
    arcTo(outerOval, from - 90f, to - from, false)
    lineTo(end.x, end.y)
    arcTo(innerOval, to - 90f, -(to - from), false)
    close()
}

private fun Color.argb(alpha: Float): Int = copy(alpha = alpha).toArgb()
