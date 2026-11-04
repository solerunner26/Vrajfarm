package com.vraj.khetihisab.backup

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZonedDateTime

/**
 * Nightly / manual backup: build the .json, then copy it to Google Drive when a Drive file was chosen
 * (that is the automatic "sync"). WhatsApp and e-mail are user actions on the Backup screen.
 */
class BackupRunner(
    private val status: BackupStatusRepository,
    private val driveUri: () -> Uri?,
    private val makeFile: suspend (ZonedDateTime) -> File,
    private val toDrive: (Uri, File) -> Boolean,
    private val clock: () -> ZonedDateTime,
    private val onReadyToSend: () -> Unit = {},
    private val onFinalFailure: (BackupFailure) -> Unit = {},
) {
    sealed interface Outcome {
        data class Success(val file: File, val drive: Boolean) : Outcome
        data class Failed(val failure: BackupFailure) : Outcome
        data object Skipped : Outcome
    }

    suspend fun run(trigger: BackupTrigger): Outcome = LOCK.withLock {
        val nowMs = clock().toInstant().toEpochMilli()
        if (trigger == BackupTrigger.SAFETY) {
            val last = status.current().lastSuccessAt
            if (last != null && nowMs - last < SAFETY_SKIP_MS) return@withLock Outcome.Skipped
        }
        status.recordAttempt()
        val file = try {
            withContext(Dispatchers.IO) { makeFile(clock()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status.recordFailure(BackupFailure.LOCAL)
            onFinalFailure(BackupFailure.LOCAL)
            return@withLock Outcome.Failed(BackupFailure.LOCAL)
        }
        val drive = driveUri()
        if (drive == null) {
            // Nothing automatic to send to: the file is ready; remind the user once a night.
            status.recordFailure(BackupFailure.NOT_CONFIGURED)
            if (trigger == BackupTrigger.NIGHTLY) onReadyToSend()
            return@withLock Outcome.Success(file, drive = false)
        }
        val ok = withContext(Dispatchers.IO) { toDrive(drive, file) }
        if (!ok) {
            status.recordFailure(BackupFailure.DRIVE)
            onFinalFailure(BackupFailure.DRIVE)
            return@withLock Outcome.Failed(BackupFailure.DRIVE)
        }
        status.recordSuccess(file.length(), "drive")
        Outcome.Success(file, drive = true)
    }

    companion object {
        const val SAFETY_SKIP_MS = 20L * 60 * 60 * 1000
        private val LOCK = Mutex()
    }
}
