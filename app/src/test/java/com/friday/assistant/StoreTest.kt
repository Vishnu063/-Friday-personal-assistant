package com.friday.assistant

import android.content.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class StoreTest {
    private lateinit var context: Context
    private var store: Store? = null
    @Before fun setup() { context = RuntimeEnvironment.getApplication(); context.deleteDatabase("friday.db") }
    @After fun cleanup() { store?.close(); context.deleteDatabase("friday.db") }
    @Test fun migratesExistingV1ItemsWithoutLosingState() {
        context.openOrCreateDatabase("friday.db", Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY, kind TEXT NOT NULL, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, due INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE chats (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, text TEXT NOT NULL)")
            db.execSQL("INSERT INTO entries VALUES ('old','task','Existing task',1,0,1)")
            db.version = 1
        }
        store = Store(context)
        assertTrue(store!!.entries().single().done)
        assertEquals("Existing task", store!!.entries().single().text)
    }
    @Test fun deletedMemoryIsExcludedFromContextAndBackupUntilRestored() {
        store = Store(context)
        val id = store!!.add("memory", "I prefer short answers")
        store!!.delete(id)
        assertTrue(store!!.entries("memory").isEmpty())
        assertFalse(store!!.export().contains("I prefer short answers"))
        assertEquals(id, store!!.entries(trash = true).single().id)
        store!!.restore(id)
        assertEquals("I prefer short answers", store!!.entries("memory").single().text)
    }
    @Test fun repeatedBackupMergeDoesNotDuplicateItemsAndKeepsCompletion() {
        store = Store(context)
        val backup = BackupData(listOf(BackupItem("task", "Finish interview", true, 0)), listOf(ChatLine("You", "Hello")), "AWS")
        assertEquals(1, store!!.import(backup))
        assertEquals(0, store!!.import(backup))
        assertEquals(1, store!!.entries().size)
        assertTrue(store!!.entries().single().done)
        assertEquals(1, store!!.chats().size)
    }
}
