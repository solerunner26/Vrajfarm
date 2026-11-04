package com.vraj.khetihisab.backup

import android.content.Context
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.data.repo.LedgerMath
import com.vraj.khetihisab.report.GujaratiPdfGenerator
import com.vraj.khetihisab.report.ReportType
import java.io.File
import java.time.ZonedDateTime

/** Everything the backup worker / controller needs. */
class BackupDeps(
    val settings: BackupSettings,
    val status: BackupStatusRepository,
    val uploader: BackupUploader,
    val snapshotter: BackupSnapshotter,
    val notifier: BackupNotifier,
    val clock: () -> ZonedDateTime = { ZonedDateTime.now(LedgerMath.IST) },
) {
    fun runner(): BackupRunner = BackupRunner(
        settings = settings,
        status = status,
        uploader = uploader,
        snapshot = snapshotter::snapshot,
        zipDir = snapshotter.zipDir,
        clock = clock,
        onRecovered = notifier::notifyRecovered,
        onFinalFailure = notifier::notifyFailed,
    )
}

/**
 * Process-wide backup dependencies. Workers and receivers are created by the system, so they cannot take
 * constructor arguments; they read [deps]. AppContainer may expose the same instance (see INTEGRATION.md).
 * Tests set [override].
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

    private fun build(app: Context): BackupDeps {
        val db = AppDatabase.get(app)
        val pdf = GujaratiPdfGenerator(app, db)
        return BackupDeps(
            settings = BackupSettings.create(app),
            status = BackupStatusRepository(app.backupStatusDataStore),
            uploader = BackupUploader(),
            snapshotter = BackupSnapshotter(
                db = db,
                dbFile = app.getDatabasePath(AppDatabase.NAME),
                filesDir = app.filesDir,
                zipDir = File(app.cacheDir, "backup"),
                dailyPdf = { day -> pdf.generate(ReportType.DAILY_LEDGER, DateRange.day(day)) },
            ),
            notifier = BackupNotifier(app),
        )
    }
}
