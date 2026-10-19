package com.friday.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun DictationOptions(settings: DictationSettings, privateMode: Boolean, reuse: (String) -> Unit) {
    Text("Dictation", style = MaterialTheme.typography.titleMedium)
    listOf(Triple("Review before sending", "review", settings.review), Triple("Remove um / uh / erm", "fillers", settings.fillers),
        Triple("Hold mic to speak", "hold", settings.hold), Triple("Recording sounds", "sounds", settings.sounds),
        Triple("Ask other media to pause while listening", "quiet", settings.quiet)).forEach { (label, key, enabled) ->
        Row { Text(label, Modifier.weight(1f)); Switch(enabled, { settings.flag(key, it) }) }
    }
    Text("Tap mode: tap again to finish. Hold mode: release to finish. Allow microphone access first. Long dictation lets you add several phrases before sending. Live words depend on your speech provider.", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(settings.words, settings::words, Modifier.fillMaxWidth(), label = { Text("Personal vocabulary") }, minLines = 3, maxLines = 6)
    Text("One word per line to suggest spelling (Android 13+, provider support required). For explicit replacement use: cube er net ease => Kubernetes. Review catches mistakes. Cleanup removes fillers and extra spaces; it is not a full grammar checker or a second AI call.", style = MaterialTheme.typography.bodySmall)
    Text("Keep approved transcripts (max 20)")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 1, 7).forEach { days -> FilterChip(settings.retention == days, { settings.retention(days) }, label = { Text(if (days == 0) "Off" else "$days day(s)") }) } }
    Text("Raw audio is never saved by Friday. Private chat skips transcript history. Retention is pruned when Friday starts, opens history or saves a transcript; Android/provider audio policies are separate.", style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = settings::refresh) { Text("Refresh history") }
    if (!privateMode) settings.history.asReversed().forEach { item ->
        Text(DateFormat.getDateTimeInstance().format(Date(item.time)), style = MaterialTheme.typography.labelSmall)
        Text(item.text.take(300))
        Row { TextButton(onClick = { reuse(item.text) }) { Text("Review again") }; TextButton(onClick = { settings.delete(item.time) }) { Text("Delete") } }
    }
    TextButton(onClick = settings::clear) { Text("Delete all transcripts") }
    Text("Microphone routing follows Android and your speech provider. Connect your headset before recording; Friday cannot guarantee a chosen input with system recognition. Audio focus requests depend on other apps and Android. No global volume changes.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun TranscriptReview(text: String, original: String, change: (String) -> Unit, send: () -> Unit, discard: () -> Unit, more: () -> Unit, enabled: Boolean) {
    AlertDialog(onDismissRequest = discard, title = { Text("Review your words") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (original != text) Text("Last phrase heard: ${original.take(500)}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(text, { change(it.take(6000)) }, Modifier.fillMaxWidth(), minLines = 3, maxLines = 10, label = { Text("Edit before sending") })
            Text("${text.length}/6000 characters. Nothing is submitted until Send.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(enabled = enabled && text.isNotBlank(), onClick = send) { Text("Send") } }, dismissButton = {
        Row { TextButton(onClick = more, enabled = text.length < 6000) { Text("Add phrase") }; TextButton(onClick = discard) { Text("Discard") } }
    })
}
