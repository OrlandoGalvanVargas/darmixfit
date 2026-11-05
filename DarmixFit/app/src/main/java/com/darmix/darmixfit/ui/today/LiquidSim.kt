package com.darmix.darmixfit.ui.today

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Random
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

internal class LiquidSensors {
    var pullX = 0f
    var pullY = SensorManager.GRAVITY_EARTH
    var shakeX = 0f
    var shakeY = 0f

    fun rest() {
        shakeX = 0f
        shakeY = 0f
    }
}

@Composable
internal fun rememberLiquidSensors(): LiquidSensors {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sensors = remember { LiquidSensors() }
    DisposableEffect(context, lifecycle) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gravity = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val linear = manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val raw = if (gravity == null || linear == null) manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null
        var lowX = 0f
        var lowY = SensorManager.GRAVITY_EARTH
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY -> {
                        sensors.pullX = -x
                        sensors.pullY = y
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        sensors.shakeX += (x - sensors.shakeX) * SHAKE_SMOOTHING
                        sensors.shakeY += (-y - sensors.shakeY) * SHAKE_SMOOTHING
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowX += (x - lowX) * GRAVITY_SMOOTHING
                        lowY += (y - lowY) * GRAVITY_SMOOTHING
                        sensors.pullX = -lowX
                        sensors.pullY = lowY
                        sensors.shakeX += ((x - lowX) - sensors.shakeX) * SHAKE_SMOOTHING
                        sensors.shakeY += (-(y - lowY) - sensors.shakeY) * SHAKE_SMOOTHING
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        var registered = false
        fun register() {
            if (registered || manager == null) return
            listOfNotNull(gravity.takeIf { raw == null }, linear.takeIf { raw == null }, raw).forEach {
                manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
                registered = true
            }
        }
        fun unregister() {
            if (!registered) return
            manager?.unregisterListener(listener)
            registered = false
            sensors.rest()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            unregister()
        }
    }
    return sensors
}

internal class LiquidSim {

    var count = 0
        private set
    var upX = 0f
        private set
    var upY = -1f
        private set

    private val px = FloatArray(MAX_PARTICLES)
    private val py = FloatArray(MAX_PARTICLES)
    private val vx = FloatArray(MAX_PARTICLES)
    private val vy = FloatArray(MAX_PARTICLES)
    private val prevX = FloatArray(MAX_PARTICLES)
    private val prevY = FloatArray(MAX_PARTICLES)
    private val density = FloatArray(MAX_PARTICLES)
    private val nearDensity = FloatArray(MAX_PARTICLES)
    private val shiftX = FloatArray(MAX_PARTICLES)
    private val shiftY = FloatArray(MAX_PARTICLES)
    private val next = IntArray(MAX_PARTICLES)
    private val heads = IntArray(GRID * GRID)
    private val random = Random(19L)
    private var initialized = false
    private var ambientClock = 0f

    fun x(i: Int) = px[i]
    fun y(i: Int) = py[i]

    fun step(dt: Float, sensors: LiquidSensors, progress: Float, ambient: Float) {
        if (dt <= 0f) return
        val scale = GRAVITY / SensorManager.GRAVITY_EARTH
        val pullX = sensors.pullX
        val pullY = sensors.pullY
        val pull = hypot(pullX, pullY)
        if (pull > MIN_PULL) {
            upX = -pullX / pull
            upY = -pullY / pull
        }
        ambientClock += dt
        val sway = ambient * AMBIENT_FORCE * sin(ambientClock * 2f * PI.toFloat() / AMBIENT_PERIOD)
        val gx = (pullX - sensors.shakeX) * scale + upY * sway
        val gy = (pullY - sensors.shakeY) * scale - upX * sway

        val target = (progress.coerceIn(0f, 1f) * MAX_PARTICLES).toInt()
        if (!initialized) {
            fillSettled(target)
            initialized = true
        } else {
            adjustCount(target)
        }

        val steps = ceil(dt / STEP).toInt().coerceIn(1, MAX_SUBSTEPS)
        val h = (dt / steps).coerceAtMost(STEP * 1.5f)
        repeat(steps) { substep(h, gx, gy) }
    }

