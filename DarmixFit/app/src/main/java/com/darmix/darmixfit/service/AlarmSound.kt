package com.darmix.darmixfit.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class AlarmSound(context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private var track: AudioTrack? = null
    private var tone: ToneGenerator? = null
    private var focusRequest: AudioFocusRequest? = null
    private var onDone: (() -> Unit)? = null
    private var done = false

    fun play(maxMillis: Long, onDone: () -> Unit) {
        this.onDone = onDone
        val personal = personalOutput()
        val attributes = attributesFor(personal != null)
        requestFocus(attributes)
        handler.postDelayed({ finish() }, maxMillis)
        if (startChime(attributes, personal)) {
            handler.postDelayed({ finish() }, minOf(maxMillis, DarmixChime.durationMillis + END_MARGIN_MS))
        } else {
            startFallbackTone(personal != null)
        }
    }

    fun release() {
        done = true
        onDone = null
        cleanup()
    }

    private fun personalOutput(): AudioDeviceInfo? {
        val types = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AudioDeviceInfo.TYPE_BLE_HEADSET)
                add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
            }
        }
        val device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type in types }
            ?: return null
        val mediaAudible = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
        return if (mediaAudible) device else null
    }

    private fun attributesFor(personal: Boolean): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(if (personal) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun startChime(attributes: AudioAttributes, device: AudioDeviceInfo?): Boolean {
        return try {
            val pcm = DarmixChime.pcm
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(DarmixChime.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track = audioTrack
            val written = audioTrack.write(pcm, 0, pcm.size)
            if (written <= 0 || audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                releaseTrack()
                return false
            }
            if (device != null) audioTrack.setPreferredDevice(device)
            audioTrack.play()
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo reproducir la campana", e)
            releaseTrack()
            false
        }
    }

    private fun startFallbackTone(personal: Boolean) {
        try {
            val stream = if (personal) AudioManager.STREAM_MUSIC else AudioManager.STREAM_ALARM
            val generator = ToneGenerator(stream, 100)
            tone = generator
            generator.startTone(ToneGenerator.TONE_PROP_ACK, 1200)
            handler.postDelayed({ finish() }, 1400)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Tampoco funcionó el tono de respaldo", e)
            finish()
        }
    }

    private fun requestFocus(attributes: AudioAttributes) {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun finish() {
        if (done) return
        done = true
        val callback = onDone
        onDone = null
        cleanup()
        callback?.invoke()
    }

    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        releaseTrack()
        tone?.release()
        tone = null
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun releaseTrack() {
        track?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        track = null
    }

    private companion object {
        const val TAG = "AlarmSound"
        const val END_MARGIN_MS = 150L
    }
}
