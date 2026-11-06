package com.darmix.darmixfit.ui.today

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sign

internal enum class ReactorPhase { OFF, RUN, FULL }

internal class ReactorDrive private constructor(val phase: ReactorPhase, val charge: Float) {

    val motion: Float = when (phase) {
        ReactorPhase.OFF -> 0f
        ReactorPhase.RUN -> lerp(0.12f, 1f, charge)
        ReactorPhase.FULL -> 1f
    }

    val waves: Float = when (phase) {
        ReactorPhase.OFF -> 0f
        ReactorPhase.RUN -> lerp(0.4f, 1f, charge)
        ReactorPhase.FULL -> 1f
    }

    val glassDim: Float = when (phase) {
        ReactorPhase.OFF -> 0.62f
        ReactorPhase.RUN -> lerp(0.46f, 0.06f, charge)
        ReactorPhase.FULL -> 0f
    }

    val sheen: Float = when (phase) {
        ReactorPhase.OFF -> 0.55f
        ReactorPhase.RUN -> lerp(0.7f, 1f, charge)
        ReactorPhase.FULL -> 1f
    }

    val rimLevel: Float = when (phase) {
        ReactorPhase.OFF -> 0f
        ReactorPhase.RUN -> 1f + charge
        ReactorPhase.FULL -> 3f
    }

    val flickerRate: Float = lerp(0.35f, 7f, charge * charge)
    val flickerBase: Float = lerp(0.22f, 0.9f, charge)
    val flickerChance: Float = lerp(0.45f, 1f, charge)

    override fun equals(other: Any?): Boolean =
        other is ReactorDrive && other.phase == phase && other.charge == charge

    override fun hashCode(): Int = phase.hashCode() * 31 + charge.hashCode()

    companion object {
        fun of(completed: Int, total: Int): ReactorDrive = when {
            total <= 0 || completed <= 0 -> ReactorDrive(ReactorPhase.OFF, 0f)
            completed >= total -> ReactorDrive(ReactorPhase.FULL, 1f)
            total <= 2 -> ReactorDrive(ReactorPhase.RUN, 1f)
            else -> ReactorDrive(ReactorPhase.RUN, ((completed - 1f) / (total - 2f)).coerceIn(0f, 1f))
        }
    }
}

internal fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

internal class ReactorMotion {
    var clock = 0f
        private set
    var flowA = 0f
        private set
    var flowB = 0f
        private set
    var waveFront = 0f
        private set
    var waveBack = 0f
        private set
    var breath = 0f
        private set
    var flicker = 1f
        private set
    var angle = 0f
        private set
    val liquid = LiquidSim()
    var full = Float.NaN
        private set
    var dim = Float.NaN
        private set

    private var velocity = 0f
    private var dragging = false
    private var manual = false
    private var tickAccumulator = 0f
    private var flickerPhase = 0f
    private var speedNow = Float.NaN

    fun step(dt: Float, drive: ReactorDrive, progress: Float, sensors: LiquidSensors, feedback: ReactorSpinFeedback) {
        clock += dt
        if (speedNow.isNaN()) speedNow = drive.motion
        speedNow += (drive.motion - speedNow) * (1f - exp(-dt * SPEED_RATE))
        if (drive.motion == 0f && speedNow < 0.01f) speedNow = 0f
        val speed = speedNow
        if (speed > 0f) {
            flowA = (flowA + dt * speed / FLOW_A_SECONDS) % 1f
            flowB = (flowB + dt * speed / FLOW_B_SECONDS) % 1f
            breath = (breath + dt * speed / BREATH_SECONDS) % 1f
        }
        if (drive.waves > 0f) {
            waveFront = (waveFront + dt * drive.waves / WAVE_FRONT_SECONDS) % 1f
            waveBack = (waveBack + dt * drive.waves / WAVE_BACK_SECONDS) % 1f
        }
        flicker = if (drive.phase == ReactorPhase.RUN) {
            flickerPhase = (flickerPhase + dt * drive.flickerRate) % FLICKER_WRAP
            flickerValue(drive)
        } else {
            1f
        }

        val targetFull = if (progress >= 1f) 1f else 0f
        if (full.isNaN()) full = targetFull
        if (dim.isNaN()) dim = drive.glassDim
        full += (targetFull - full) * (1f - exp(-dt * FULL_RATE))
        dim += (drive.glassDim - dim) * (1f - exp(-dt * DIM_RATE))
        liquid.step(dt, sensors, progress, drive.waves)

        if (!dragging && manual) {
            if (abs(velocity) > FLING_STOP) {
                val delta = velocity * dt
                rotate(delta)
                addTicks(delta, feedback)
                velocity *= exp(-FLING_FRICTION * dt)
            } else {
                manual = false
                velocity = 0f
                tickAccumulator = 0f
                feedback.end()
            }
        }
    }

