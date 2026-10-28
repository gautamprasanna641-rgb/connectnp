package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Text-to-Speech manager configured for bilingual Nepali and English speech.
 */
class ConnectNpTextToSpeech(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _currentlySpeakingMessageId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    if (_currentlySpeakingMessageId.value == utteranceId) {
                        _currentlySpeakingMessageId.value = null
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (_currentlySpeakingMessageId.value == utteranceId) {
                        _currentlySpeakingMessageId.value = null
                    }
                }
            })
            Log.d("ConnectNpTTS", "TextToSpeech engine initialized successfully")
        } else {
            Log.e("ConnectNpTTS", "TextToSpeech initialization failed with status $status")
        }
    }

    /**
     * Speaks the provided text in the user's preferred language (Nepali or English).
     */
    fun speak(messageId: String, text: String, isNepali: Boolean) {
        if (!isInitialized || tts == null) {
            Log.w("ConnectNpTTS", "TextToSpeech not initialized yet")
            return
        }

        // Toggle: if already speaking this message, stop it
        if (_currentlySpeakingMessageId.value == messageId) {
            stop()
            return
        }

        stop()

        // Configure language: Nepali (ne-NP) with Devanagari fallback or English (en-US)
        if (isNepali) {
            val neLocale = Locale("ne", "NP")
            val neResult = tts?.setLanguage(neLocale)
            if (neResult == TextToSpeech.LANG_MISSING_DATA || neResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val hiLocale = Locale("hi", "IN")
                val hiResult = tts?.setLanguage(hiLocale)
                if (hiResult == TextToSpeech.LANG_MISSING_DATA || hiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
        } else {
            tts?.setLanguage(Locale.US)
        }

        tts?.setPitch(1.0f)
        tts?.setSpeechRate(0.95f)

        _currentlySpeakingMessageId.value = messageId
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stop() {
        tts?.stop()
        _currentlySpeakingMessageId.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
