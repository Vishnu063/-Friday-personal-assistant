package com.friday.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/** Owner-recorded acceptance checks, deliberately separate from automated CI results. */
class DeviceChecks(private val context: Context) {
    private val prefs = context.getSharedPreferences("device_checks", 0)
    @Suppress("DEPRECATION")
    val version = context.packageManager.getPackageInfo(context.packageName, 0).let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong() }
    val checks = linkedMapOf(
        "voice" to "Hear English and Hindi; stop and interrupt a long reply",
        "microphone" to "Recognize speech and recover after denying permission",
        "headset" to "Check the actual Bluetooth/wired microphone and speaker",
        "reminder" to "Receive a scheduled reminder; test restart/reboot",
        "model" to "Import a small compatible model; test speed, accuracy and stop",
        "layout" to "Check large fonts, small screen, keyboard and background return"
    )
    fun state(id: String): String {
        require(id in checks)
        val raw = prefs.getString(id, "not_tested").orEmpty()
        return if (raw == "not_tested" || prefs.getLong("$id.version", -1) == version) raw else "retest_after_update"
    }
    fun mark(id: String, state: String) {
        require(id in checks && state in setOf("passed", "failed", "not_tested"))
        prefs.edit().putString(id, state).putLong("$id.version", version).putLong("$id.time", System.currentTimeMillis()).apply()
    }
    @Suppress("DEPRECATION")
    fun signing(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val fingerprint = signatures?.firstOrNull()?.toByteArray()?.let { MessageDigest.getInstance("SHA-256").digest(it).joinToString(":") { b -> "%02X".format(b) } } ?: "Unavailable"
        val preview = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return "${if (preview) "Debug preview: stable update signing is not established" else "Non-debug build: compare its certificate with your trusted release"}\nSigning certificate SHA-256: $fingerprint"
    }
    fun report(): String = "Friday device checks • version code $version\n${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}\nOwner-recorded results, not automated device certification:\n" + checks.entries.joinToString("\n") { "${it.value}: ${state(it.key)}" }
    fun clear() { prefs.edit().clear().apply() }
}

@Composable fun DeviceChecksPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val checks = remember { DeviceChecks(context) }
    var revision by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf(false) }
    val report = remember(revision) { checks.report() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Device acceptance checklist", style = MaterialTheme.typography.titleMedium)
        Text("Run each check on this phone, then record what you observed. App updates require retesting. CI cannot mark these passed.")
        checks.checks.forEach { (id, label) ->
            Text(label)
            Text("Result: ${checks.state(id)}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("passed" to "Passed", "failed" to "Failed", "not_tested" to "Not tested").forEach { (state, label) ->
                    FilterChip(selected = checks.state(id) == state, onClick = { checks.mark(id, state); revision++ }, label = { Text(label) })
                }
            }
        }
        Text(runCatching { checks.signing() }.getOrDefault("Signing information unavailable. See docs/RELEASE_SIGNING.md."), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { review = true }) { Text("Review device-test report") }
    }
    if (review) AlertDialog(onDismissRequest = { review = false }, title = { Text("Review before sharing") }, text = { Text(report) }, confirmButton = {
        TextButton(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), "Share device-test report")); review = false }) { Text("Share report") }
    }, dismissButton = { TextButton(onClick = { review = false }) { Text("Close") } })
}
