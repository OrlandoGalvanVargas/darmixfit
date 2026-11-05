package com.darmix.darmixfit.ui.today

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

internal object ReactorGeometry {
    const val VIEWBOX = 320f
    const val CENTER = 160f
    val Center = Offset(CENTER, CENTER)

    const val BEZEL = 160f
    const val CHAMFER = 158.2f
    const val FACE = 156.4f
    const val HOUSING = 153f
    const val HOUSING_EDGE = 152f
    const val HOUSING_INNER = 112f
    const val HOUSING_INNER_EDGE = 113f
    const val WELL = 109f
    const val BASE_GLOW = 108f
    const val TUBE = 132.5f
    const val TUBE_WIDTH = 36f
    const val WINDOW_INNER = 116f
    const val WINDOW_OUTER = 149f
    const val WINDOW_COUNT = 10
    const val WINDOW_SEPARATION = 11f

    const val RING = 99f
    const val RING_OUTER = 110f
    const val RING_INNER = 88f
    const val SLOT_INNER = 92f
    const val SLOT_OUTER = 106f
    const val SLOT_COUNT = 24
    const val SLOT_GAP = 2f

    const val CORE = 83.5f
    const val GLASS_OUTER = CORE + 4.6f
    const val GLASS_RIM = CORE - 0.7f
    const val GLASS_TAB_COUNT = 8

    const val GRAB_INNER = 82f
    const val GRAB_OUTER = 118f

    fun polar(radius: Float, degrees: Float): Offset {
        val radians = Math.toRadians((degrees - 90f).toDouble())
        return Offset(
            CENTER + radius * cos(radians).toFloat(),
            CENTER + radius * sin(radians).toFloat()
        )
    }
}
