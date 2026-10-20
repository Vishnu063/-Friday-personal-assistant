package com.friday.assistant

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.net.URI
import java.text.Normalizer
import java.util.Locale

/** Local review queue only. No LinkedIn API, cookies, credentials or send automation. */
data class ReferralContact(val profile: String, val name: String, val company: String, val position: String, val state: String, val prior: Boolean)

object ReferralCsv {
    const val MAX_BYTES = 4 * 1024 * 1024
    fun rows(text: String): List<List<String>> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Choose a CSV smaller than 4 MB." }
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var ended = false; var i = 0
        fun field() { row.add(cell.toString()); cell.clear(); ended = false }
        fun line() { field(); if (row.any { it.isNotBlank() }) rows.add(row); row = mutableListOf(); require(rows.size <= 20001) { "CSV has more than 20,000 rows." } }
        while (i < text.length) {
            val c = text[i++]
            when {
                quoted && c == '"' -> if (i < text.length && text[i] == '"') { cell.append('"'); i++ } else { quoted = false; ended = true }
                quoted -> cell.append(c)
                c == '"' -> { require(cell.isEmpty() && !ended) { "Invalid CSV quotes." }; quoted = true }
                c == ',' -> field()
                c == '\r' || c == '\n' -> { if (c == '\r' && i < text.length && text[i] == '\n') i++; line() }
                else -> { require(!ended) { "Unexpected text after CSV quote." }; cell.append(c) }
            }
        }
        require(!quoted) { "CSV has an unclosed quote." }
        if (cell.isNotEmpty() || row.isNotEmpty() || ended) line()
        return rows
    }
    fun table(text: String, required: Set<String>): List<Map<String, String>> {
        val rows = rows(text.removePrefix("\uFEFF"))
        val headerAt = rows.indexOfFirst { row -> required.all { key -> row.any { it.trim().uppercase(Locale.ROOT) == key } } }
        require(headerAt in 0..10) { "Unsupported CSV headers. See Friday's referral guide." }
        val header = rows[headerAt].map { it.trim().uppercase(Locale.ROOT) }
        require(header.distinct().size == header.size) { "Duplicate CSV headers." }
        return rows.drop(headerAt + 1).map { row ->
            require(row.size == header.size) { "CSV row does not match its headers; nothing imported." }
            header.zip(row).toMap()
        }
    }
    fun profile(value: String): String {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: error("Invalid LinkedIn profile URL.")
        require(uri.scheme == "https" && uri.host?.lowercase(Locale.ROOT) in setOf("linkedin.com", "www.linkedin.com") && uri.rawUserInfo == null && uri.port == -1 && uri.rawQuery == null && uri.rawFragment == null) { "Use a plain https://www.linkedin.com/in/name profile URL." }
        require(Regex("/in/[A-Za-z0-9_%\\-]+/?").matches(uri.rawPath.orEmpty())) { "Use a LinkedIn /in/ profile URL." }
        return "https://www.linkedin.com" + uri.rawPath.trimEnd('/').lowercase(Locale.ROOT)
    }
    fun name(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ").trim()
    fun draft(name: String, role: String, facts: String, sender: String): String {
        require(listOf(name, role, facts, sender).all { it.isNotBlank() && it.length <= 1000 }) { "Add your name, target role and accurate experience first." }
        return "Hi ${name.trim()}, thanks for connecting!\n\nI’m exploring ${role.trim()} opportunities. ${facts.trim()}\n\nIf there’s a suitable opening at your company, would you be comfortable considering me for a referral? I’d be happy to share my resume and the job link.\n\nThank you for your time!\n${sender.trim()}"
    }
}

