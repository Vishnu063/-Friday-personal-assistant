package com.friday.assistant

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Entry(val id: String, val kind: String, val text: String, val done: Boolean, val due: Long)
data class ChatLine(val role: String, val text: String)

class Store(context: Context) : SQLiteOpenHelper(context, "friday.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY, kind TEXT NOT NULL, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, due INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE chats (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, text TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) { if (old < 2) db.execSQL("ALTER TABLE entries ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0") }
    fun entries(kind: String? = null, trash: Boolean = false, limit: Int? = null): List<Entry> = readableDatabase.query("entries", null, "deleted${if (trash) ">0" else "=0"}" + if (kind == null) "" else " AND kind=?", kind?.let { arrayOf(it) }, null, null, "created DESC", limit?.coerceIn(1, 10000)?.toString()).use { c ->
        buildList { while (c.moveToNext()) add(Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0, c.getLong(4))) }
    }
    fun countActive(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM entries WHERE deleted=0", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    fun search(kind: String, query: String, limit: Int = 200): List<Entry> {
        val pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        return readableDatabase.rawQuery("SELECT id,kind,text,done,due FROM entries WHERE deleted=0 AND kind=? AND text LIKE ? ESCAPE '\\' ORDER BY created DESC LIMIT ?", arrayOf(kind, pattern, limit.coerceIn(1, 1000).toString())).use { c ->
            buildList { while (c.moveToNext()) add(Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0, c.getLong(4))) }
        }
    }
    fun memoryCandidates(): List<Entry> = readableDatabase.rawQuery("SELECT id,kind,substr(text,1,600),done,due FROM entries WHERE deleted=0 AND kind='memory' ORDER BY created DESC LIMIT 64", null).use { c ->
        buildList { while (c.moveToNext()) add(Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0, c.getLong(4))) }
    }
    fun add(kind: String, text: String, due: Long = 0): String {
        val id = UUID.randomUUID().toString()
        insert(Entry(id, kind, text.take(16000), false, due)); return id
    }
    private fun insert(entry: Entry) = writableDatabase.insertOrThrow("entries", null, ContentValues().apply {
        put("id", entry.id); put("kind", entry.kind); put("text", entry.text); put("done", if (entry.done) 1 else 0); put("due", entry.due); put("created", System.currentTimeMillis()); put("deleted", 0)
    })
    fun toggle(entry: Entry) = writableDatabase.update("entries", ContentValues().apply { put("done", if (entry.done) 0 else 1) }, "id=?", arrayOf(entry.id))
    fun edit(entry: Entry, text: String) = writableDatabase.update("entries", ContentValues().apply { put("text", text.take(16000)) }, "id=?", arrayOf(entry.id))
    fun reschedule(id: String, due: Long) = writableDatabase.update("entries", ContentValues().apply { put("due", due); put("done", 0) }, "id=? AND kind='task' AND deleted=0", arrayOf(id))
    fun openTasks(limit: Int = 12): List<Entry> = readableDatabase.rawQuery("SELECT id,kind,substr(text,1,600),done,due FROM entries WHERE deleted=0 AND kind='task' AND done=0 ORDER BY CASE WHEN due>0 THEN due ELSE 32503680000000 END, created DESC LIMIT ?", arrayOf(limit.coerceIn(1, 1000).toString())).use { c ->
        buildList { while (c.moveToNext()) add(Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0, c.getLong(4))) }
    }
    fun countOpenTasks(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM entries WHERE deleted=0 AND kind='task' AND done=0", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    fun pendingTasks(): List<Entry> = readableDatabase.rawQuery("SELECT id,kind,substr(text,1,200),done,due FROM entries WHERE deleted=0 AND kind='task' AND done=0 AND due>?", arrayOf(System.currentTimeMillis().toString())).use { c ->
        buildList { while (c.moveToNext()) add(Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0, c.getLong(4))) }
    }
    fun find(id: String): Entry? = readableDatabase.rawQuery("SELECT id,kind,text,done,due FROM entries WHERE deleted=0 AND id=?", arrayOf(id)).use { c ->
        if (c.moveToFirst()) Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0, c.getLong(4)) else null
    }
    fun delete(id: String) = writableDatabase.update("entries", ContentValues().apply { put("deleted", System.currentTimeMillis()) }, "id=?", arrayOf(id))
    fun restore(id: String) = writableDatabase.update("entries", ContentValues().apply { put("deleted", 0) }, "id=?", arrayOf(id))
    fun emptyTrash() = writableDatabase.delete("entries", "deleted>0", null)
    fun chats(limit: Int = 100): List<ChatLine> = readableDatabase.rawQuery("SELECT role,text FROM (SELECT * FROM chats ORDER BY id DESC LIMIT ?) ORDER BY id", arrayOf(limit.coerceIn(1, 100).toString())).use { c -> buildList { while (c.moveToNext()) add(ChatLine(c.getString(0), c.getString(1))) } }
    fun chat(role: String, text: String) {
        writableDatabase.insertOrThrow("chats", null, ContentValues().apply { put("role", role); put("text", text.take(16000)) })
        writableDatabase.execSQL("DELETE FROM chats WHERE id NOT IN (SELECT id FROM chats ORDER BY id DESC LIMIT 100)")
    }
    fun import(data: BackupData): Int {
        var inserted = 0
        fun key(kind: String, text: String, due: Long, done: Boolean) = listOf(kind, text, due.toString(), done.toString())
        val existing = entries().map { key(it.kind, it.text, it.due, it.done) }.toMutableSet()
        writableDatabase.beginTransaction()
        try {
            data.items.forEach { if (existing.add(key(it.kind, it.text, it.due, it.done))) { insert(Entry(UUID.randomUUID().toString(), it.kind, it.text, it.done, it.due)); inserted++ } }
            val seen = chats().map { it.role to it.text }.toMutableSet()
            data.chats.forEach { if (seen.add(it.role to it.text)) chat(it.role, it.text) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
        return inserted
    }
    fun clearChats() { writableDatabase.delete("chats", null, null) }
    fun clear() { writableDatabase.delete("entries", null, null); clearChats() }
    fun export(): String = JSONObject().apply {
        put("format", "friday-v2")
        put("entries", JSONArray().apply { entries().forEach { put(JSONObject().apply { put("kind", it.kind); put("text", it.text); put("done", it.done); put("due", it.due) }) } })
        put("chats", JSONArray().apply { chats().forEach { put(JSONObject().apply { put("role", it.role); put("text", it.text) }) } })
    }.toString(2)
}
