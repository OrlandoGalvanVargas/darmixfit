package com.darmix.darmixfit.ui.today

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.darmix.darmixfit.ui.settings.LocalHapticsEnabled
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

@Composable
internal fun rememberSpinFeedback(): ReactorSpinFeedback {
    val view = LocalView.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val feedback = remember(view) { ReactorSpinFeedback(view) }
    SideEffect { feedback.hapticsEnabled = hapticsEnabled }
    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }
    return feedback
}

internal class ReactorSpinFeedback(private val view: View) {

    private class Trigger(val variant: Int, val gain: Float)

    private class Voice(val data: ShortArray, val gain: Float) {
        var position = 0
    }

    private val pending = ConcurrentLinkedQueue<Trigger>()

    @Volatile
    private var stopRequested = false

    @Volatile
    private var released = false

    @Volatile
    private var worker: Thread? = null

    @Volatile
    var hapticsEnabled = true

    private var lastHaptic = 0L
    private var variant = 0

    fun click(intensity: Float) {
        if (released) return
        val now = SystemClock.uptimeMillis()
        if (hapticsEnabled && now - lastHaptic >= HAPTIC_GAP_MS) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastHaptic = now
        }
        variant = (variant + 1) % SpinClickBank.VARIANTS
        pending.add(Trigger(variant, MIN_GAIN + (MAX_GAIN - MIN_GAIN) * intensity))
        stopRequested = false
        ensureWorker()
    }

    fun end() {
        stopRequested = true
    }

    fun release() {
        released = true
        stopRequested = true
        pending.clear()
    }

    private fun ensureWorker() {
        val current = worker
        if (current != null && current.isAlive) return
        worker = Thread(::render, "darmix-spin").apply {
            isDaemon = true
            start()
        }
    }

    private fun render() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val bank = SpinClickBank.samples
        val track = runCatching { buildTrack() }.getOrNull() ?: return
        val voices = ArrayList<Voice>(MAX_VOICES)
        val mix = FloatArray(FRAMES)
        val out = ShortArray(FRAMES)
        var idleFrames = 0
        runCatching {
            track.play()
            while (!released) {
                while (true) {
                    val trigger = pending.poll() ?: break
                    if (voices.size >= MAX_VOICES) voices.removeAt(0)
                    voices.add(Voice(bank[trigger.variant], trigger.gain))
                }
                if (voices.isEmpty()) {
                    idleFrames += FRAMES
                    val limit = if (stopRequested) STOP_TAIL_FRAMES else IDLE_LIMIT_FRAMES
                    if (idleFrames > limit) break
                } else {
                    idleFrames = 0
                }
                mix.fill(0f)
                val iterator = voices.iterator()
                while (iterator.hasNext()) {
                    val voice = iterator.next()
                    val count = min(FRAMES, voice.data.size - voice.position)
                    for (i in 0 until count) {
                        mix[i] += voice.data[voice.position + i] * voice.gain / Short.MAX_VALUE
                    }
                    voice.position += count
                    if (voice.position >= voice.data.size) iterator.remove()
                }
                val crowd = if (voices.size > CROWD) sqrt(CROWD.toFloat() / voices.size) else 1f
                for (i in 0 until FRAMES) {
                    out[i] = ((tanh(mix[i] * crowd * DRIVE) / DRIVE_NORM).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(out, 0, FRAMES)
            }
        }
        runCatching { track.stop() }
        track.release()
    }

    private fun buildTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            SpinClickBank.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SpinClickBank.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBuffer, FRAMES * 4))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private companion object {
        val DRIVE_NORM = tanh(1.1f)
        const val FRAMES = 256
        const val MAX_VOICES = 24
        const val CROWD = 4
        const val DRIVE = 1.1f
        const val HAPTIC_GAP_MS = 28L
        const val MIN_GAIN = 0.22f
        const val MAX_GAIN = 0.5f
        const val STOP_TAIL_FRAMES = SpinClickBank.SAMPLE_RATE * 3 / 10
        const val IDLE_LIMIT_FRAMES = SpinClickBank.SAMPLE_RATE * 4
    }
}

private object SpinClickBank {
    const val SAMPLE_RATE = 44_100
    const val VARIANTS = 4

    private const val DURATION_SECONDS = 0.045
    private const val PEAK = 0.8
    private const val ECHO_DELAY = 0.0032
    private const val ECHO_GAIN = 0.3

    private val bases = doubleArrayOf(1180.0, 1240.0, 1120.0, 1290.0)
    private val ratios = doubleArrayOf(1.0, 2.31, 3.86)
    private val amplitudes = doubleArrayOf(1.0, 0.55, 0.3)
    private val decays = doubleArrayOf(0.0065, 0.0042, 0.0026)

    val samples: Array<ShortArray> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Array(VARIANTS) { synthesize(it) }
    }

    private fun synthesize(index: Int): ShortArray {
        val length = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val buffer = DoubleArray(length)
        val noise = Random(89L + index * 7L)
        tick(buffer, 0.0, bases[index], 1.0, noise)
        tick(buffer, ECHO_DELAY, bases[index] * 1.04, ECHO_GAIN, noise)
        var peak = 0.0
        for (v in buffer) peak = max(peak, abs(v))
        val gain = if (peak > 0.0) PEAK / peak else 0.0
        return ShortArray(length) { i -> (buffer[i] * gain * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun tick(buffer: DoubleArray, start: Double, base: Double, gain: Double, noise: Random) {
        val first = (start * SAMPLE_RATE).toInt()
        val twoPi = 2.0 * PI
        var smoothed = 0.0
        for (i in first until buffer.size) {
            val t = (i - first).toDouble() / SAMPLE_RATE
            val attack = min(t / 0.0002, 1.0)
            var tone = 0.0
            for (p in ratios.indices) {
                tone += amplitudes[p] * exp(-t / decays[p]) * sin(twoPi * base * ratios[p] * t)
            }
            smoothed += ((noise.nextDouble() * 2.0 - 1.0) - smoothed) * 0.55
            val snap = 0.8 * exp(-t / 0.0009) * smoothed
            buffer[i] += gain * attack * (tone + snap)
        }
    }
}
