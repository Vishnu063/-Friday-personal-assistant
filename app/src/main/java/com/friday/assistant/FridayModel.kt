package com.friday.assistant

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class FridayModel(application: Application) : AndroidViewModel(application) {
    val store = Store(application)
    val brain = LocalBrain(application)
    private val prefs = application.getSharedPreferences("friday", 0)
    var entries by mutableStateOf(store.entries()); private set
    var messages by mutableStateOf(store.chats()); private set
    var busy by mutableStateOf(false); private set
    var streaming by mutableStateOf(""); private set
    var notice by mutableStateOf("")
    var speechText by mutableStateOf(""); private set
    var modelName by mutableStateOf(if (brain.installed()) prefs.getString("model", "Imported model").orEmpty() else "No model installed"); private set
    var language by mutableStateOf(prefs.getString("language", "en-IN").orEmpty()); private set
    var speakReplies by mutableStateOf(prefs.getBoolean("speak", true)); private set
    var useMemory by mutableStateOf(prefs.getBoolean("memory", true)); private set
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false)); private set
    var draftDocument by mutableStateOf("")
    var resume by mutableStateOf(prefs.getString("resume", "").orEmpty()); private set
    var accentMint by mutableStateOf(prefs.getBoolean("mint", false)); private set
    fun onboard() { onboarded = true; prefs.edit().putBoolean("onboarded", true).apply() }
    fun language(value: String) { language = value; prefs.edit().putString("language", value).apply() }
    fun speech(value: Boolean) { speakReplies = value; prefs.edit().putBoolean("speak", value).apply() }
    fun memory(value: Boolean) { useMemory = value; prefs.edit().putBoolean("memory", value).apply() }
    fun theme(value: Boolean) { accentMint = value; prefs.edit().putBoolean("mint", value).apply() }
    fun resume(value: String) { resume = value.take(12000); prefs.edit().putString("resume", resume).apply() }
    fun refresh() { entries = store.entries() }
    fun save(kind: String, text: String, due: Long = 0) {
        if (text.isBlank()) { notice = "Enter something to save."; return }
        val id = store.add(kind, text.trim(), due)
        if (due > 0) runCatching { Reminders.schedule(getApplication(), id, due) }.onFailure { notice = "Task saved, but Android could not schedule its reminder." }
        refresh()
    }
    fun delete(entry: Entry) { Reminders.cancel(getApplication(), entry.id); store.delete(entry.id); refresh() }
    fun toggle(entry: Entry) {
        store.toggle(entry)
        if (!entry.done) Reminders.cancel(getApplication(), entry.id)
        else if (entry.due > System.currentTimeMillis()) Reminders.schedule(getApplication(), entry.id, entry.due)
        refresh()
    }
    fun edit(entry: Entry, text: String) { if (text.isNotBlank()) { delete(entry); save(entry.kind, text, if (entry.done) 0 else entry.due) } }
    fun reply(text: String, persist: Boolean = true) {
        messages = (messages + ChatLine("Friday", text)).takeLast(100)
        if (persist) store.chat("Friday", text)
        speechText = ""; speechText = text
    }
    fun briefing(): String {
        val tasks = entries.filter { it.kind == "task" && !it.done }
        return if (tasks.isEmpty()) "Your task list is clear. What would you like to work on today?" else "You have ${tasks.size} open tasks:\n" + tasks.take(8).joinToString("\n") { "• ${it.text}" }
    }
    fun ask(question: String, ephemeral: Boolean = false) {
        if (busy || question.isBlank()) return
        val history = store.chats()
        messages = (messages + ChatLine("You", if (ephemeral) "Explain selected content (not saved)" else question)).takeLast(100)
        if (!ephemeral) store.chat("You", question)
        busy = true; streaming = ""
        viewModelScope.launch {
            try {
                val answer = brain.answer(question, if (ephemeral) emptyList() else history, if (useMemory && !ephemeral) entries.filter { it.kind == "memory" } else emptyList()) { partial -> withContext(Dispatchers.Main) { streaming = partial } }
                reply(answer, !ephemeral)
            } catch (error: Exception) { notice = error.message?.take(240) ?: "Model could not answer. Try a smaller compatible model." }
            catch (error: LinkageError) { notice = "The local AI runtime is not compatible with this device. Other tools remain available." }
            finally { busy = false; streaming = "" }
        }
    }
    fun importModel(uri: Uri) {
        if (busy) return
        busy = true; notice = "Importing model… keep Friday open. Large files take time."
        viewModelScope.launch {
            try { modelName = brain.import(uri); prefs.edit().putString("model", modelName).apply(); notice = "Model imported. Send a short message in Chat to test it on your phone." }
            catch (error: Exception) { notice = error.message ?: "Model import failed." }
            finally { busy = false }
        }
    }
    fun removeModel() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { brain.unload(true); modelName = "No model installed"; prefs.edit().remove("model").apply() }
            catch (error: Exception) { notice = error.message.orEmpty() }
            finally { busy = false }
        }
    }
    fun clearChat() { if (!busy) { store.clearChats(); messages = emptyList(); speechText = "" } }
    fun erasePersonalData() {
        if (busy) return
        entries.forEach { Reminders.cancel(getApplication(), it.id) }
        store.clear(); messages = emptyList(); refresh(); resume(""); draftDocument = ""; speechText = ""; ScreenService.cancelRead()
        notice = "Chats, notes, tasks, memories and resume erased. Remove the model separately in Settings."
    }
    override fun onCleared() {
        brain.stop()
        // The viewModel job cancels first; serialize native release after inference is drained.
        val executor = Executors.newSingleThreadExecutor()
        executor.execute { kotlinx.coroutines.runBlocking { brain.unload() }; store.close(); executor.shutdown() }
    }
}
