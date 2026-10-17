package com.friday.assistant
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class EncryptedBackupTest {
    private val plain = """{"format":"friday-v2","entries":[{"kind":"note","text":"नमस्ते Friday","done":false,"due":0}],"chats":[]}"""
    @Test fun roundTripIsRandomizedAndClearsPasswordBuffers() {
        val password = "my long password".toCharArray()
        val first = EncryptedBackup.encrypt(plain, password)
        assertTrue(password.all { it == '\u0000' })
        val second = EncryptedBackup.encrypt(plain, "my long password".toCharArray())
        assertNotEquals(first, second)
        val unlock = "my long password".toCharArray()
        assertEquals(plain, EncryptedBackup.decrypt(first, unlock))
        assertTrue(unlock.all { it == '\u0000' })
        assertFalse(first.contains("Friday"))
    }
    @Test fun wrongPasswordTamperingAndUnsupportedKdfFailClosed() {
        val encrypted = EncryptedBackup.encrypt(plain, "my long password".toCharArray())
        val wrong = "wrong password".toCharArray()
        assertTrue(runCatching { EncryptedBackup.decrypt(encrypted, wrong) }.isFailure)
        assertTrue(wrong.all { it == '\u0000' })
        val root = JSONObject(encrypted)
        root.put("iterations", 1)
        assertTrue(runCatching { EncryptedBackup.decrypt(root.toString(), "my long password".toCharArray()) }.isFailure)
        val damaged = JSONObject(encrypted)
        val bytes = java.util.Base64.getDecoder().decode(damaged.getString("ciphertext"))
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        damaged.put("ciphertext", java.util.Base64.getEncoder().encodeToString(bytes))
        assertTrue(runCatching { EncryptedBackup.decrypt(damaged.toString(), "my long password".toCharArray()) }.isFailure)
    }
}
