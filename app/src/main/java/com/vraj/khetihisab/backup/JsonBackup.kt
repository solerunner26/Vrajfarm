package com.vraj.khetihisab.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * The whole app in ONE .json file (owner request 2026-11-04: backup goes to WhatsApp, Google Drive and
 * e-mail; restore from any of them on a new phone).
 *
 * Layout (streamed, so photos never sit in memory all at once):
 * ```
 * { "app": "vraj_kheti_hisab", "format": 2, "dbVersion": 1, "appVersion": "0.2.0", "createdAt": 1730000000000,
 *   "counts": { "txn": 120, ... },
 *   "tables": { "txn": [ { "id": 1, "date": "2026-11-04", ... } ], ... },
 *   "files": [ { "path": "bill_photos/a.webp", "data": "<base64>" } ] }
 * ```
 * Header fields come first, so [inspect] can show "date + counts" before anything is replaced.
 * The PIN, biometric flag and theme are device settings and are NOT in the file.
 */
object JsonBackup {
    const val APP_ID = "vraj_kheti_hisab"
    const val FORMAT = 2
    const val MIME = "application/json"

    /** Photo folders under filesDir (CLAUDE.md: all photos go into the backup). */
    val PHOTO_DIRS = listOf("bill_photos", "people_photos", "borrower_photos", "reminder_photos")

    private val SKIP_TABLES = setOf("android_metadata", "room_master_table", "sqlite_sequence")

    data class Summary(
        val createdAt: Long,
        val dbVersion: Int,
        val appVersion: String,
        val counts: Map<String, Long>,
        val photoCount: Int,
    ) {
        val entries: Long get() = counts["txn"] ?: 0L
    }

    sealed interface RestoreResult {
        data class Ok(val summary: Summary) : RestoreResult
        /** Not one of our files. */
        data object NotABackup : RestoreResult
        /** Made by a newer app version; update the app first. */
        data class TooNew(val dbVersion: Int) : RestoreResult
        data class Broken(val message: String) : RestoreResult
    }

    fun tableNames(db: SupportSQLiteDatabase): List<String> {
        val out = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use { c ->
            while (c.moveToNext()) {
                val n = c.getString(0)
                if (n !in SKIP_TABLES && !n.startsWith("sqlite_")) out += n
            }
        }
        return out
    }

    /** Writes the backup to [out]. Caller closes nothing; this closes the writer. */
    fun export(
        db: SupportSQLiteDatabase,
        dbVersion: Int,
        appVersion: String,
        filesDir: File,
        out: OutputStream,
        now: Long = System.currentTimeMillis(),
    ): Summary {
        runCatching { db.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() } }
        val tables = tableNames(db)
        val counts = tables.associateWith { t -> db.query("SELECT COUNT(*) FROM `$t`").use { c -> if (c.moveToFirst()) c.getLong(0) else 0L } }
        val photos = PHOTO_DIRS.flatMap { dir ->
            File(filesDir, dir).takeIf { it.isDirectory }?.walkTopDown()?.filter { it.isFile }?.map { dir to it }?.toList().orEmpty()
        }
        JsonWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { w ->
            w.beginObject()
            w.name("app").value(APP_ID)
            w.name("format").value(FORMAT.toLong())
            w.name("dbVersion").value(dbVersion.toLong())
            w.name("appVersion").value(appVersion)
            w.name("createdAt").value(now)
            w.name("photoCount").value(photos.size.toLong())
            w.name("counts").beginObject()
            counts.forEach { (t, n) -> w.name(t).value(n) }
            w.endObject()

            w.name("tables").beginObject()
            for (t in tables) {
                w.name(t).beginArray()
                db.query("SELECT * FROM `$t`").use { c -> while (c.moveToNext()) writeRow(w, c) }
                w.endArray()
            }
            w.endObject()

            w.name("files").beginArray()
            for ((dir, f) in photos) {
                w.beginObject()
                w.name("path").value("$dir/${f.relativeTo(File(filesDir, dir)).invariantSeparatorsPath}")
                w.name("data").value(Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                w.endObject()
            }
            w.endArray()
            w.endObject()
        }
        return Summary(now, dbVersion, appVersion, counts, photos.size)
    }

    private fun writeRow(w: JsonWriter, c: Cursor) {
        w.beginObject()
        for (i in 0 until c.columnCount) {
            w.name(c.getColumnName(i))
            when (c.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> w.nullValue()
                Cursor.FIELD_TYPE_INTEGER -> w.value(c.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> w.value(c.getDouble(i))
                Cursor.FIELD_TYPE_BLOB -> {
                    w.beginObject(); w.name(BLOB).value(Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP)); w.endObject()
                }
                else -> w.value(c.getString(i))
            }
        }
        w.endObject()
    }

