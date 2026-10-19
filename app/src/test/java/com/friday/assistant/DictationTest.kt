package com.friday.assistant

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONArray
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DictationTest {
    private lateinit var context: Context
    @Before fun setup() { context = RuntimeEnvironment.getApplication(); context.getSharedPreferences("dictation", 0).edit().clear().commit() }
    @Test fun cleanupPreservesNumbersAndWordBoundaries() {
        assertEquals("Kubernetes has 12 pods at 10:30, Kumar", DictationText.clean("cube has 12 pods at 10:30, Kumar", "cube => Kubernetes", true))
        assertEquals("aluminum humble 50", DictationText.clean("um, aluminum uh humble 50", "", true))
        assertEquals("VISHNU", DictationText.clean("VISHNU", "Vishnu", false)) // suggestions do not rewrite words
    }
    @Test fun dictionaryUsesLiteralPatternsAndReplacementText() {
        assertEquals("$5 C++", DictationText.clean("price c++", "price => $5\nc++ => C++", false))
        assertEquals("foocube cubelet", DictationText.clean("foocube cubelet", "cube => K", false))
        assertEquals(listOf("Kubernetes", "Vishnu"), DictationText.vocabulary("cube => Kubernetes\n\nVishnu"))
        assertEquals(6000, DictationText.clean("x".repeat(7000), "", false).length)
    }
    @Test fun historyIsOptInPrivateAwareBoundedAndDeletable() {
        val settings = DictationSettings(context)
        settings.remember("never stored", false); assertTrue(settings.history.isEmpty())
        settings.retention(1)
        settings.remember("private", true); assertTrue(settings.history.isEmpty())
        repeat(25) { settings.remember("phrase $it", false) }
        assertEquals(20, settings.history.size)
        assertEquals("phrase 5", DictationSettings(context).history.first().text)
        settings.delete(settings.history.first().time)
        assertFalse(settings.history.any { it.text == "phrase 5" })
        settings.retention(0)
        assertTrue(DictationSettings(context).history.isEmpty())
    }
    @Test fun expiredHistoryIsPrunedAndEraseRemovesPersonalWords() {
        val data = JSONArray().put(JSONObject().put("text", "expired").put("time", System.currentTimeMillis() - 8 * 86400000L))
        context.getSharedPreferences("dictation", 0).edit().putInt("retention", 7).putString("history", data.toString()).commit()
        val settings = DictationSettings(context)
        assertTrue(settings.history.isEmpty())
        settings.words("private vocabulary"); settings.remember("new", false)
        settings.erase()
        assertEquals("", DictationSettings(context).words)
        assertTrue(DictationSettings(context).history.isEmpty())
    }
    @Test fun optionsPersistAndDefaultToReview() {
        val settings = DictationSettings(context)
        assertTrue(settings.review)
        settings.flag("hold", true); settings.flag("sounds", true); settings.words("Jenkins")
        val reloaded = DictationSettings(context)
        assertTrue(reloaded.hold); assertTrue(reloaded.sounds); assertEquals("Jenkins", reloaded.words)
    }
}