class Referrals(context: Context) : SQLiteOpenHelper(context, "referrals.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE contacts(profile TEXT PRIMARY KEY, name TEXT NOT NULL, company TEXT NOT NULL, position TEXT NOT NULL, state TEXT NOT NULL DEFAULT 'review', present INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE TABLE history(identity TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE imports(kind TEXT PRIMARY KEY, imported INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    private fun transaction(block: (SQLiteDatabase) -> Unit) {
        val db = writableDatabase; db.beginTransaction()
        try { block(db); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }
    fun imported(kind: String): Long = readableDatabase.rawQuery("SELECT imported FROM imports WHERE kind=?", arrayOf(kind)).use { if (it.moveToFirst()) it.getLong(0) else 0 }
    private fun imported(db: SQLiteDatabase, kind: String) { db.insertWithOnConflict("imports", null, ContentValues().apply { put("kind", kind); put("imported", System.currentTimeMillis()) }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun importConnections(text: String): Int {
        val records = ReferralCsv.table(text, setOf("FIRST NAME", "LAST NAME", "URL"))
        require(records.size in 1..2000) { "Import 1–2,000 connections at a time." }
        val people = records.map { row ->
            val name = listOf(row.getValue("FIRST NAME"), row.getValue("LAST NAME")).filter { it.isNotBlank() }.joinToString(" ").trim()
            require(name.isNotBlank() && name.length <= 250) { "A connection has no valid name." }
            listOf(ReferralCsv.profile(row.getValue("URL")), name, row["COMPANY"].orEmpty().take(500), row["POSITION"].orEmpty().take(500))
        }
        require(people.map { it[0] }.distinct().size == people.size) { "Duplicate profile URLs; review the CSV first." }
        transaction { db ->
            db.execSQL("UPDATE contacts SET present=0")
            people.forEach { p ->
                db.insertWithOnConflict("contacts", null, ContentValues().apply { put("profile", p[0]); put("name", p[1]); put("company", p[2]); put("position", p[3]) }, SQLiteDatabase.CONFLICT_IGNORE)
                db.update("contacts", ContentValues().apply { put("name", p[1]); put("company", p[2]); put("position", p[3]); put("present", 1) }, "profile=?", arrayOf(p[0]))
            }
            imported(db, "connections")
        }
        return people.size
    }
    fun importMessages(text: String): Int {
        val records = ReferralCsv.table(text, setOf("FROM", "TO"))
        val identities = mutableSetOf<String>()
        records.forEach { row ->
            // Count a message even when CONTENT is blank: attachments/reactions still mean contact.
            for (column in listOf("FROM", "TO")) {
                val people = row.getValue(column).trim()
                require(people.isNotBlank()) { "A message has unknown participants. Nothing imported; review it manually." }
                people.split(';', ',', '\n').forEach { value ->
                    val normalized = ReferralCsv.name(value)
                    require(normalized.isNotBlank() && normalized !in setOf("linkedin member", "unknown", "deleted member")) { "A message participant cannot be identified." }
                    identities.add("name:$normalized")
                }
            }
            listOf("SENDER PROFILE URL", "RECIPIENT PROFILE URLS").forEach { key ->
                Regex("https://(?:www\\.)?linkedin\\.com/in/[A-Za-z0-9_%\\-]+/?", RegexOption.IGNORE_CASE).findAll(row[key].orEmpty()).forEach { match ->
                    identities.add("url:" + ReferralCsv.profile(match.value))
                }
            }
        }
        transaction { db ->
            identities.forEach { key -> db.insertWithOnConflict("history", null, ContentValues().apply { put("identity", key) }, SQLiteDatabase.CONFLICT_IGNORE) }
            imported(db, "messages")
        }
        return records.size
    }
    private fun prior(profile: String, name: String) = readableDatabase.rawQuery("SELECT 1 FROM history WHERE identity IN (?,?) LIMIT 1", arrayOf("url:$profile", "name:${ReferralCsv.name(name)}")).use { it.moveToFirst() }
    fun contacts(): List<ReferralContact> = readableDatabase.rawQuery("SELECT profile,name,company,position,state FROM contacts WHERE present=1 ORDER BY name LIMIT 2000", null).use { c ->
        buildList { while (c.moveToNext()) add(ReferralContact(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), prior(c.getString(0), c.getString(1)))) }
    }
    fun prepare(profile: String) {
        transaction { db ->
            require(imported("messages") > 0) { "Import message history first." }
            val contact = contacts().firstOrNull { it.profile == profile } ?: error("Connection not found.")
            require(!contact.prior && contact.state in setOf("review", "prepared")) { "Previous contact or blocked/sent entry. Do not resend." }
            db.update("contacts", ContentValues().apply { put("state", "prepared") }, "profile=?", arrayOf(profile))
        }
    }
    fun mark(profile: String, state: String) {
        require(state in setOf("sent", "blocked"))
        transaction { db ->
            val contact = contacts().firstOrNull { it.profile == profile } ?: error("Connection not found.")
            require(state == "blocked" || contact.state == "prepared") { "Review and copy the draft before marking it sent." }
            db.update("contacts", ContentValues().apply { put("state", state) }, "profile=?", arrayOf(profile))
        }
    }
    fun clear() = transaction { db -> listOf("contacts", "history", "imports").forEach { db.delete(it, null, null) } }
}
