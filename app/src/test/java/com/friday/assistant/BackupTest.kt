package com.friday.assistant

import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private val good = """{"format":"friday-v1","entries":[{"kind":"task","text":"Study","done":false,"due":0}],"chats":[{"role":"You","text":"Hi"}],"resume":"AWS"}"""
    @Test fun acceptsOldBackupWithoutImportingPermissionsOrModels() {
        val data = Backup.parse(good.dropLast(1) + ",\"modelPath\":\"/bad\",\"permission\":true}")
        assertEquals("Study", data.items.single().text)
        assertEquals("AWS", data.resume)
    }
    @Test fun rejectsMalformedAndUnsupportedBackupsBeforeMutation() {
        listOf("{}", good.replace("friday-v1", "other"), good.replace("task", "execute"), good.replace("\"due\":0", "\"due\":-1"), good.replace("\"done\":false", "\"done\":\"no\"")).forEach { input ->
            assertTrue(runCatching { Backup.parse(input) }.isFailure)
        }
    }
    @Test fun rejectsOversizedTextAndUnknownChatRole() {
        assertTrue(runCatching { Backup.parse(good.replace("Study", "x".repeat(16001))) }.isFailure)
        assertTrue(runCatching { Backup.parse(good.replace("\"You\"", "\"Execute\"")) }.isFailure)
    }
}
