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

class Store(context: Context) : SQLiteOpenHelper(context, "friday.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY, kind TEXT NOT NULL, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, due INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE chats (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, text TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = Unit
    fun entries(kind: String? = null): List<Entry> = readableDatabase.query("entries", null, if (kind == null) null else "kind=?", kind?.let { arrayOf(it) }, null, null, "created DESC", "500").use { cursor ->
        buildList { while (cursor.moveToNext()) add(Entry(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) != 0, cursor.getLong(4))) }
    }
    fun add(kind: String, text: String, due: Long = 0): String {
        val id = UUID.randomUUID().toString()
        writableDatabase.insertOrThrow("entries", null, ContentValues().apply {
            put("id", id); put("kind", kind); put("text", text.take(16000)); put("due", due); put("created", System.currentTimeMillis())
        })
        return id
    }
    fun toggle(entry: Entry) = writableDatabase.update("entries", ContentValues().apply { put("done", if (entry.done) 0 else 1) }, "id=?", arrayOf(entry.id))
    fun edit(entry: Entry, text: String) = writableDatabase.update("entries", ContentValues().apply { put("text", text.take(16000)) }, "id=?", arrayOf(entry.id))
    fun pendingTasks(): List<Entry> = readableDatabase.rawQuery("SELECT id,kind,text,done,due FROM entries WHERE kind='task' AND done=0 AND due>?", arrayOf(System.currentTimeMillis().toString())).use { cursor ->
        buildList { while (cursor.moveToNext()) add(Entry(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) != 0, cursor.getLong(4))) }
    }
    fun find(id: String): Entry? = readableDatabase.rawQuery("SELECT id,kind,text,done,due FROM entries WHERE id=?", arrayOf(id)).use { cursor ->
        if (cursor.moveToFirst()) Entry(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) != 0, cursor.getLong(4)) else null
    }
    fun delete(id: String) = writableDatabase.delete("entries", "id=?", arrayOf(id))
    fun chats(): List<ChatLine> = readableDatabase.rawQuery("SELECT role,text FROM (SELECT * FROM chats ORDER BY id DESC LIMIT 100) ORDER BY id", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(ChatLine(cursor.getString(0), cursor.getString(1))) }
    }
    fun chat(role: String, text: String) {
        writableDatabase.insertOrThrow("chats", null, ContentValues().apply { put("role", role); put("text", text.take(16000)) })
        writableDatabase.execSQL("DELETE FROM chats WHERE id NOT IN (SELECT id FROM chats ORDER BY id DESC LIMIT 100)")
    }
    fun clearChats() { writableDatabase.delete("chats", null, null) }
    fun clear() { writableDatabase.delete("entries", null, null); clearChats() }
    fun export(): String = JSONObject().apply {
        put("format", "friday-v1")
        put("entries", JSONArray().apply { entries().forEach { put(JSONObject().apply { put("kind", it.kind); put("text", it.text); put("done", it.done); put("due", it.due) }) } })
        put("chats", JSONArray().apply { chats().forEach { put(JSONObject().apply { put("role", it.role); put("text", it.text) }) } })
    }.toString(2)
}
