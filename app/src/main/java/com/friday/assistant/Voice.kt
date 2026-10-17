package com.friday.assistant

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Speech engines are created only when used and released on background/idle. */
class Voice(private val context: Context, private val status: (String) -> Unit, private val result: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var pendingSpeech: String? = null
    private var ready = false
    private var closed = false
    private var session = 0
    private var speechGeneration = 0
    private var utterance = ""
    var foreground = true
    var conversation = false
    var language = "en-IN"
    var offlineOnly = false
    var rate = 1f
    val onDevice get() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    private fun createTts() {
        val generation = ++speechGeneration
        tts = TextToSpeech(context) { code -> handler.post {
            if (!closed && foreground && generation == speechGeneration) {
                ready = code == TextToSpeech.SUCCESS
                val queued = pendingSpeech; pendingSpeech = null
                if (!ready) status("Install an Android text-to-speech engine in phone Settings.") else if (queued != null) speak(queued)
            }
        } }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { handler.post { if (id == utterance && foreground) status("Speaking • tap mic to interrupt") } }
            override fun onDone(id: String?) { handler.post {
                if (id == utterance && foreground && !closed) {
                    status("Ready")
                    if (conversation) handler.postDelayed({ if (conversation && foreground && id == utterance) listen() }, 600)
                    else handler.postDelayed({ if (!conversation && id == utterance) releaseSpeech() }, 30000)
                }
            } }
            @Deprecated("Android callback") override fun onError(id: String?) { handler.post { if (id == utterance) status("Speech playback failed. Check your voice engine.") } }
        })
    }
    fun listen() {
        if (!foreground || closed) return
        if (offlineOnly && !onDevice) { status("On-device recognition is unavailable. Disable strict offline recognition or type instead."); return }
        utterance = ""; pendingSpeech = null
        val currentSession = ++session
        handler.removeCallbacksAndMessages(null); tts?.stop()
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
        if (!SpeechRecognizer.isRecognitionAvailable(context) && !onDevice) { status("No speech recognizer installed. You can type instead."); return }
        recognizer = if (onDevice && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (currentSession == session) status(if (onDevice) "Listening • on-device" else "Listening • system provider may use internet") }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { if (currentSession == session) status("Transcribing…") }
            override fun onError(error: Int) {
                if (currentSession != session || !foreground || closed) return
                status(when (error) { SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap mic to retry."; SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice."; else -> "Voice unavailable (code $error). Try typing or changing language." })
                recognizer?.destroy(); recognizer = null
            }
            override fun onResults(results: Bundle?) {
                if (!foreground || closed || currentSession != session) return
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                recognizer?.destroy(); recognizer = null; status("Ready")
                if (text != null) result(text)
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        runCatching { recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, language).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)) }
            .onFailure { status("Could not start voice. Try typing.") }
    }
    fun speak(text: String) {
        if (!foreground || closed || text.isBlank()) return
        if (tts == null) { pendingSpeech = text; createTts(); return }
        if (!ready) { pendingSpeech = text; return }
        val engine = tts ?: return
        if (engine.setLanguage(Locale.forLanguageTag(language)) < 0) { status("Install the $language voice in Android TTS settings."); return }
        if (offlineOnly) {
            val local = engine.voices?.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language == Locale.forLanguageTag(language).language }
            if (local == null) { status("No offline TTS voice for $language. Install one in Android voice settings."); return }; engine.voice = local
        }
        engine.setSpeechRate(rate); utterance = "friday-${session}-${System.nanoTime()}"
        if (engine.speak(text.take(3900), TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) status("Speech engine could not play this reply.")
    }
    fun stop() { utterance = ""; pendingSpeech = null; session++; conversation = false; handler.removeCallbacksAndMessages(null); recognizer?.cancel(); recognizer?.destroy(); recognizer = null; tts?.stop(); status("Ready") }
    fun releaseSpeech() { stop(); speechGeneration++; tts?.shutdown(); tts = null; ready = false }
    fun health() = "Recognition: ${if (onDevice) "on-device available" else if (SpeechRecognizer.isRecognitionAvailable(context)) "system provider available" else "missing"}\nTTS: ${if (ready) "loaded" else "loads when needed"}"
    fun destroy() { releaseSpeech(); closed = true }
}
