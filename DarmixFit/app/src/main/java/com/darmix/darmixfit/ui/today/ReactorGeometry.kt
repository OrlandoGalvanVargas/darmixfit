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
    const val HOUSING_SHADE = 121f
    const val HOUSING_SHADE_WIDTH = 20f
    const val HOUSING_SHADE_REACH = 131f
    const val WELL = 109f
    const val BASE_GLOW = 108f
    const val TUBE = 132.5f
    const val TUBE_WIDTH = 36f
    const val WINDOW_INNER = 116f
    const val WINDOW_OUTER = 149f
    const val WINDOW_COUNT = 10
    const val WINDOW_SEPARATION = 11f
    const val SPILL_INNER = 122.5f
    const val SPILL_OUTER = 142.5f
    const val SPILL_BANDS = 12

    const val GROOVE = 113.2f
    const val RING = 99f
    const val RING_OUTER = 110f
    const val RING_INNER = 88f
    const val RING_SHADE = 106f
    const val RING_SHADE_REACH = 112f
    const val SLOT_INNER = 94f
    const val SLOT_OUTER = 104f
    const val SLOT_SHADE = 106f
    const val SLOT_COUNT = 12
    const val SLOT_WIDTH = 10f

    const val CORE = 83.5f
    const val RIM_INNER = CORE - 5f
    const val RIM_OUTER = CORE + 4.6f
    const val MESH = CORE - 22.5f
    const val MESH_RIM = MESH + 2.6f
    const val MESH_PERIOD = 7.5f
    const val HUB = 40f
    const val SPOKE_HALF = 3.6f
    const val PLATE_INNER = MESH - 1f
    const val PLATE_OUTER = 114f
    const val PLATE_HALF = 11f
    const val PLATE_CUT = 3f
    const val SCREW = PLATE_OUTER - 10f

    const val GRAB_INNER = 82f
    const val GRAB_OUTER = 118f

    fun polar(radius: Float, degrees: Float): Offset {
        val radians = Math.toRadians((degrees - 90f).toDouble())
        return Offset(
            CENTER + radius * cos(radians).toFloat(),
            CENTER + radius * sin(radians).toFloat()
        )
    }

    fun radial(radius: Float, side: Float, degrees: Float): Offset {
        val radians = Math.toRadians((degrees - 90f).toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        return Offset(CENTER + radius * c - side * s, CENTER + radius * s + side * c)
    }
}
