package com.friday.assistant

/** Preserve the full text and Unicode pairs while respecting the Android engine's input limit. */
object SpeechText {
    private val sentenceEnd = Regex("[.!?।॥！？。](?=\\s)|\\n")

    /** Partial model output is cumulative. Keep the unfinished sentence for the next update. */
    fun completePrefix(text: String, start: Int): Int =
        sentenceEnd.findAll(text, start).lastOrNull()?.range?.last?.plus(1) ?: start

    fun chunks(text: String, limit: Int): List<String> {
        require(limit >= 2)
        if (text.isBlank()) return emptyList()
        val parts = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + limit, text.length)
            if (end < text.length && Character.isHighSurrogate(text[end - 1]) && Character.isLowSurrogate(text[end])) end--
            if (end < text.length) {
                val boundary = (end - 1 downTo start + (end - start) / 2).firstOrNull { text[it].isWhitespace() }
                if (boundary != null) end = boundary + 1
            }
            parts += text.substring(start, end)
            start = end
        }
        return parts
    }
}
