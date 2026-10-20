package com.friday.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

object LinkedInWire {
    fun origin(value: String): String {
        val uri = URI(value.trim().trimEnd('/'))
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.rawPath.isNullOrEmpty() && uri.port in -1..65535 && uri.port != 0) { "Use your companion's HTTPS origin without a path or credentials." }
        return uri.toASCIIString()
    }
    fun account(data: JSONObject): Pair<String, String> {
        require(data.optString("mode") == "read_only" && data.optString("name").isNotBlank() && Regex("[A-Za-z0-9_-]+").matches(data.optString("account_id"))) { "Unsupported companion account response." }
        return data.getString("name") to data.getString("account_id")
    }
    fun check(data: JSONObject, profile: String, account: String): JSONObject {
        require(data.getString("account_id") == account) { "The companion account changed. Disconnect and verify it again." }
        require(ReferralCsv.profile(data.getString("profile")) == ReferralCsv.profile(profile) && data.optString("name").isNotBlank() && data.optString("state") in setOf("prior_contact", "not_found_unverified")) { "Unverified recipient/history response. Check LinkedIn manually." }
        return data
    }
    fun request(base: String, token: String, path: String, body: JSONObject? = null): JSONObject {
        require(path in setOf("/v1/account", "/v1/check"))
        val connection = URL(origin(base) + path).openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000; connection.readTimeout = 35000
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            require(connection.responseCode == 200) { "Companion HTTP ${connection.responseCode}. Check its local session and token; no message was sent." }
            require(connection.contentType.orEmpty().startsWith("application/json")) { "Companion did not return JSON. Check the endpoint." }
            val bytes = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(4096)
                while (true) { val n = input.read(buffer); if (n < 0) break; require(bytes.size() + n <= 65536) { "Companion response too large." }; bytes.write(buffer, 0, n) }
            }
            return JSONObject(bytes.toString("UTF-8"))
        } finally { connection.disconnect() }
    }
}

/** Only an encrypted companion token is stored; LinkedIn passwords/cookies never enter Friday. */
class LinkedInConnection(context: Context) {
    private val prefs = context.getSharedPreferences("linkedin_companion", 0)
    val url get() = prefs.getString("origin", "").orEmpty()
    val name get() = prefs.getString("name", "").orEmpty()
    val connected get() = url.isNotBlank() && prefs.contains("token")
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("friday_linkedin_companion", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("friday_linkedin_companion", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun token(): String {
        val packed = Base64.decode(prefs.getString("token", ""), Base64.NO_WRAP)
        require(packed.size > 28) { "Connect the companion again." }
        return Cipher.getInstance("AES/GCM/NoPadding").run { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, packed.copyOfRange(0, 12))); String(doFinal(packed.copyOfRange(12, packed.size)), Charsets.UTF_8) }
    }
    fun connect(base: String, token: String): String {
        val clean = LinkedInWire.origin(base)
        require(token.length in 32..256 && token.all { it.code in 33..126 }) { "Enter the companion token from your computer, not your LinkedIn password." }
        val (name, id) = LinkedInWire.account(LinkedInWire.request(clean, token, "/v1/account"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = Base64.encodeToString(cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putString("origin", clean).putString("token", encrypted).putString("name", name).putString("account", id).commit()) { "Could not save companion settings." }
        return name
    }
    fun check(profile: String): JSONObject {
        val normalized = ReferralCsv.profile(profile)
        val data = LinkedInWire.request(url, token(), "/v1/check", JSONObject().put("profile", normalized))
        return LinkedInWire.check(data, normalized, prefs.getString("account", "").orEmpty())
    }
    fun disconnect() { prefs.edit().clear().commit() }
}
