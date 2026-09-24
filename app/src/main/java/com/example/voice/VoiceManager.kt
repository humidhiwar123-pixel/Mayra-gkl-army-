package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private var currentLanguageLocale: Locale = Locale("hi", "IN")
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                            _liveTranscript.value = ""
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            _rmsDb.value = rmsdB
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = VoiceState.PROCESSING
                        }

                        override fun onError(error: Int) {
                            _voiceState.value = VoiceState.IDLE
                            val errDesc = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue in recognition"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                else -> "Recognition error #$error"
                            }
                            _liveTranscript.value = errDesc
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            _liveTranscript.value = text
                            _voiceState.value = VoiceState.PROCESSING
                            if (text.isNotBlank()) {
                                onSpeechRecognized(text)
                            } else {
                                _voiceState.value = VoiceState.IDLE
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            _liveTranscript.value = matches?.firstOrNull() ?: ""
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            speechRecognizer = null
        }
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    applyVoiceSettings(currentLanguageLocale, speechRate, speechPitch)
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _voiceState.value = VoiceState.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            _voiceState.value = VoiceState.IDLE
                        }

                        override fun onError(utteranceId: String?) {
                            _voiceState.value = VoiceState.IDLE
                        }
                    })
                }
            }
        } catch (e: Exception) {
            textToSpeech = null
            isTtsInitialized = false
        }
    }

    fun applyVoiceSettings(locale: Locale, rate: Float = 1.0f, pitch: Float = 1.0f) {
        currentLanguageLocale = locale
        speechRate = rate
        speechPitch = pitch
        if (isTtsInitialized && textToSpeech != null) {
            val result = textToSpeech?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setSpeechRate(rate)
            textToSpeech?.setPitch(pitch)
        }
    }

    fun startListening(languageMode: String = "hinglish") {
        stopSpeaking()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            val langTag = when (languageMode.lowercase()) {
                "hindi" -> "hi-IN"
                "english" -> "en-IN"
                else -> "hi-IN" // Hinglish understands Hindi & English phonemes
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized || textToSpeech == null) return
        stopSpeaking()

        // Clean out emojis and markdown symbols for natural TTS speech
        val cleaned = text.replace(Regex("[*#_~`⚡❌⚠️🔒•]"), "")
            .replace(Regex("http\\S+"), "link")
            .take(600) // keep within comfortable spoken utterance length

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "myra_utterance_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }

    fun stopSpeaking() {
        if (textToSpeech != null && textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
