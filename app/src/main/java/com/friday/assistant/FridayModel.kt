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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.io.File

class FridayModel(application: Application) : AndroidViewModel(application) {
    val store = Store(application)
    val brain = LocalBrain(application)
    private val prefs = application.getSharedPreferences("friday", 0)
    var messages by mutableStateOf(store.chats(limit = 40)); private set
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
    var privacyMode by mutableStateOf(false); private set
    var trash by mutableStateOf(store.entries(trash = true, limit = 50)); private set
    var lastDeleted by mutableStateOf<Entry?>(null); private set
    var offlineVoice by mutableStateOf(prefs.getBoolean("offlineVoice", false)); private set
    var backgroundVoice by mutableStateOf(prefs.getBoolean("backgroundVoice", false)); private set
    var speechRate by mutableFloatStateOf(prefs.getFloat("speechRate", 1f)); private set
    var modelProgress by mutableStateOf(""); private set
    var modelDiagnostic by mutableStateOf("Not tested on this phone yet"); private set
    private var modelJob: Job? = null
    var appLock by mutableStateOf(prefs.getBoolean("appLock", false)); private set
    var compactMode by mutableStateOf(prefs.getBoolean("compactMode", true)); private set
    var aiLoaded by mutableStateOf(false); private set
    var idleSeconds by mutableIntStateOf(prefs.getInt("idleSeconds", 45).let { if (it in listOf(5, 45, 300)) it else 45 }); private set
    fun idleSeconds(value: Int) { if (value !in listOf(5, 45, 300)) return; idleSeconds = value; prefs.edit().putInt("idleSeconds", value).apply(); if (!busy) scheduleRelease() }
    private var idleJob: Job? = null
    var revision by mutableIntStateOf(0); private set
    private var foreground = true
    fun foreground(value: Boolean) { foreground = value; if (!value) { speechText = ""; releaseAI() } }
    fun loadSavedChat() { if (!privacyMode) messages = store.chats() }
    fun appLock(value: Boolean) { appLock = value; prefs.edit().putBoolean("appLock", value).apply() }
    fun compact(value: Boolean) { if (!busy) { compactMode = value; brain.compact = value; prefs.edit().putBoolean("compactMode", value).apply(); releaseAI() } }
    private fun scheduleRelease() {
        idleJob?.cancel()
        if (!foreground) { releaseAI(); return }
        idleJob = viewModelScope.launch { delay(idleSeconds * 1000L); if (!busy) { brain.unload(); aiLoaded = false } }
    }
    fun releaseAI() {
        idleJob?.cancel(); brain.stop()
        viewModelScope.launch { brain.unload(); aiLoaded = false }
    }
    fun basicList(kind: String): String {
        val records = store.entries(kind, limit = 12).filter { kind != "task" || !it.done }
        return if (records.isEmpty()) "No saved $kind items yet." else records.joinToString("\n") { "• ${it.text.take(200)}" }
    }
    fun privacy(value: Boolean) { if (!busy) { privacyMode = value; messages = if (value) emptyList() else store.chats(limit = 40); speechText = "" } }
    fun offlineVoice(value: Boolean) { offlineVoice = value; prefs.edit().putBoolean("offlineVoice", value).apply() }
    fun backgroundVoice(value: Boolean) { backgroundVoice = value; prefs.edit().putBoolean("backgroundVoice", value).apply() }
    fun rate(value: Float) { speechRate = value.coerceIn(.6f, 1.4f); prefs.edit().putFloat("speechRate", speechRate).apply() }
    fun restoreItem(entry: Entry) { store.restore(entry.id); lastDeleted = null; refresh(); reschedule() }
    fun emptyTrash() { store.emptyTrash(); lastDeleted = null; refresh() }
    private fun reschedule() { store.pendingTasks().forEach { runCatching { Reminders.schedule(getApplication(), it.id, it.due) } } }
    fun restoreBackup(data: BackupData) {
        if (busy) return
        val count = store.import(data)
        if (data.resume.isNotBlank()) resume(data.resume)
        refresh(); if (!privacyMode) messages = store.chats(limit = 40); reschedule()
        notice = "Restored $count new items. Duplicates skipped. Future task reminders scheduled; notification permission is still required."
    }
    fun saveStructured(kind: String, fields: Map<String, String>) { save(kind, org.json.JSONObject(fields).toString()) }
    fun updateStructured(entry: Entry, key: String, value: String) { val json = runCatching { org.json.JSONObject(entry.text) }.getOrNull() ?: return; json.put(key, value); edit(entry, json.toString()) }
    fun cancelModelOperation() { modelJob?.cancel(); brain.stop() }
    fun downloadModel(url: String, checksum: String) {
        if (busy) return
        busy = true; modelProgress = "Connecting…"
        modelJob = viewModelScope.launch {
            val file = File(getApplication<Application>().filesDir, "model.download")
            try {
                ModelDownload.fetch(url, checksum, file) { done, total -> withContext(Dispatchers.Main) { modelProgress = if (total > 0) "${done * 100 / total}% • ${done / 1048576} MB" else "${done / 1048576} MB downloaded" } }
                brain.installDownloaded(file)
                modelName = "Downloaded .litertlm model"; prefs.edit().putString("model", modelName).apply()
                modelDiagnostic = "Imported; run compatibility test"; notice = "Downloaded and installed. ${if (checksum.isBlank()) "No provider checksum supplied; integrity was not independently verified." else "SHA-256 verified."} Run the model test."
            } catch (_: CancellationException) { notice = "Download cancelled. Previous model preserved." }
            catch (error: Exception) { notice = error.message?.take(220).orEmpty() }
            finally { file.delete(); busy = false; modelProgress = "" }
        }
    }
    fun testModel() {
        if (busy) return
        idleJob?.cancel(); brain.compact = compactMode
        busy = true; modelDiagnostic = "Loading and generating…"
        viewModelScope.launch {
            val start = android.os.SystemClock.elapsedRealtime()
            var first = 0L
            try {
                brain.answer("Reply with one word: Ready", emptyList(), emptyList()) { if (first == 0L) first = android.os.SystemClock.elapsedRealtime() - start }
                modelDiagnostic = "Compatibility test passed • first text ${first}ms • total ${android.os.SystemClock.elapsedRealtime() - start}ms. Quality not assessed."
            } catch (error: Exception) { modelDiagnostic = "Test failed: ${error.message?.take(150)}" }
            catch (_: LinkageError) { modelDiagnostic = "Native runtime is unavailable on this device." }
            finally { busy = false; aiLoaded = brain.loaded; scheduleRelease() }
        }
    }
    init { store.pendingTasks().forEach { runCatching { Reminders.schedule(application, it.id, it.due) } } }
    fun onboard() { onboarded = true; prefs.edit().putBoolean("onboarded", true).apply() }
    fun language(value: String) { language = value; prefs.edit().putString("language", value).apply() }
    fun speech(value: Boolean) { speakReplies = value; prefs.edit().putBoolean("speak", value).apply() }
    fun memory(value: Boolean) { useMemory = value; prefs.edit().putBoolean("memory", value).apply() }
    fun theme(value: Boolean) { accentMint = value; prefs.edit().putBoolean("mint", value).apply() }
    fun resume(value: String) { resume = value.take(12000); prefs.edit().putString("resume", resume).apply() }
    fun refresh() { trash = store.entries(trash = true, limit = 50); revision++ }
    fun save(kind: String, text: String, due: Long = 0) {
        if (text.isBlank()) { notice = "Enter something to save."; return }
        val id = store.add(kind, text.trim(), due)
        if (due > 0) runCatching { Reminders.schedule(getApplication(), id, due) }.onFailure { notice = "Task saved, but Android could not schedule its reminder." }
        refresh()
    }
    fun delete(entry: Entry) { Reminders.cancel(getApplication(), entry.id); store.delete(entry.id); lastDeleted = entry; refresh() }
    fun reschedule(entry: Entry, due: Long) { Reminders.cancel(getApplication(), entry.id); store.reschedule(entry.id, due); refresh(); reschedule() }
    fun toggle(entry: Entry) {
        store.toggle(entry)
        if (!entry.done) Reminders.cancel(getApplication(), entry.id)
        else if (entry.due > System.currentTimeMillis()) Reminders.schedule(getApplication(), entry.id, entry.due)
        refresh()
    }
    fun edit(entry: Entry, text: String) { if (text.isNotBlank()) { store.edit(entry, text.trim()); refresh() } }
    fun reply(text: String, persist: Boolean = true, ai: Boolean = false) {
        val role = if (ai) "Friday • AI suggestion" else "Friday"
        messages = (messages + ChatLine(role, text)).takeLast(40)
        if (persist && !privacyMode) store.chat(role, text)
        speechText = ""; if (foreground) speechText = text
    }
    fun briefing(): String {
        val tasks = store.openTasks(8)
        val count = store.countOpenTasks()
        return if (tasks.isEmpty()) "Your task list is clear. What would you like to work on today?" else "You have $count open tasks:\n" + tasks.take(8).joinToString("\n") { "• ${it.text}" }
    }
    fun ask(question: String, ephemeral: Boolean = false) {
        if (busy || question.isBlank()) return
        val privateTurn = ephemeral || privacyMode
        val history = if (privateTurn) emptyList() else store.chats(limit = 4)
        messages = (messages + ChatLine("You", if (ephemeral) "Explain selected content (not saved)" else question)).takeLast(40)
        if (!privateTurn) store.chat("You", question)
        idleJob?.cancel(); brain.compact = compactMode
        busy = true; streaming = ""
        viewModelScope.launch {
            try {
                val answer = brain.answer(question, if (privateTurn) emptyList() else history, if (useMemory && !privateTurn) MemoryPolicy.select(question, store.memoryCandidates()) else emptyList()) { partial -> withContext(Dispatchers.Main) { streaming = partial } }
                reply(answer, !privateTurn, ai = true)
            } catch (error: Exception) { notice = error.message?.take(240) ?: "Model could not answer. Try a smaller compatible model." }
            catch (error: LinkageError) { notice = "The local AI runtime is not compatible with this device. Other tools remain available." }
            finally { busy = false; streaming = ""; aiLoaded = brain.loaded; scheduleRelease() }
        }
    }
    fun importModel(uri: Uri) {
        if (busy) return
        busy = true; modelProgress = "Importing…"; notice = "Importing model… keep Friday open. Large files take time."
        modelJob = viewModelScope.launch {
            try { modelName = brain.import(uri) { bytes -> withContext(Dispatchers.Main) { modelProgress = "${bytes / 1048576} MB imported" } }; modelDiagnostic = "Imported; run compatibility test"; prefs.edit().putString("model", modelName).apply(); notice = "Model imported. Send a short message in Chat to test it on your phone." }
            catch (_: CancellationException) { notice = "Import cancelled." }
            catch (error: Exception) { notice = error.message ?: "Model import failed." }
            finally { busy = false; modelProgress = "" }
        }
    }
    fun removeModel() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { brain.unload(true); modelName = "No model installed"; modelDiagnostic = "No model installed"; prefs.edit().remove("model").apply() }
            catch (error: Exception) { notice = error.message.orEmpty() }
            finally { busy = false }
        }
    }
    fun clearChat() { if (!busy) { store.clearChats(); messages = emptyList(); speechText = "" } }
    fun erasePersonalData() {
        if (busy) return
        store.pendingTasks().forEach { Reminders.cancel(getApplication(), it.id) }
        store.clear(); lastDeleted = null; messages = emptyList(); refresh(); resume(""); draftDocument = ""; speechText = ""; ScreenService.cancelRead()
        notice = "Chats, notes, tasks, memories, trash, career/project/practice data and resume erased. Remove the model separately in Settings."
    }
    override fun onCleared() {
        idleJob?.cancel(); brain.stop()
        // The viewModel job cancels first; serialize native release after inference is drained.
        val executor = Executors.newSingleThreadExecutor()
        executor.execute { kotlinx.coroutines.runBlocking { brain.unload() }; store.close(); executor.shutdown() }
    }
}
