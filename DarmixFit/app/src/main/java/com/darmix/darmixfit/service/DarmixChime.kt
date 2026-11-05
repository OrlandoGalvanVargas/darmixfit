package com.darmix.darmixfit.service

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

object DarmixChime {
    const val SAMPLE_RATE = 44_100
    private const val DURATION_SECONDS = 1.7
    private const val FADE_SECONDS = 0.15
    private const val DRIVE = 1.3
    private const val PEAK = 0.9
    private const val TWO_PI = 2.0 * PI

    private val rising = doubleArrayOf(1318.51, 1760.0, 2217.46, 2637.02)
    private val falling = doubleArrayOf(2217.46, 1760.0, 1318.51, 987.77, 659.25)

    val durationMillis: Long = (DURATION_SECONDS * 1000).toLong()

    val pcm: ShortArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { synthesize() }

    fun prewarm() {
        Thread({ pcm }, "darmix-chime").apply { isDaemon = true }.start()
    }

    private fun synthesize(): ShortArray {
        val length = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val buffer = DoubleArray(length)

        rising.forEachIndexed { index, frequency ->
            val start = index * 0.14
            chirp(buffer, start, frequency * 0.75, frequency, 0.09, 0.25)
            bell(buffer, start + 0.02, frequency, 0.35, 0.22, 1.2, 0.09)
        }
        falling.forEachIndexed { index, frequency ->
            val start = 0.62 + index * 0.13
            val level = 1.0 - index * 0.08
            chirp(buffer, start, frequency * 1.3, frequency, 0.09, 0.22 * level)
            bell(buffer, start + 0.02, frequency, 0.4, 0.22 * level, 1.1, 0.11)
        }

        val fade = (FADE_SECONDS * SAMPLE_RATE).toInt()
        for (i in 0 until fade) {
            val g = 1.0 - i.toDouble() / fade
            buffer[length - fade + i] *= g * g
        }

        var peak = 0.0
        for (i in buffer.indices) {
            val v = tanh(buffer[i] * DRIVE)
            buffer[i] = v
            peak = max(peak, abs(v))
        }
        val gain = if (peak > 0.0) PEAK / peak else 0.0
        return ShortArray(length) { i -> (buffer[i] * gain * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun chirp(buffer: DoubleArray, start: Double, from: Double, to: Double, duration: Double, amplitude: Double) {
        val first = (start * SAMPLE_RATE).toInt()
        val count = (duration * SAMPLE_RATE).toInt()
        val ratio = ln(to / from)
        var phase = 0.0
        for (n in 0 until count) {
            val i = first + n
            if (i >= buffer.size) break
            val x = n.toDouble() / SAMPLE_RATE
            val frequency = from * exp(ratio * x / duration)
            phase += TWO_PI * frequency / SAMPLE_RATE
            val envelope = exp(-x / (duration * 0.6)) * min(x / 0.002, 1.0)
            buffer[i] += amplitude * envelope * sin(phase)
        }
    }

    private fun bell(
        buffer: DoubleArray,
        start: Double,
        frequency: Double,
        duration: Double,
        amplitude: Double,
        index: Double,
        tau: Double
    ) {
        val first = (start * SAMPLE_RATE).toInt()
        val count = (duration * SAMPLE_RATE).toInt()
        for (n in 0 until count) {
            val i = first + n
            if (i >= buffer.size) break
            val x = n.toDouble() / SAMPLE_RATE
            val envelope = exp(-x / tau) * min(x / 0.003, 1.0)
            val modulation = index * exp(-x / (tau * 0.5)) * sin(TWO_PI * frequency * 2.0 * x)
            buffer[i] += amplitude * envelope * sin(TWO_PI * frequency * x + modulation)
        }
    }
}