    private fun substep(dt: Float, gx: Float, gy: Float) {
        val n = count
        if (n == 0) return
        val damping = exp(-DRAG * dt)
        for (i in 0 until n) {
            var x = (vx[i] + gx * dt) * damping
            var y = (vy[i] + gy * dt) * damping
            val speed = hypot(x, y)
            if (speed > MAX_SPEED) {
                x *= MAX_SPEED / speed
                y *= MAX_SPEED / speed
            }
            vx[i] = x
            vy[i] = y
        }

        buildGrid(n)
        forEachPair { i, j, dx, dy, r ->
            val q = r / RANGE
            val nx = dx / r
            val ny = dy / r
            val u = (vx[i] - vx[j]) * nx + (vy[i] - vy[j]) * ny
            if (u < 0f) {
                val impulse = dt * (1f - q) * VISCOSITY * -u * 0.5f
                vx[i] += impulse * nx
                vy[i] += impulse * ny
                vx[j] -= impulse * nx
                vy[j] -= impulse * ny
            }
        }

        for (i in 0 until n) {
            prevX[i] = px[i]
            prevY[i] = py[i]
            px[i] += vx[i] * dt
            py[i] += vy[i] * dt
            density[i] = 0f
            nearDensity[i] = 0f
            shiftX[i] = 0f
            shiftY[i] = 0f
        }

        buildGrid(n)
        forEachPair { i, j, _, _, r ->
            val w = 1f - r / RANGE
            val w2 = w * w
            density[i] += w2
            density[j] += w2
            nearDensity[i] += w2 * w
            nearDensity[j] += w2 * w
        }
        for (i in 0 until n) {
            val pressure = STIFFNESS * (density[i] - REST_DENSITY)
            density[i] = if (pressure < 0f) pressure * ATTRACTION else pressure
        }
        val dt2 = dt * dt
        forEachPair { i, j, dx, dy, r ->
            val w = 1f - r / RANGE
            val pressure = (density[i] + density[j]) * 0.5f
            val near = NEAR_STIFFNESS * (nearDensity[i] + nearDensity[j]) * 0.5f
            val push = dt2 * (pressure * w + near * w * w) * 0.5f / r
            shiftX[i] += push * dx
            shiftY[i] += push * dy
            shiftX[j] -= push * dx
            shiftY[j] -= push * dy
        }

        val limit = RADIUS - WALL_GAP
        for (i in 0 until n) {
            var x = px[i] + shiftX[i]
            var y = py[i] + shiftY[i]
            val rx = x - ReactorGeometry.CENTER
            val ry = y - ReactorGeometry.CENTER
            val distance = hypot(rx, ry)
            if (distance > limit) {
                x = ReactorGeometry.CENTER + rx / distance * limit
                y = ReactorGeometry.CENTER + ry / distance * limit
            }
            px[i] = x
            py[i] = y
            vx[i] = (x - prevX[i]) / dt
            vy[i] = (y - prevY[i]) / dt
        }
    }

    private inline fun forEachPair(action: (Int, Int, Float, Float, Float) -> Unit) {
        val range2 = RANGE * RANGE
        for (i in 0 until count) {
            val cx = cellOf(px[i])
            val cy = cellOf(py[i])
            for (oy in -1..1) {
                val gy = cy + oy
                if (gy < 0 || gy >= GRID) continue
                for (ox in -1..1) {
                    val gx = cx + ox
                    if (gx < 0 || gx >= GRID) continue
                    var j = heads[gy * GRID + gx]
                    while (j != -1) {
                        if (j > i) {
                            val dx = px[i] - px[j]
                            val dy = py[i] - py[j]
                            val d2 = dx * dx + dy * dy
                            if (d2 < range2 && d2 > 1e-6f) action(i, j, dx, dy, sqrt(d2))
                        }
                        j = next[j]
                    }
                }
            }
        }
    }

    private fun buildGrid(n: Int) {
        heads.fill(-1)
        for (i in 0 until n) {
            val cell = cellOf(py[i]) * GRID + cellOf(px[i])
            next[i] = heads[cell]
            heads[cell] = i
        }
    }

