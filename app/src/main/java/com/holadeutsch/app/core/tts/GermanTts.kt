package com.holadeutsch.app.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Thin wrapper around Android TTS locked to German.
 * Degrades gracefully (speaker buttons hide) if no German voice is installed.
 */
class GermanTts(context: Context) {

    var isReady by mutableStateOf(false)
        private set

    /** Mirrors the user setting; synced from StatsRepository. */
    var enabled by mutableStateOf(true)

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.GERMAN)
                isReady = result == TextToSpeech.LANG_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            }
        }
    }

    val available: Boolean get() = isReady && enabled

    fun speak(text: String) {
        if (available) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "holadeutsch")
        }
    }
}
