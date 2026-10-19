package com.friday.assistant

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

/** Explicit substitutions only: no fuzzy changes to names, amounts or commands. */
object DictationText {
    fun vocabulary(source: String) = source.lineSequence().map { it.substringAfter("=>").trim() }
        .filter { it.isNotBlank() }.take(50).map { it.take(80) }.toList()
    fun clean(text: String, dictionary: String, fillers: Boolean): String {
        var value = text.take(6000)
        dictionary.lineSequence().take(50).forEach { line ->
            val parts = line.split("=>", limit = 2).map { it.trim().take(80) }
            if (parts.size == 2 && parts.all { it.isNotBlank() }) {
                val pattern = Regex("(?<![\\p{L}\\p{N}_])${Regex.escape(parts[0])}(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE)
                value = pattern.replace(value) { parts[1] }
            }
        }
        if (fillers) value = value.replace(Regex("(?i)(?<![\\p{L}\\p{N}_])(um|uh|erm)(?![\\p{L}\\p{N}_])[, ]*"), "")
        return value.replace(Regex("[ \\t]+"), " ").trim().take(6000)
    }
}

data class Transcript(val text: String, val time: Long, val id: String = java.util.UUID.randomUUID().toString())

class DictationSettings(context: Context) {
    private val prefs = context.getSharedPreferences("dictation", 0)
    var review by mutableStateOf(prefs.getBoolean("review", true)); private set
    var fillers by mutableStateOf(prefs.getBoolean("fillers", false)); private set
    var sounds by mutableStateOf(prefs.getBoolean("sounds", false)); private set
    var quiet by mutableStateOf(prefs.getBoolean("quiet", false)); private set
    var hold by mutableStateOf(prefs.getBoolean("hold", false)); private set
    var words by mutableStateOf(prefs.getString("words", "Vishnu\nKubernetes\nJenkins\nTerraform").orEmpty()); private set
    var retention by mutableIntStateOf(prefs.getInt("retention", 0).let { if (it in listOf(0, 1, 7)) it else 0 }); private set
    var history by mutableStateOf<List<Transcript>>(emptyList()); private set
    init { refresh() }
    fun flag(key: String, value: Boolean) {
        when (key) { "review" -> review = value; "fillers" -> fillers = value; "sounds" -> sounds = value; "quiet" -> quiet = value; "hold" -> hold = value; else -> return }
        prefs.edit().putBoolean(key, value).apply()
    }
    fun words(value: String) { words = value.take(8000); prefs.edit().putString("words", words).apply() }
    fun retention(days: Int) { retention = if (days in listOf(0, 1, 7)) days else 0; prefs.edit().putInt("retention", retention).apply(); refresh() }
    fun refresh() {
        val cutoff = System.currentTimeMillis() - retention * 86400000L
        history = if (retention == 0) emptyList() else runCatching {
            val data = JSONArray(prefs.getString("history", "[]"))
            (0 until data.length()).map { val item = data.getJSONObject(it); Transcript(item.getString("text").take(6000), item.getLong("time"), item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }) }
                .filter { it.time >= cutoff }.takeLast(20)
        }.getOrDefault(emptyList())
        persist()
    }
    fun remember(text: String, privateMode: Boolean) { refresh(); if (!privateMode && retention > 0) { history = (history + Transcript(text.take(6000), System.currentTimeMillis())).takeLast(20); persist() } }
    fun delete(id: String) { history = history.filterNot { it.id == id }; persist() }
    fun clear() { history = emptyList(); persist() }
    fun erase() { words(""); clear() }
    private fun persist() { val data = JSONArray(); history.forEach { data.put(JSONObject().put("text", it.text).put("time", it.time).put("id", it.id)) }; prefs.edit().putString("history", data.toString()).apply() }
}
