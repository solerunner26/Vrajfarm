package com.vraj.khetihisab.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * Nightly 23:45 full snapshot upload (doc 15 "Worker steps"). The logic lives in [BackupRunner];
 * this class maps its outcome to WorkManager results:
 * - success → `Result.success(stored, bytes)`
 * - retryable (429 / 5xx / network / hash / local) and attempts left → `Result.retry()` (exponential, 10 min)
 * - 401 / 403 / 413 / 400-client / 404-405 / config / disk / not configured, or 6th attempt → `Result.failure()`
 */
class NightlyBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val deps = BackupGraph.deps(applicationContext)
        val trigger = BackupTrigger.from(inputData.getString(BackupTrigger.KEY))
        return when (val o = deps.runner().run(trigger, runAttemptCount)) {
            is BackupRunner.Outcome.Success -> Result.success(
                workDataOf(OUT_STORED to o.stored, OUT_BYTES to o.bytes),
            )
            BackupRunner.Outcome.Skipped -> Result.success()
            is BackupRunner.Outcome.Retry -> Result.retry()
            is BackupRunner.Outcome.Failed ->
                // Periodic safety-net work must not end in FAILED (that would cancel the periodic chain on some versions).
                if (trigger == BackupTrigger.SAFETY) Result.success() else Result.failure(workDataOf(OUT_ERROR to o.failure.code))
        }
    }

    /** Required for expedited work on API < 31, where WorkManager runs it as a foreground service. */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val n = BackupNotifier(applicationContext).progressNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BackupNotifier.ID_PROGRESS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BackupNotifier.ID_PROGRESS, n)
        }
    }

    companion object {
        const val OUT_STORED = "stored"
        const val OUT_BYTES = "bytes"
        const val OUT_ERROR = "error"
    }
}
