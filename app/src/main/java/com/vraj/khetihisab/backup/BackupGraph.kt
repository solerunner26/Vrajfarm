package com.vraj.khetihisab.backup

import android.content.Context
import com.vraj.khetihisab.BuildConfig
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.LedgerMath
import java.io.File
import java.time.ZonedDateTime

/** Everything the backup worker and the Backup screen need. */
class BackupDeps(
    private val app: Context,
    val status: BackupStatusRepository,
    val prefs: BackupPrefs,
    val notifier: BackupNotifier,
    val clock: () -> ZonedDateTime = { ZonedDateTime.now(LedgerMath.IST) },
) {
    /** filesDir/backup/ — shared through the FileProvider (`backup` path). Only the newest file is kept. */
    val dir: File get() = File(app.filesDir, "backup").apply { mkdirs() }

    /** Builds a fresh .json of everything and returns it. */
    suspend fun makeBackupFile(now: ZonedDateTime = clock()): File {
        val db = AppDatabase.get(app).openHelper.writableDatabase
        val out = File(dir, JsonBackup.fileName(now))
        val tmp = File(dir, out.name + ".part")
        tmp.outputStream().use {
            JsonBackup.export(db, AppDatabase.VERSION, BuildConfig.VERSION_NAME, app.filesDir, it, now.toInstant().toEpochMilli())
        }
        dir.listFiles()?.filter { it.name.endsWith(".json") }?.forEach { it.delete() }
        if (!tmp.renameTo(out)) error("rename failed")
        return out
    }

    fun latestFile(): File? = dir.listFiles()?.filter { it.name.endsWith(".json") }?.maxByOrNull { it.lastModified() }

    fun runner(): BackupRunner = BackupRunner(
        status = status,
        driveUri = { prefs.driveUri },
        makeFile = ::makeBackupFile,
        toDrive = { uri, f -> BackupShare.writeToDrive(app, uri, f) },
        clock = clock,
        onReadyToSend = notifier::notifyReadyToSend,
        onFinalFailure = notifier::notifyFailed,
    )
}

/**
 * Process-wide backup dependencies. Workers and receivers are created by the system, so they read
 * [deps]. Tests set [override].
 */
object BackupGraph {
    @Volatile var override: ((Context) -> BackupDeps)? = null
    @Volatile private var instance: BackupDeps? = null

    fun deps(context: Context): BackupDeps {
        override?.let { return it(context) }
        return instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }
    }

    private fun build(app: Context) = BackupDeps(
        app = app,
        status = BackupStatusRepository(app.backupStatusDataStore),
        prefs = BackupPrefs.of(app),
        notifier = BackupNotifier(app),
    )
}
