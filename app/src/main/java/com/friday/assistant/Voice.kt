package com.friday.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
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
class Voice(private val context: Context, private val status: (String) -> Unit, private val result: (String) -> Unit, private val partial: (String) -> Unit = {}) {
    private val handler = Handler(Looper.getMainLooper())
    // Timers can be cancelled without dropping a queued TTS initialization callback.
    private val timers = Handler(Looper.getMainLooper())
    private val chunks = ArrayDeque<String>()
    private var playback = 0
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var turn = 0
    private var streaming = false
    private var spokenOffset = 0
    private var ready = false
    private var closed = false
    private var session = 0
    private var speechGeneration = 0
    private var utterance = ""
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focus: AudioFocusRequest? = null
    private var tone: ToneGenerator? = null
    private var finishRequested = false
    private var listeningReady = false
    private var preferSystemRecognizer = false
    var listening = false; private set
    var sounds = false
    var quiet = false
    var vocabulary: List<String> = emptyList()
    private fun beep(end: Boolean = false) {
        if (!sounds) return
        if (tone == null) tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35) }.getOrNull()
        tone?.startTone(if (end) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, 80)
    }
    private fun endInput() {
        listening = false; listeningReady = false; finishRequested = false; partial("")
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
    }
    fun finishListening() { if (listening) { finishRequested = true; if (listeningReady) recognizer?.stopListening() else status("Starting microphone… please wait.") } }
    var foreground = true
    var conversation = false
    var language = "en-IN"
    var offlineOnly = false
    var rate = 1f
    val onDevice get() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    private fun createTts() {
        val generation = ++speechGeneration
        tts = TextToSpeech(context) { code -> handler.post {
            if (!closed && generation == speechGeneration) {
                ready = code == TextToSpeech.SUCCESS
                if (!ready) {
                    clearPlayback(); tts?.shutdown(); tts = null
                    status("Install or select an Android text-to-speech engine, then retry.")
                } else if (foreground && chunks.isNotEmpty()) startPlayback()
                else if (!foreground) clearPlayback()
            }
        } }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { handler.post { if (id == utterance && foreground) status("Speaking • tap mic to interrupt") } }
            override fun onDone(id: String?) { handler.post {
                if (id == utterance && foreground && !closed) {
                    utterance = ""
                    if (chunks.isNotEmpty()) playChunk(TextToSpeech.QUEUE_ADD)
                    else finishPlayback()
                }
            } }
            @Deprecated("Android callback") override fun onError(id: String?) { handler.post {
                if (id == utterance && !closed) { clearPlayback(); conversation = false; status("Speech playback failed. Check your voice engine and retry.") }
            } }
        })
    }
    fun listen() {
        if (!foreground || closed) return
        if (offlineOnly && !onDevice) { status("On-device recognition is unavailable. Disable strict offline recognition or type instead."); return }
        endInput(); listening = true
        status("Starting microphone…")
        clearPlayback()
        val currentSession = ++session
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
        if (!SpeechRecognizer.isRecognitionAvailable(context) && !onDevice) { endInput(); status("No speech recognizer installed. You can type instead."); return }
        if (quiet) {
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setOnAudioFocusChangeListener { change -> if (change < 0) stop() }.build()
            val granted = runCatching { audio.requestAudioFocus(focus!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED }.getOrDefault(false)
            if (!granted) { endInput(); status("Audio focus unavailable. Pause other media or turn off Ask other media to pause, then retry."); return }
        }
        val useOnDevice = onDevice && (offlineOnly || !preferSystemRecognizer) && Build.VERSION.SDK_INT >= 31
        recognizer = runCatching {
            if (useOnDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
        }.getOrElse {
            endInput()
            if (useOnDevice && !offlineOnly && !preferSystemRecognizer) {
                preferSystemRecognizer = true
                listen()
            } else status("Could not open the speech recognizer. Check Android speech settings.")
            return
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (currentSession == session) {
                listeningReady = true; beep()
                status(if (useOnDevice) "Listening • on-device" else "Listening • system provider may use internet")
                if (finishRequested) recognizer?.stopListening()
            } }
            override fun onBeginningOfSpeech() { if (currentSession == session) status("Hearing you…") }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { if (currentSession == session) status("Transcribing…") }
            override fun onError(error: Int) {
                if (currentSession != session || !foreground || closed) return
                session++; endInput()
                // Some phones report an on-device provider but close it immediately.
                // Retry once using Android's default recognizer, unless strict offline was requested.
                if (useOnDevice && !preferSystemRecognizer && !offlineOnly &&
                    (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER ||
                     error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                     error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)) {
                    preferSystemRecognizer = true
                    recognizer?.destroy(); recognizer = null
                    status("On-device speech unavailable. Trying system recognizer…")
                    val retrySession = session
                    handler.post { if (foreground && !closed && session == retrySession) listen() }
                    return
                }
                status(when (error) { SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap mic to retry."; SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice."; else -> "Voice unavailable (code $error). Try typing or changing language." })
                recognizer?.destroy(); recognizer = null
            }
            override fun onResults(results: Bundle?) {
                if (!foreground || closed || currentSession != session) return
                session++
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                recognizer?.destroy(); recognizer = null; endInput(); beep(true); status("Ready")
                if (text != null) result(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (currentSession == session && listening && foreground && !closed) partial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().take(6000))
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        runCatching { recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, language).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offlineOnly).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true).apply { if (Build.VERSION.SDK_INT >= 33) putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(vocabulary)) }) }
            .onFailure { recognizer?.destroy(); recognizer = null; endInput(); status("Could not start voice. Try typing.") }
    }
    fun speak(text: String) {
        if (!foreground || closed || text.isBlank()) return
        if (listening) { session++; recognizer?.cancel(); recognizer?.destroy(); recognizer = null; endInput() }
        clearPlayback()
        enqueue(text)
    }

    fun beginReply(): Int {
        clearPlayback()
        streaming = true
        ensureSpeechEngine()
        return turn
    }

    fun updateReply(reply: Int, text: String?, finished: Boolean) {
        if (reply != turn || !streaming || !foreground || closed) return
        if (text == null) {
            clearPlayback()
            return
        }
        val end = if (finished) text.length else SpeechText.completePrefix(text, spokenOffset)
        val part = text.substring(spokenOffset, end)
        spokenOffset = end
        streaming = !finished
        if (part.isNotBlank()) enqueue(part)
        else if (finished && utterance.isEmpty() && chunks.isEmpty()) finishPlayback()
    }

    private fun clearPlayback() {
        turn++
        streaming = false
        spokenOffset = 0
        utterance = ""
        chunks.clear()
        timers.removeCallbacksAndMessages(null)
        tts?.stop()
    }

    private fun finishPlayback() {
        if (streaming) return
        status("Ready")
        val completedTurn = turn
        if (conversation) {
            timers.postDelayed({
                if (conversation && foreground && turn == completedTurn) listen()
            }, 250)
        } else {
            timers.postDelayed({ if (!conversation && turn == completedTurn) releaseSpeech() }, 30000)
        }
    }

    private fun enqueue(text: String) {
        chunks.addAll(SpeechText.chunks(text, TextToSpeech.getMaxSpeechInputLength()))
        ensureSpeechEngine()
        if (ready && utterance.isEmpty()) startPlayback()
    }

    private fun ensureSpeechEngine() {
        if (tts == null) {
            runCatching { createTts() }.onFailure {
                clearPlayback(); tts = null; ready = false
                status("Could not start the speech engine. Check Android voice settings.")
            }
        }
    }

    private fun startPlayback() {
        val engine = tts ?: return
        if (engine.setLanguage(Locale.forLanguageTag(language)) < 0) { clearPlayback(); status("Install the $language voice in Android TTS settings."); return }
        if (offlineOnly) {
            val local = engine.voices?.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language == Locale.forLanguageTag(language).language }
            if (local == null) { clearPlayback(); status("No offline TTS voice for $language. Install one in Android voice settings."); return }
            if (engine.setVoice(local) == TextToSpeech.ERROR) { clearPlayback(); status("Could not activate the offline voice for $language."); return }
        }
        engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        engine.setSpeechRate(rate.coerceIn(.6f, 1.4f))
        playChunk(TextToSpeech.QUEUE_FLUSH)
    }
    private fun playChunk(mode: Int) {
        val engine = tts ?: return
        if (!foreground || closed || chunks.isEmpty()) return
        utterance = "friday-${++playback}"
        if (engine.speak(chunks.removeFirst(), mode, null, utterance) == TextToSpeech.ERROR) {
            clearPlayback(); conversation = false
            status("Speech engine could not play this reply. Check Android voice settings.")
        }
    }
    fun stop() { endInput(); clearPlayback(); session++; conversation = false; recognizer?.cancel(); recognizer?.destroy(); recognizer = null; status("Ready") }
    fun releaseSpeech() { stop(); speechGeneration++; tts?.shutdown(); tts = null; ready = false }
    fun health() = "Recognition: ${if (onDevice) "on-device available" else if (SpeechRecognizer.isRecognitionAvailable(context)) "system provider available" else "missing"}\nTTS: ${if (ready) "loaded" else "loads when needed"}"
    fun destroy() { closed = true; releaseSpeech(); handler.removeCallbacksAndMessages(null); tone?.release(); tone = null }
}
