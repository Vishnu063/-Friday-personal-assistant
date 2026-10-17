package com.friday.assistant

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned, authenticated password encryption. Passwords/derived keys are never persisted. */
object EncryptedBackup {
    const val FORMAT = "friday-encrypted-v1"
    const val MAX_FILE_BYTES = 12 * 1024 * 1024
    private const val ITERATIONS = 210000
    private val aad = FORMAT.toByteArray(Charsets.UTF_8)
    fun isEncrypted(text: String): Boolean {
        require(text.toByteArray().size <= MAX_FILE_BYTES) { "Backup file is larger than 12 MB." }
        Backup.checkDepth(text)
        return JSONObject(text).optString("format") == FORMAT
    }
    private fun key(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    fun encrypt(plain: String, password: CharArray): String {
        var derived: ByteArray? = null
        try {
            require(password.size >= 10) { "Use a password with at least 10 characters." }
            Backup.parse(plain)
            val random = SecureRandom(); val salt = ByteArray(16).also(random::nextBytes); val iv = ByteArray(12).also(random::nextBytes)
            derived = key(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, iv)); cipher.updateAAD(aad)
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            return JSONObject().put("format", FORMAT).put("kdf", "PBKDF2WithHmacSHA256").put("iterations", ITERATIONS)
                .put("salt", Base64.getEncoder().encodeToString(salt)).put("iv", Base64.getEncoder().encodeToString(iv)).put("ciphertext", Base64.getEncoder().encodeToString(encrypted)).toString()
        } finally { derived?.fill(0); password.fill('\u0000') }
    }
    fun decrypt(envelope: String, password: CharArray): String {
        var derived: ByteArray? = null
        try {
            require(isEncrypted(envelope)) { "Not an encrypted Friday backup." }
            val root = JSONObject(envelope)
            require(root.optString("kdf") == "PBKDF2WithHmacSHA256" && root.optInt("iterations") == ITERATIONS) { "Unsupported encryption settings." }
            val salt = Base64.getDecoder().decode(root.getString("salt")); val iv = Base64.getDecoder().decode(root.getString("iv"))
            val encrypted = Base64.getDecoder().decode(root.getString("ciphertext"))
            require(salt.size == 16 && iv.size == 12 && encrypted.size in 16..(Backup.MAX_BYTES + 16)) { "Invalid encrypted backup." }
            derived = key(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, iv)); cipher.updateAAD(aad)
            val plain = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
            Backup.parse(plain)
            return plain
        } catch (error: Exception) { throw IllegalArgumentException("Incorrect password, unsupported format, or damaged backup.", error) }
        finally { derived?.fill(0); password.fill('\u0000') }
    }
}
