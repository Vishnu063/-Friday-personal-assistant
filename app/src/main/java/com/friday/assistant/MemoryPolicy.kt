package com.friday.assistant

/** Bounded retrieval of user-saved preferences; does not send the whole personal database. */
object MemoryPolicy {
    fun select(question: String, candidates: List<Entry>, maxChars: Int = 900): List<Entry> {
        val words = Regex("[\\p{L}\\p{N}]{3,}").findAll(question.lowercase()).map { it.value }.toSet() - setOf("the", "and", "what", "that", "with", "please", "friday")
        val ranked = candidates.filter { it.kind == "memory" && !it.done }.take(64).mapIndexed { index, entry ->
            val text = entry.text.lowercase()
            Triple(entry, words.count { word -> Regex("(?<![\\p{L}\\p{N}])${Regex.escape(word)}(?![\\p{L}\\p{N}])").containsMatchIn(text) }, index)
        }.sortedWith(compareByDescending<Triple<Entry, Int, Int>> { it.second }.thenBy { it.third })
        var remaining = maxChars.coerceIn(0, 1800)
        return ranked.filterIndexed { index, item -> item.second > 0 || index == 0 }.take(4).mapNotNull { item ->
            val text = item.first.text.take(minOf(remaining, 300))
            remaining -= text.length
            if (text.isBlank()) null else item.first.copy(text = text)
        }
    }
    fun maxTokens(compact: Boolean) = if (compact) 2048 else 4096
    fun maxReplyTokens(compact: Boolean) = if (compact) 256 else 512
}
