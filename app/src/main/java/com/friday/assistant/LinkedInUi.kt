package com.friday.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun LinkedInPanel(vm: FridayModel) {
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(vm.linkedin.url) }
    var token by remember { mutableStateOf("") }
    var profile by rememberSaveable { mutableStateOf("") }
    var account by remember { mutableStateOf(vm.linkedin.name) }
    var connected by remember { mutableStateOf(vm.linkedin.connected) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Optional live LinkedIn check", style = MaterialTheme.typography.titleMedium)
            Text("Experimental read-only connection to a companion on your own computer. No CSV is needed for this check. Sending stays in LinkedIn; this does not prove you have never chatted.")
            if (!connected) {
                Text("First set up the local companion using docs/LINKEDIN_CONNECTION.md in the repo. Friday never asks for your LinkedIn password or cookies.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(url, { url = it.take(500) }, enabled = !working, label = { Text("Your companion HTTPS address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(token, { token = it.take(256) }, enabled = !working, label = { Text("Companion token") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(enabled = !working && url.isNotBlank() && token.isNotBlank(), onClick = {
                    working = true; status = "Connecting and checking account…"
                    scope.launch {
                        try { account = withContext(Dispatchers.IO) { vm.linkedin.connect(url, token) }; connected = true; status = "Connected to $account — read-only." }
                        catch (_: Exception) { status = "Connection failed. Check the HTTPS address, token and local LinkedIn session. No message was sent." }
                        finally { token = ""; working = false }
                    }
                }) { Text("Connect and verify account") }
            } else {
                Text("Account: $account • read-only")
                OutlinedTextField(profile, { profile = it.take(500) }, enabled = !working, label = { Text("Connection's LinkedIn profile URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(enabled = !working && profile.isNotBlank(), onClick = {
                    working = true; status = "Checking profile identity and available conversation history…"
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) { vm.linkedin.check(profile) }
                            status = if (result.getString("state") == "prior_contact") "${result.getString("name")}: previous conversation found. Exclude this person."
                                     else "${result.getString("name")}: no conversation found in available results. Archived/deleted history may be missing. Check LinkedIn before contacting them; this result does not approve sending."
                        } catch (_: Exception) { status = "History could not be verified. Check the companion session/account. Do not treat this as no previous contact." }
                        finally { working = false }
                    }
                }) { Text("Check available chat history") }
                TextButton(enabled = !working, onClick = { vm.linkedin.disconnect(); connected = false; account = ""; status = "Disconnected. The local companion's LinkedIn session is separate." }) { Text("Disconnect Friday") }
            }
            if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
