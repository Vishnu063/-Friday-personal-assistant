package com.friday.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable fun ReferralAssistant(vm: FridayModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var contacts by remember { mutableStateOf<List<ReferralContact>>(emptyList()) }
    var imported by remember { mutableLongStateOf(0) }
    var sender by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("DevOps Engineer") }
    var facts by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<ReferralContact?>(null) }
    var draft by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(revision) {
        contacts = withContext(Dispatchers.IO) { vm.referrals.contacts() }
        imported = withContext(Dispatchers.IO) { vm.referrals.imported("messages") }
    }
    fun importFile(uri: Uri?, messages: Boolean) {
        if (uri == null || loading) return
        loading = true
        scope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream(); val bytes = ByteArray(8192)
                        while (true) { val n = input.read(bytes); if (n < 0) break; require(output.size() + n <= ReferralCsv.MAX_BYTES) { "CSV is larger than 4 MB." }; output.write(bytes, 0, n) }
                        Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(output.toByteArray())).toString()
                    } ?: error("Could not open CSV.")
                    if (messages) vm.referrals.importMessages(text) else vm.referrals.importConnections(text)
                }
                selected = null; checked = false; status = "$count ${if (messages) "messages indexed" else "connections imported"} locally."; revision++
            } catch (e: Exception) { status = e.message?.take(180) ?: "Import failed; existing records preserved." }
            finally { loading = false }
        }
    }
    val connectionsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importFile(it, false) }
    val messagesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importFile(it, true) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("LinkedIn referral assistant", style = MaterialTheme.typography.headlineMedium)
            Text("Review drafts locally. LinkedIn is where you check the conversation and send. Friday does not read your account or send messages automatically.")
            OutlinedButton(enabled = !loading, onClick = { connectionsPicker.launch(arrayOf("text/*", "application/octet-stream", "application/vnd.ms-excel")) }) { Text("1. Import Connections.csv") }
            OutlinedButton(enabled = !loading, onClick = { messagesPicker.launch(arrayOf("text/*", "application/octet-stream", "application/vnd.ms-excel")) }) { Text("2. Import messages.csv") }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (imported > 0) Text("History imported: ${DateFormat.getDateTimeInstance().format(Date(imported))}")
            Text("Any recorded message in either direction excludes that person, including blank messages. Name matches also exclude people conservatively. Deleted chats, changed names, an old export or a different account can hide history: check LinkedIn before every draft. No result proves you have never spoken.", style = MaterialTheme.typography.bodySmall)
            Text("Exports and the contact ledger stay on this phone and are excluded from Friday backups. Erasing app data removes this protection; import history again before contacting anyone.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(sender, { sender = it.take(120) }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(role, { role = it.take(120) }, label = { Text("Target role") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(facts, { facts = it.take(1000) }, label = { Text("Your accurate experience and availability") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(query, { query = it.take(200) }, label = { Text("Find a connection or company") }, modifier = Modifier.fillMaxWidth())
            val excluded = contacts.count { it.prior || it.state in setOf("sent", "blocked") }
            Text("${contacts.size} imported • $excluded excluded • showing up to 30 matching contacts")
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.secondary)
        }
        items(contacts.filter { it.name.contains(query, true) || it.company.contains(query, true) }.take(30), key = { it.profile }) { contact ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium)
                    Text("${contact.company} • ${contact.position}")
                    Text(if (contact.prior) "Excluded: a previous message matches" else when (contact.state) { "prepared" -> "Draft previously copied — check LinkedIn before continuing"; "sent" -> "You marked this contact as sent"; "blocked" -> "Excluded by you"; else -> "Needs a live conversation check" })
                    OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(contact.profile))) }.onFailure { status = "No app could open LinkedIn." } }) { Text("Open profile to check chat") }
                    Button(enabled = imported > 0 && !contact.prior && contact.state in setOf("review", "prepared") && sender.isNotBlank() && facts.isNotBlank() && role.isNotBlank(), onClick = {
                        selected = contact; checked = false; draft = ReferralCsv.draft(contact.name, role, facts, sender)
                    }) { Text("Review referral draft") }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
    selected?.let { contact ->
        AlertDialog(onDismissRequest = { if (!loading) selected = null }, title = { Text("Review for ${contact.name}") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confirm this is the same person and account. If you see any earlier message, or cannot verify history, exclude them.")
                OutlinedTextField(draft, { draft = it.take(4000) }, label = { Text("Message to copy") }, minLines = 3, maxLines = 7)
                Row { Checkbox(checked, { checked = it }); Text("I checked this person's LinkedIn chat and found no previous message.", Modifier.weight(1f)) }
                if (contact.state == "prepared") Text("A draft was already copied. Do not send it twice.")
            }
        }, confirmButton = {
            TextButton(enabled = checked && draft.isNotBlank() && !loading, onClick = {
                loading = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { vm.referrals.prepare(contact.profile) }
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Referral draft", draft))
                        status = "Copied for ${contact.name}. Send in LinkedIn after checking the chat; Friday has not sent anything."; selected = null; revision++
                    } catch (e: Exception) { status = e.message.orEmpty(); selected = null }
                    finally { loading = false }
                }
            }) { Text("Copy reviewed draft") }
        }, dismissButton = {
            Column {
                TextButton(enabled = !loading, onClick = { vm.referrals.mark(contact.profile, "blocked"); selected = null; revision++ }) { Text("Exclude this person") }
                if (contact.state == "prepared") TextButton(enabled = !loading, onClick = { vm.referrals.mark(contact.profile, "sent"); selected = null; revision++ }) { Text("I already sent it — mark sent") }
                TextButton(enabled = !loading, onClick = { selected = null }) { Text("Cancel") }
            }
        })
    }
}
