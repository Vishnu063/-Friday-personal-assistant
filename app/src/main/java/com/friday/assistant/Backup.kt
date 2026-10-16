package com.friday.assistant

import org.json.JSONObject

data class BackupItem(val kind: String, val text: String, val done: Boolean, val due: Long)
data class BackupData(val items: List<BackupItem>, val chats: List<ChatLine>, val resume: String)

/** Parse and validate the entire file before touching any local data. No executable settings. */
object Backup {
    const val MAX_BYTES = 8 * 1024 * 1024
    val kinds = setOf("note", "task", "memory", "job", "career", "project", "practice")
    fun parse(text: String): BackupData {
        require(text.toByteArray().size <= MAX_BYTES) { "Backup is larger than 8 MB." }
        val root = JSONObject(text)
        require(root.optString("format") in setOf("friday-v1", "friday-v2")) { "Not a supported Friday backup." }
        val records = root.getJSONArray("entries")
        require(records.length() <= 2000) { "Restore supports at most 2,000 items per backup." }
        val entries = (0 until records.length()).map { index ->
            val item = records.getJSONObject(index)
            val kind = item.getString("kind")
            val body = item.getString("text")
            val due = item.optLong("due", 0)
            require(kind in kinds && body.isNotBlank() && body.length <= 16000 && due >= 0 && due <= 32503680000000L) { "Invalid item at position ${index + 1}." }
            BackupItem(kind, body, item.getBoolean("done"), due)
        }
        val messages = root.optJSONArray("chats")
        require((messages?.length() ?: 0) <= 100) { "Backup contains too many chat messages." }
        val chats = (0 until (messages?.length() ?: 0)).map { index ->
            val item = messages!!.getJSONObject(index)
            val role = item.getString("role"); val body = item.getString("text")
            require(role in setOf("You", "Friday", "Friday • AI suggestion") && body.length <= 16000) { "Invalid chat message." }
            ChatLine(role, body)
        }
        val resume = root.optString("resume", "")
        require(resume.length <= 12000) { "Resume text is too long." }
        return BackupData(entries, chats, resume)
    }
}