    /** Reads only the header (date, counts) for the "આ બેકઅપ પાછો લાવશો?" dialog. */
    fun inspect(input: InputStream): Summary? = runCatching {
        JsonReader(input.reader(Charsets.UTF_8)).use { r ->
            var app: String? = null
            var created = 0L
            var dbv = 0
            var appv = ""
            var photos = 0
            val counts = linkedMapOf<String, Long>()
            r.beginObject()
            loop@ while (r.hasNext()) {
                when (r.nextName()) {
                    "app" -> app = r.nextString()
                    "createdAt" -> created = r.nextLong()
                    "dbVersion" -> dbv = r.nextInt()
                    "appVersion" -> appv = r.nextString()
                    "photoCount" -> photos = r.nextInt()
                    "counts" -> {
                        r.beginObject()
                        while (r.hasNext()) counts[r.nextName()] = r.nextLong()
                        r.endObject()
                    }
                    "tables", "files" -> break@loop
                    else -> r.skipValue()
                }
            }
            if (app != APP_ID) null else Summary(created, dbv, appv, counts, photos)
        }
    }.getOrNull()

    /**
     * Replaces EVERYTHING with the backup: all app tables are emptied and refilled inside one
     * transaction, then the photo folders are swapped. A bad file leaves the current data untouched
     * (rows are parsed fully before the transaction starts; photos go to a staging folder first).
     */
    fun restore(db: SupportSQLiteDatabase, currentDbVersion: Int, filesDir: File, input: InputStream): RestoreResult {
        val staging = File(filesDir, "restore_tmp").apply { deleteRecursively(); mkdirs() }
        try {
            var app: String? = null
            var created = 0L
            var dbv = 0
            var appv = ""
            val tables = linkedMapOf<String, List<ContentValues>>()
            var photoCount = 0
            JsonReader(input.reader(Charsets.UTF_8)).use { r ->
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "app" -> app = r.nextString()
                        "createdAt" -> created = r.nextLong()
                        "dbVersion" -> dbv = r.nextInt()
                        "appVersion" -> appv = r.nextString()
                        "tables" -> {
                            if (app != APP_ID) return RestoreResult.NotABackup
                            if (dbv > currentDbVersion) return RestoreResult.TooNew(dbv)
                            r.beginObject()
                            while (r.hasNext()) {
                                val t = r.nextName()
                                val rows = mutableListOf<ContentValues>()
                                r.beginArray()
                                while (r.hasNext()) rows += readRow(r)
                                r.endArray()
                                tables[t] = rows
                            }
                            r.endObject()
                        }
                        "files" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                var path: String? = null
                                var data: String? = null
                                r.beginObject()
                                while (r.hasNext()) {
                                    when (r.nextName()) {
                                        "path" -> path = r.nextString()
                                        "data" -> data = r.nextString()
                                        else -> r.skipValue()
                                    }
                                }
                                r.endObject()
                                val p = path ?: continue
                                val top = p.substringBefore('/')
                                if (top !in PHOTO_DIRS || p.contains("..")) continue
                                val f = File(staging, p)
                                f.parentFile?.mkdirs()
                                f.writeBytes(Base64.decode(data ?: "", Base64.NO_WRAP))
                                photoCount++
                            }
                            r.endArray()
                        }
                        else -> r.skipValue()
                    }
                }
                r.endObject()
            }
            if (app != APP_ID) return RestoreResult.NotABackup
            if (dbv > currentDbVersion) return RestoreResult.TooNew(dbv)

            val known = tableNames(db).toSet()
            db.execSQL("PRAGMA foreign_keys=OFF")
            try {
                db.beginTransaction()
                try {
                    known.forEach { db.execSQL("DELETE FROM `$it`") }
                    for ((t, rows) in tables) {
                        if (t !in known) continue
                        rows.forEach { db.insert(t, SQLiteDatabase.CONFLICT_REPLACE, it) }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } finally {
                db.execSQL("PRAGMA foreign_keys=ON")
            }

            for (dir in PHOTO_DIRS) {
                val live = File(filesDir, dir)
                val staged = File(staging, dir)
                live.deleteRecursively()
                if (staged.isDirectory) staged.copyRecursively(live, overwrite = true)
            }
            val counts = tables.mapValues { it.value.size.toLong() }
            return RestoreResult.Ok(Summary(created, dbv, appv, counts, photoCount))
        } catch (e: Exception) {
            return RestoreResult.Broken(e.javaClass.simpleName)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun readRow(r: JsonReader): ContentValues {
        val cv = ContentValues()
        r.beginObject()
        while (r.hasNext()) {
            val col = r.nextName()
            when (r.peek()) {
                JsonToken.NULL -> { r.nextNull(); cv.putNull(col) }
                JsonToken.NUMBER -> {
                    val raw = r.nextString()
                    val asLong = raw.toLongOrNull()
                    if (asLong != null) cv.put(col, asLong) else cv.put(col, raw.toDouble())
                }
                JsonToken.BOOLEAN -> cv.put(col, if (r.nextBoolean()) 1L else 0L)
                JsonToken.BEGIN_OBJECT -> {
                    r.beginObject()
                    var bytes: ByteArray? = null
                    while (r.hasNext()) {
                        if (r.nextName() == BLOB) bytes = Base64.decode(r.nextString(), Base64.NO_WRAP) else r.skipValue()
                    }
                    r.endObject()
                    if (bytes == null) cv.putNull(col) else cv.put(col, bytes)
                }
                else -> cv.put(col, r.nextString())
            }
        }
        r.endObject()
        return cv
    }

    private const val BLOB = "\$blob"

    fun fileName(now: java.time.ZonedDateTime): String =
        "VrajKheti-backup-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", java.util.Locale.ROOT)) + ".json"
}
