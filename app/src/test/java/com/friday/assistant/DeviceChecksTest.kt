package com.friday.assistant

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DeviceChecksTest {
    private lateinit var context: Context
    @Before fun setup() { context=RuntimeEnvironment.getApplication(); DeviceChecks(context).clear() }
    @Test fun checksStartUntestedAndPersistOnlyExplicitResults() {
        val checks=DeviceChecks(context)
        assertTrue(checks.checks.keys.all { checks.state(it)=="not_tested" })
        checks.mark("voice","passed")
        assertEquals("passed",DeviceChecks(context).state("voice"))
        assertEquals("not_tested",checks.state("headset"))
        assertTrue(checks.report().contains("Owner-recorded"))
        checks.clear(); assertEquals("not_tested",DeviceChecks(context).state("voice"))
    }
    @Test fun previousVersionResultsRequireRetesting() {
        val checks=DeviceChecks(context); checks.mark("microphone","failed")
        context.getSharedPreferences("device_checks",0).edit().putLong("microphone.version",checks.version-1).commit()
        assertEquals("retest_after_update",DeviceChecks(context).state("microphone"))
        try { checks.mark("voice","automatically_certified"); fail("Invalid result accepted") } catch (_: IllegalArgumentException) {}
    }
}
