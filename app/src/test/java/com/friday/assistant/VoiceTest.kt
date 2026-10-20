package com.friday.assistant

import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

/** Android speech-service simulations, not recorded microphone or acoustic tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class VoiceTest {
    private lateinit var context: Context
    private lateinit var voice: Voice
    private val states = mutableListOf<String>()
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        voice = Voice(context, { states.add(it) }, {})
        ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("en-IN"))
        ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("hi-IN"))
    }
    @After fun cleanup() { voice.destroy() }
    private fun initialize(code: Int = TextToSpeech.SUCCESS): ShadowTextToSpeech {
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        val shadow = shadowOf(engine)
        shadow.onInitListener.onInit(code)
        shadowOf(Looper.getMainLooper()).idle()
        return shadow
    }
    @Test fun stoppedInitializationStillAllowsTheNextReply() {
        voice.speak("Interrupted before initialization")
        val shadow = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        shadow.onInitListener.onInit(TextToSpeech.SUCCESS)
        voice.stop() // regression: must not remove the queued engine initialization callback
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(shadow.spokenTextList.isEmpty())
        voice.speak("Next reply works")
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Next reply works", shadow.lastSpokenText)
    }
    @Test fun longHindiAndEnglishRepliesAreNotTruncated() {
        val text = "Hello, Kubernetes. नमस्ते फ्राइडे। 🌻 ".repeat(300)
        voice.language = "hi-IN"
        voice.speak(text)
        val shadow = initialize()
        assertEquals(text, shadow.spokenTextList.joinToString(""))
        assertTrue(shadow.spokenTextList.size > 1)
        assertTrue(shadow.spokenTextList.all { it.length <= TextToSpeech.getMaxSpeechInputLength() })
        assertEquals("hi", shadow.currentLanguage.language)
    }
    @Test fun stopCancelsQueuedPartsAndCompletionCannotRestartThem() {
        voice.speak("First ".repeat(2000))
        val shadow = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        shadow.onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).runOneTask() // initialize and enqueue only the first part
        val count = shadow.spokenTextList.size
        assertEquals(1, count)
        voice.stop()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(count, shadow.spokenTextList.size)
        assertTrue(shadow.isStopped)
    }
    @Test fun failedEngineInitializationCanBeRetried() {
        voice.speak("First attempt")
        val failed = ShadowTextToSpeech.getLastTextToSpeechInstance()
        val shadow = initialize(TextToSpeech.ERROR)
        assertTrue(shadow.isShutdown)
        assertTrue(states.any { it.contains("retry") })
        voice.speak("Retry works")
        assertNotSame(failed, ShadowTextToSpeech.getLastTextToSpeechInstance())
        assertEquals("Retry works", initialize().lastSpokenText)
    }
    @Test fun backgroundAndDestroyedVoiceNeverStartPlayback() {
        voice.speak("Queued before background")
        voice.foreground = false
        val shadow = initialize()
        assertTrue(shadow.spokenTextList.isEmpty())
        voice.speak("Do not play in background")
        assertTrue(shadow.spokenTextList.isEmpty())
        voice.destroy(); voice.foreground = true; voice.speak("Do not restart a destroyed voice")
        assertTrue(shadow.spokenTextList.isEmpty())
    }
    @Test fun unsupportedLanguageGivesAnActionableError() {
        voice.language = "xx-XX"; voice.speak("Unavailable voice")
        val shadow = initialize()
        assertTrue(shadow.spokenTextList.isEmpty())
        assertTrue(states.any { it.contains("Install the xx-XX voice") })
    }
    @Test fun strictOfflineRejectsMissingRecognizerInsteadOfListeningOnline() {
        voice.offlineOnly = true
        voice.listen()
        assertFalse(voice.listening)
        assertTrue(states.last().contains("On-device recognition is unavailable"))
    }
    @Test fun splitterPreservesEveryCharacterAndSurrogatePair() {
        val text = "a🌻b नमस्ते ".repeat(10)
        val chunks = SpeechText.chunks(text, 4)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 4 && !Character.isHighSurrogate(it.last()) && !Character.isLowSurrogate(it.first()) })
        assertTrue(SpeechText.chunks("  ", 4).isEmpty())
    }
}
