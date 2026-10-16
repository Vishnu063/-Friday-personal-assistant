package com.friday.assistant

import org.junit.Assert.*
import org.junit.Test

class PersonalToolsTest {
    @Test fun feedbackIsChecklistNotFabricatedHiringScore() {
        val feedback = Coach.review(0, "I compare df and du, then lsof for deleted files and check inode usage")
        assertTrue(feedback.contains("not an interview score"))
        assertTrue(feedback.contains("All checklist terms mentioned"))
        assertEquals("Saved", Coach.jobStatus("arbitrary status"))
        assertEquals("Interview", Coach.jobStatus("Interview"))
    }
    @Test fun downloadRejectsCredentialsAndNonHttpsOrLocalEndpoints() {
        listOf("http://example.com/model", "https://name:secret@example.com/model", "https://localhost/model", "https://127.0.0.1/model", "https://example.com:8443/model", "https://example.com/model#fragment").forEach {
            assertTrue(it, runCatching { ModelDownload.validateUrl(it) }.isFailure)
        }
        assertEquals("huggingface.co", ModelDownload.validateUrl("https://huggingface.co/example/model.litertlm").host)
    }
}
