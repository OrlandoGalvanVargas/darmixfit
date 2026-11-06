package com.darmix.darmixfit.service

import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

object DarmixChime {
    const val SAMPLE_RATE = 44_100
    private const val DURATION_SECONDS = 2.0
    private const val FADE_SECONDS = 0.15
    private const val STRIKES = 3
    private const val STRIKE_GAP = 0.28
    private const val FUNDAMENTAL = 840.0
    private const val DETUNE = 1.003
    private const val DETUNE_LEVEL = 0.25
    private const val HAMMER_LEVEL = 1.2
    private const val HAMMER_DECAY = 0.006
    private const val HAMMER_SMOOTH = 8
    private const val DRIVE = 0.55
    private const val PEAK = 0.9
    private const val TWO_PI = 2.0 * PI

    private class Partial(val ratio: Double, val amplitude: Double, val decay: Double)

    private val partials = arrayOf(
        Partial(1.0, 1.0, 0.32),
        Partial(2.32, 0.75, 0.22),
        Partial(2.97, 0.35, 0.16),
        Partial(4.11, 0.55, 0.13),
        Partial(5.44, 0.3, 0.1),
        Partial(6.8, 0.25, 0.08),
        Partial(8.3, 0.15, 0.06)
    )

    val durationMillis: Long = (DURATION_SECONDS * 1000).toLong()

    val pcm: ShortArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { synthesize() }

    fun prewarm() {
        Thread({ pcm }, "darmix-chime").apply { isDaemon = true }.start()
    }

    private fun synthesize(): ShortArray {
        val length = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val buffer = DoubleArray(length)
        val random = Random(5L)
        repeat(STRIKES) { i -> strike(buffer, i * STRIKE_GAP, random) }

        var peak = 0.0
        for (i in buffer.indices) {
            val v = tanh(buffer[i] * DRIVE)
            buffer[i] = v
            peak = max(peak, abs(v))
        }
        val gain = if (peak > 0.0) PEAK / peak else 0.0
        val fade = (FADE_SECONDS * SAMPLE_RATE).toInt()
        for (i in 0 until fade) {
            val g = 1.0 - i.toDouble() / fade
            buffer[length - fade + i] *= g * g
        }
        return ShortArray(length) { i -> (buffer[i] * gain * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun strike(buffer: DoubleArray, start: Double, random: Random) {
        val first = (start * SAMPLE_RATE).toInt()
        val count = buffer.size - first
        if (count <= 0) return
        val noise = DoubleArray(count) { random.nextGaussian() }
        var window = 0.0
        for (n in 0 until count) {
            window += noise[n]
            if (n >= HAMMER_SMOOTH) window -= noise[n - HAMMER_SMOOTH]
            val x = n.toDouble() / SAMPLE_RATE
            var tone = 0.0
            for (p in partials) {
                val envelope = exp(-x / p.decay)
                if (envelope < 1e-4) continue
                val f = FUNDAMENTAL * p.ratio
                tone += p.amplitude * envelope * (sin(TWO_PI * f * x) + DETUNE_LEVEL * sin(TWO_PI * f * DETUNE * x + 1.1))
            }
            val hammer = HAMMER_LEVEL * (noise[n] - window / HAMMER_SMOOTH) * exp(-x / HAMMER_DECAY)
            buffer[first + n] += (tone + hammer) * min(x / 0.0008, 1.0)
        }
    }
}