    private fun cellOf(value: Float): Int = (value / RANGE).toInt().coerceIn(0, GRID - 1)

    private fun fillSettled(target: Int) {
        count = 0
        val downX = -upX
        val downY = -upY
        val sideX = -downY
        val sideY = downX
        val rowStep = SPACING * 0.866f
        var row = 0
        var depth = RADIUS - SPACING * 0.6f
        while (count < target && depth > -RADIUS) {
            val offset = if (row % 2 == 0) 0f else SPACING / 2f
            var lateral = -RADIUS + offset
            while (lateral < RADIUS && count < target) {
                val x = ReactorGeometry.CENTER + downX * depth + sideX * lateral
                val y = ReactorGeometry.CENTER + downY * depth + sideY * lateral
                if (hypot(x - ReactorGeometry.CENTER, y - ReactorGeometry.CENTER) < RADIUS - SPACING * 0.5f) {
                    place(count++, x, y)
                }
                lateral += SPACING
            }
            depth -= rowStep
            row++
        }
    }

    private fun adjustCount(target: Int) {
        var budget = CHANGE_PER_FRAME
        while (count < target && budget-- > 0) {
            val side = (random.nextFloat() * 2f - 1f) * RADIUS * 0.45f
            val lift = RADIUS * (0.55f + random.nextFloat() * 0.2f)
            val x = ReactorGeometry.CENTER + upX * lift - upY * side
            val y = ReactorGeometry.CENTER + upY * lift + upX * side
            place(count, x, y)
            vx[count] = -upX * POUR_SPEED
            vy[count] = -upY * POUR_SPEED
            count++
        }
        while (count > target && budget-- > 0) {
            var top = 0
            var best = -Float.MAX_VALUE
            for (i in 0 until count) {
                val height = (px[i] - ReactorGeometry.CENTER) * upX + (py[i] - ReactorGeometry.CENTER) * upY
                if (height > best) {
                    best = height
                    top = i
                }
            }
            val last = --count
            px[top] = px[last]
            py[top] = py[last]
            vx[top] = vx[last]
            vy[top] = vy[last]
        }
    }

    private fun place(i: Int, x: Float, y: Float) {
        px[i] = x
        py[i] = y
        vx[i] = 0f
        vy[i] = 0f
    }

    companion object {
        const val MAX_PARTICLES = 260
        const val RADIUS = ReactorGeometry.CORE
        val SPACING = sqrt(PI.toFloat() * RADIUS * RADIUS / MAX_PARTICLES)
        val RANGE = SPACING * 2.1f
        val SPRITE_RADIUS = SPACING * 1.7f

        private const val STEP = 1f / 240f
        private const val MAX_SUBSTEPS = 8
        private const val GRAVITY = 1200f
        private const val MIN_PULL = 0.8f
        private const val MAX_SPEED = 900f
        private const val VISCOSITY = 2f
        private const val STIFFNESS = 42_000f
        private const val ATTRACTION = 0.4f
        private const val NEAR_STIFFNESS = 105_000f
        private const val DRAG = 0.15f
        private const val WALL_GAP = 2.5f
        private const val CHANGE_PER_FRAME = 4
        private const val POUR_SPEED = 60f
        private const val AMBIENT_FORCE = 26f
        private const val AMBIENT_PERIOD = 2.6f
        private val GRID = ceil(ReactorGeometry.VIEWBOX / RANGE).toInt() + 1
        private val REST_DENSITY = restDensity()

        private fun restDensity(): Float {
            var sum = 0f
            for (row in -6..6) {
                for (col in -6..6) {
                    val x = col * SPACING + if (row % 2 != 0) SPACING / 2f else 0f
                    val y = row * SPACING * 0.866f
                    val d = hypot(x, y)
                    if (d <= 0f) continue
                    val q = d / RANGE
                    if (q < 1f) sum += (1f - q) * (1f - q)
                }
            }
            return sum
        }
    }
}

private const val SHAKE_SMOOTHING = 0.5f
private const val GRAVITY_SMOOTHING = 0.1f
