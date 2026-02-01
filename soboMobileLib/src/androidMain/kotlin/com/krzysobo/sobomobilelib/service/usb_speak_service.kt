package com.krzysobo.sobomobilelib.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import java.util.Locale

class UsbSpeakService(private val context: Context, private val usbDevice: AudioDeviceInfo?) {
    private lateinit var tts: TextToSpeech
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentFocusRequest: AudioFocusRequest? = null  // Track for abandon

    fun initTts() {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set default audio attributes for the TTS engine (replaces setAudioStreamType)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)  // TTS-specific; favors USB/comm devices
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)  // Maps to notification for volume/focus
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)  // Ensures audibility over media
                    .build()

                val result = tts.setAudioAttributes(attrs)
                if (result == TextToSpeech.SUCCESS) {
                    println("TTS audio attributes set successfully")
                } else {
                    println("Failed to set TTS audio attributes: $result")
                }

                // Optional: Set language
                tts.language = Locale.US
            }
        }, "com.google.tts")  // Or your engine

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onStart(utteranceId: String?) {
                utteranceId?.let { setUsbForUtterance(it) }
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { clearUsbForUtterance(it) }
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onError(utteranceId: String?) {
                if (utteranceId != null) {
                    clearUsbForUtterance(utteranceId)
                }
            }
        })
    }

    fun speak(text: String, utteranceId: String = "default") {
        val params = Bundle().apply {
            // No KEY_PARAM_STREAM needed—attributes handle it
            // Generate unique session ID to scope USB routing to this utterance
            val sessionId = audioManager.generateAudioSessionId()
            putInt(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, sessionId)

            // Optional: Volume/pan (1.0f = full, 0.0f = center)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0.0f)
        }

        val speakResult = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (speakResult != TextToSpeech.SUCCESS) {
            println("TTS speak failed: $speakResult")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setUsbForUtterance(utteranceId: String) {
        usbDevice?.let { device ->
            // Request transient focus with matching attributes (ensures no ducking from media)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .build()

            currentFocusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { focusChange ->
                        // Optional: Handle loss (e.g., pause TTS if focus lost)
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            tts.stop()
                        }
                    }
                    .build()

            val focusGranted = audioManager.requestAudioFocus(currentFocusRequest!!)
            if (focusGranted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Set USB preferred for comm/sonification streams (affects this session only)
                val setSuccess = audioManager.setCommunicationDevice(device)
                println("USB routed for TTS utterance: ${if (setSuccess) "success" else "failed"}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun clearUsbForUtterance(utteranceId: String) {
        // Clear device preference and focus (resets to HDMI default)
        audioManager.clearCommunicationDevice()
        currentFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            currentFocusRequest = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun shutdown() {
        clearUsbForUtterance("")  // Clean up if active
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun playSpeechFile(uri: Uri) {
        val mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            prepare()

            val sessionId = audioManager.generateAudioSessionId()
            setAudioSessionId(sessionId)
            usbDevice?.let { setPreferredDevice(it) }

            setOnCompletionListener {
                audioManager.clearCommunicationDevice()
                release()
            }
            start()
        }
    }
}