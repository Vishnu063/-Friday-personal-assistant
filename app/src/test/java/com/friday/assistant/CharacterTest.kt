package com.friday.assistant

import android.app.Application
import android.content.Intent
import android.provider.Settings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CharacterTest {
    private lateinit var context: Application
    @Before fun setup() {
        org.robolectric.shadows.ShadowLog.stream = System.out
        context=RuntimeEnvironment.getApplication()
        CharacterPreferences(context).clear()
        ShadowSettings.setCanDrawOverlays(false)
    }
    @Test fun tenSelectableCharactersIncludeFiveBoysAndFiveGirls() {
        assertEquals(10, FridayCharacters.all.map { it.id }.distinct().size)
        assertEquals(5, FridayCharacters.all.count { it.girl })
        assertEquals(5, FridayCharacters.all.count { !it.girl })
        for (character in FridayCharacters.all) {
            CharacterPreferences(context).select(character.id)
            assertEquals(character, CharacterPreferences(context).selected)
        }
    }
    @Test fun selectionSurvivesRecreationAndCorruptOrErasedPreferencesRecover() {
        val prefs=CharacterPreferences(context); prefs.select("nia")
        assertEquals("nia",CharacterPreferences(context).selected.id)
        try { prefs.select("unknown"); fail("Unknown selection accepted") } catch (_: IllegalArgumentException) {}
        assertEquals("nia",prefs.selected.id)
        context.getSharedPreferences("friday_character",0).edit().putString("selected","obsolete").commit()
        assertEquals("kai",prefs.selected.id)
        prefs.clear(); assertEquals("kai",prefs.selected.id)
    }
    @Test fun draggingStaysReachableAfterRotationOrSmallWindow() {
        assertEquals(0,CharacterPosition.clamp(-500,1080,100))
        assertEquals(980,CharacterPosition.clamp(1400,1080,100))
        assertEquals(0,CharacterPosition.clamp(500,80,100))
        assertEquals(420,CharacterPosition.clamp(420,1080,100))
    }
    @Test fun deniedOverlayAccessDoesNotStartService() {
        assertFalse(Settings.canDrawOverlays(context))
        assertTrue(CharacterOverlayService.start(context).contains("Allow"))
        assertFalse(CharacterOverlayService.running)
    }
    @Test fun foregroundCharacterCanStopAndDoesNotRestartAfterTaskRemoval() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller=Robolectric.buildService(CharacterOverlayService::class.java).create()
        val service=controller.get()
        service.onStartCommand(Intent(context,CharacterOverlayService::class.java),0,1)
        assertTrue(CharacterOverlayService.running)
        service.onTaskRemoved(Intent())
        assertTrue(shadowOf(service).isStoppedBySelf)
        controller.destroy()
        assertFalse(CharacterOverlayService.running)
    }
    @Test fun stopActionNeverCreatesAnOverlay() {
        val controller=Robolectric.buildService(CharacterOverlayService::class.java).create()
        val service=controller.get()
        service.onStartCommand(Intent().setAction(CharacterOverlayService.ACTION_STOP),0,1)
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertFalse(CharacterOverlayService.running)
        controller.destroy()
    }
}