    fun beginDrag() {
        dragging = true
        manual = true
        velocity = 0f
    }

    fun drag(delta: Float, dt: Float, feedback: ReactorSpinFeedback) {
        rotate(delta)
        val instant = delta / dt.coerceAtLeast(0.004f)
        velocity = velocity * 0.45f + instant * 0.55f
        addTicks(delta, feedback)
    }

    fun endDrag(stale: Boolean) {
        dragging = false
        velocity = if (stale) 0f else velocity.coerceIn(-MAX_FLING, MAX_FLING)
    }

    private fun flickerValue(drive: ReactorDrive): Float {
        val cycle = floor(flickerPhase).toInt()
        val within = flickerPhase - cycle
        val charge = drive.charge
        val up = if (hash(cycle, 1) < drive.flickerChance * (1f - charge * 0.6f)) {
            pulse(within, 0.3f + 0.4f * hash(cycle, 3), 0.34f) * (0.6f + 0.4f * hash(cycle, 5))
        } else {
            0f
        }
        val down = if (hash(cycle, 2) < drive.flickerChance * (0.3f + 0.7f * charge)) {
            pulse(within, 0.3f + 0.4f * hash(cycle, 4), 0.42f) * (0.6f + 0.4f * hash(cycle, 6))
        } else {
            0f
        }
        return (drive.flickerBase + 0.42f * (1f - charge) * up - 0.38f * charge * down).coerceIn(0.08f, 1f)
    }

    private fun pulse(position: Float, center: Float, width: Float): Float =
        (1f - abs(position - center) / (width / 2f)).coerceIn(0f, 1f)

    private fun hash(cycle: Int, seed: Int): Float {
        var h = cycle * 374761393 + seed * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0xFFFF) / 65535f
    }

    private fun rotate(delta: Float) {
        angle = (angle + delta) % 360f
    }

    private fun addTicks(delta: Float, feedback: ReactorSpinFeedback) {
        tickAccumulator += delta
        while (abs(tickAccumulator) >= TICK_DEGREES) {
            tickAccumulator -= TICK_DEGREES * sign(tickAccumulator)
            feedback.click((abs(velocity) / CLICK_FULL_SPEED).coerceIn(0.15f, 1f))
        }
    }

    companion object {
        private const val FLOW_A_SECONDS = 1.1f
        private const val FLOW_B_SECONDS = 0.75f
        private const val WAVE_FRONT_SECONDS = 2.6f
        private const val WAVE_BACK_SECONDS = 4.4f
        private const val BREATH_SECONDS = 2.4f
        private const val FULL_RATE = 4f
        private const val DIM_RATE = 3.5f
        private const val FLING_STOP = 14f
        private const val FLING_FRICTION = 2.1f
        private const val MAX_FLING = 1440f
        private const val TICK_DEGREES = 15f
        private const val CLICK_FULL_SPEED = 900f
        private const val FLICKER_WRAP = 100_000f
        private const val SPEED_RATE = 1.4f
    }
}
