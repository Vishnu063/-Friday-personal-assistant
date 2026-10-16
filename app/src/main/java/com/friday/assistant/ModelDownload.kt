package com.friday.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

object ModelDownload {
    fun validateUrl(value: String): URI {
        val uri = URI(value.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null && uri.port in listOf(-1, 443)) { "Use a public HTTPS model URL without embedded credentials." }
        val host = uri.host.lowercase()
        require(host != "localhost" && !host.endsWith(".local") && !host.matches(Regex("[0-9.]+")) && !host.contains(':')) { "Use the model provider's public hostname." }
        return uri
    }
    suspend fun fetch(url: String, checksum: String, file: File, onProgress: suspend (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        require(checksum.isBlank() || checksum.matches(Regex("[a-fA-F0-9]{64}"))) { "SHA-256 must contain 64 hexadecimal characters." }
        var current = validateUrl(url).toString()
        try {
            for (redirect in 0..5) {
                currentCoroutineContext().ensureActive()
                val connection = URL(current).openConnection() as HttpsURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 15000; connection.readTimeout = 30000
                    connection.setRequestProperty("Accept", "application/octet-stream")
                    val code = connection.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        require(redirect < 5) { "Too many download redirects." }
                        current = validateUrl(URI(current).resolve(connection.getHeaderField("Location") ?: error("Missing redirect URL")).toString()).toString()
                        continue
                    }
                    require(code == 200) { "Download failed (HTTP $code). For gated models, sign in using your browser and use Import instead." }
                    val type = connection.contentType.orEmpty()
                    require(!type.contains("text/html")) { "This URL is a web page, not a model file." }
                    val total = connection.contentLengthLong
                    require(total < 0 || total <= 4L * 1024 * 1024 * 1024) { "Choose a model smaller than 4 GB." }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var downloaded = 0L
                    connection.inputStream.use { input -> file.outputStream().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer); if (count < 0) break
                            require(file.parentFile!!.usableSpace > count + 256L * 1024 * 1024) { "Not enough storage." }
                            downloaded += count
                            require(downloaded <= 4L * 1024 * 1024 * 1024) { "Download exceeded 4 GB." }
                            output.write(buffer, 0, count); digest.update(buffer, 0, count); onProgress(downloaded, total)
                        }
                    } }
                    require(downloaded > 1024 && (total < 0 || downloaded == total)) { "The download is incomplete." }
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    require(checksum.isBlank() || actual.equals(checksum, true)) { "Checksum mismatch. Download discarded; current model preserved." }
                    return@withContext file
                } finally { connection.disconnect() }
            }
            error("Could not download model.")
        } catch (error: Throwable) { file.delete(); throw error }
    }
}
