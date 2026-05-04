package com.signlanguage.translator.domain.services

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin lifecycle wrapper around Android TextToSpeech for translated sentence playback.
 */
class TextToSpeechService(context: Context) : TextToSpeech.OnInitListener {
    private var isReady = false
    private val textToSpeech: TextToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            textToSpeech.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    fun release() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        isReady = false
    }

    companion object {
        private const val UTTERANCE_ID = "translation_sentence"
    }
}
