package com.vraj.khetihisab.backup

import android.database.sqlite.SQLiteException
import com.vraj.khetihisab.BuildConfig
import com.vraj.khetihisab.data.AppDatabase
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Worker steps 1–3 (doc 15): WAL checkpoint, today's daily-ledger PDF, ZIP with manifest.
 * The ZIP goes to `cacheDir/backup/vraj_<yyyyMMdd>.zip`.
 */
class BackupSnapshotter(
    private val db: AppDatabase,
    private val dbFile: File,
    private val filesDir: File,
    val zipDir: File,
    /** Generates today's daily ledger PDF; null / exception = backup continues without it. */
    private val dailyPdf: suspend (LocalDate) -> File?,
) {
    suspend fun snapshot(now: ZonedDateTime): BackupZipBuilder.Output {
        checkpoint()
        val counts = tableCounts()
        val today = now.toLocalDate()
        val pdf = runCatching { dailyPdf(today) }.getOrNull()
        val input = BackupZipBuilder.Input(
            dbFiles = listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")),
            photoDirs = BackupZipBuilder.PHOTO_DIRS.associateWith { File(filesDir, it) },
            dailyPdf = pdf,
            dailyPdfEntry = "reports/daily_$today.pdf",
            dbVersion = AppDatabase.VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            counts = counts,
            createdAt = now,
        )
        return BackupZipBuilder.build(input, File(zipDir, zipName(today)))
    }

    /** `PRAGMA wal_checkpoint(FULL)`; when the DB is busy/locked, retry once after 2 s (doc 15 edge case). */
    private suspend fun checkpoint() {
        repeat(2) { attempt ->
            val busy = try {
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { c ->
                    c.moveToFirst() && c.getInt(0) != 0
                }
            } catch (e: SQLiteException) {
                if (attempt == 1) throw e
                true
            }
            if (!busy) return
            if (attempt == 0) delay(2_000)
        }
        // Still busy after the retry: continue — the -wal file is zipped too, so no data is lost.
    }

    /** Row count of every app table (Room bookkeeping tables excluded), for the manifest / server email. */
    private fun tableCounts(): Map<String, Long> {
        val sdb = db.openHelper.readableDatabase
        val names = mutableListOf<String>()
        sdb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT IN ('android_metadata','room_master_table') ORDER BY name",
        ).use { c -> while (c.moveToNext()) names += c.getString(0) }
        return names.associateWith { t ->
            sdb.query("SELECT COUNT(*) FROM \"${t.replace("\"", "\"\"")}\"").use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
        }
    }

    companion object {
        private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT)
        fun zipName(day: LocalDate): String = "vraj_${day.format(DAY)}.zip"
    }
}
