package com.friday.assistant

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LinkedInConnectionTest {
    private fun fails(block: () -> Unit) { try { block(); fail("Expected refusal") } catch (_: IllegalArgumentException) {} }
    @Test fun tokenOriginCannotContainCredentialsPathsOrRedirectTargets() {
        assertEquals("https://companion.example:8443", LinkedInWire.origin("https://companion.example:8443/"))
        for (url in listOf("http://companion.example", "https://user:secret@example.com", "https://example.com/path", "https://example.com?token=secret", "https://example.com#fragment")) fails { LinkedInWire.origin(url) }
    }
    @Test fun companionAccountAndRecipientMustMatchTheReviewedRequest() {
        assertEquals("Owner" to "OWNER", LinkedInWire.account(JSONObject().put("mode", "read_only").put("name", "Owner").put("account_id", "OWNER")))
        fails { LinkedInWire.account(JSONObject().put("mode","send").put("name","Owner").put("account_id","OWNER")) }
        val profile = "https://www.linkedin.com/in/mira"
        val good = JSONObject().put("profile",profile).put("name","Mira").put("account_id","OWNER").put("state","not_found_unverified")
        assertEquals("not_found_unverified", LinkedInWire.check(good,profile,"OWNER").getString("state"))
        fails { LinkedInWire.check(good,profile,"DIFFERENT_ACCOUNT") }
        fails { LinkedInWire.check(good,"https://www.linkedin.com/in/another","OWNER") }
        fails { LinkedInWire.check(JSONObject(good.toString()).put("state","eligible"),profile,"OWNER") }
    }
}
