package com.darmix.darmixfit.service

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

object DarmixChime {
    const val SAMPLE_RATE = 44_100
    private const val DURATION_SECONDS = 2.0
    private const val DETUNE = 1.0021
    private const val CHORUS_LEVEL = 0.3
    private const val FADE_SECONDS = 0.08
    private const val DRIVE = 1.1
    private const val PEAK = 0.9

    private const val E5 = 659.25
    private const val A5 = 880.0
    private const val E6 = 1318.51
    private const val A6 = 1760.0
    private const val E7 = 2637.02

    private class Partial(val ratio: Double, val amplitude: Double, val decay: Double)

    private val partials = arrayOf(
        Partial(1.0, 1.0, 1.0),
        Partial(2.0, 0.45, 0.55),
        Partial(2.76, 0.32, 0.40),
        Partial(5.40, 0.16, 0.22),
        Partial(8.93, 0.07, 0.12)
    )

    val durationMillis: Long = (DURATION_SECONDS * 1000).toLong()

    val pcm: ShortArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { synthesize() }

    fun prewarm() {
        Thread({ pcm }, "darmix-chime").apply { isDaemon = true }.start()
    }

    private fun synthesize(): ShortArray {
        val length = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val buffer = DoubleArray(length)

        bell(buffer, 0.00, E5, 0.30, 0.16)
        bell(buffer, 0.11, A5, 0.30, 0.16)
        bell(buffer, 0.22, E6, 0.38, 0.42)
        kick(buffer, 0.22, 0.65)

        bell(buffer, 0.74, A5, 0.30, 0.16)
        bell(buffer, 0.85, E6, 0.32, 0.18)
        bell(buffer, 0.96, A6, 0.36, 0.62)
        bell(buffer, 0.96, E6, 0.20, 0.62)
        bell(buffer, 0.96, E7, 0.07, 0.35)
        kick(buffer, 0.96, 0.70)

        val fade = (FADE_SECONDS * SAMPLE_RATE).toInt()
        for (i in 0 until fade) {
            val g = 1.0 - i.toDouble() / fade
            buffer[length - fade + i] *= g * g
        }

        var peak = 0.0
        for (i in buffer.indices) {
            val v = tanh(buffer[i] * DRIVE)
            buffer[i] = v
            peak = maxOf(peak, abs(v))
        }
        val gain = if (peak > 0.0) PEAK / peak else 0.0
        return ShortArray(length) { i -> (buffer[i] * gain * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun bell(buffer: DoubleArray, start: Double, frequency: Double, amplitude: Double, tau: Double) {
        val first = (start * SAMPLE_RATE).toInt()
        val twoPi = 2.0 * PI
        for (i in first until buffer.size) {
            val x = (i - first).toDouble() / SAMPLE_RATE
            val attack = min(x / 0.002, 1.0)
            var sample = 0.0
            for (p in partials) {
                val f = frequency * p.ratio
                val envelope = exp(-x / (tau * p.decay))
                if (envelope < 1e-4) continue
                sample += p.amplitude * envelope *
                    (sin(twoPi * f * x) + CHORUS_LEVEL * sin(twoPi * f * DETUNE * x + 0.7))
            }
            buffer[i] += amplitude * attack * sample
        }
    }

    private fun kick(buffer: DoubleArray, start: Double, amplitude: Double) {
        val first = (start * SAMPLE_RATE).toInt()
        var phase = 0.0
        for (i in first until buffer.size) {
            val x = (i - first).toDouble() / SAMPLE_RATE
            val envelope = exp(-x / 0.085)
            if (envelope < 1e-4) break
            val f = 50.0 + 170.0 * exp(-x * 35.0)
            phase += 2.0 * PI * f / SAMPLE_RATE
            buffer[i] += amplitude * envelope * sin(phase)
        }
    }
}
