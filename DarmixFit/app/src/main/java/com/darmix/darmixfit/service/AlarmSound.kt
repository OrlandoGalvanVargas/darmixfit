package com.darmix.darmixfit.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Reproduce un aviso breve por el canal de alarma y avisa cuando termina. */
class AlarmSound(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var focusRequest: AudioFocusRequest? = null
    private var onDone: (() -> Unit)? = null
    private var done = false

    fun play(maxMillis: Long, onDone: () -> Unit) {
        this.onDone = onDone
        requestFocus()
        handler.postDelayed({ finish() }, maxMillis) // corte duro: nunca suena más de esto
        if (!startRingtone()) startFallbackTone()
    }

    /** Libera todo sin avisar a nadie. */
    fun release() {
        done = true
        onDone = null
        cleanup()
    }

    private fun startRingtone(): Boolean {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return false
        return try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(attributes)
            mp.setDataSource(context, uri)
            mp.isLooping = false
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ ->
                releasePlayer()
                startFallbackTone()
                true
            }
            mp.prepareAsync()
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo usar el tono del sistema", e)
            releasePlayer()
            false
        }
    }

    private fun startFallbackTone() {
        try {
            val generator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tone = generator
            generator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2500)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Tampoco funcionó el tono de respaldo", e)
            finish()
        }
    }

    private fun requestFocus() {
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
        releasePlayer()
        tone?.release()
        tone = null
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun releasePlayer() {
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
    }

    private companion object {
        const val TAG = "AlarmSound"
    }
}