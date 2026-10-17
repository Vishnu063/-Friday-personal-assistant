package com.friday.assistant
import org.junit.Assert.*
import org.junit.Test
class MemoryPolicyTest {
    private fun entry(id: String, text: String, kind: String = "memory") = Entry(id, kind, text, false, 0)
    @Test fun selectsRelevantPreferencesWithStrictBudget() {
        val candidates = listOf(entry("a", "I like tea"), entry("b", "Use Kotlin for Android"), entry("c", "Kotlin secret", "note"))
        assertEquals("b", MemoryPolicy.select("Help with Kotlin", candidates).first().id)
        assertTrue(MemoryPolicy.select("Kotlin", candidates, 8).sumOf { it.text.length } <= 8)
        assertTrue(MemoryPolicy.select("Kotlin", candidates, 0).isEmpty())
        assertTrue(MemoryPolicy.select("Kotlin", candidates).all { it.kind == "memory" })
    }
    @Test fun boundsCandidateCountAndCompactContext() {
        val candidates = (1..100).map { entry(it.toString(), "Kotlin ".repeat(200)) }
        val selected = MemoryPolicy.select("Kotlin", candidates)
        assertTrue(selected.size <= 4)
        assertTrue(selected.sumOf { it.text.length } <= 900)
        assertTrue(MemoryPolicy.maxTokens(true) < MemoryPolicy.maxTokens(false))
    }
}
