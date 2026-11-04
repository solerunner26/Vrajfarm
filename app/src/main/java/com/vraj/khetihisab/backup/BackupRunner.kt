package com.vraj.khetihisab.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import java.io.File
import java.time.ZonedDateTime

/**
 * The worker's logic without WorkManager (JVM-tested with MockWebServer): snapshot → upload → classify →
 * persist status → tidy local ZIPs.
 */
class BackupRunner(
    private val settings: BackupSettings,
    private val status: BackupStatusRepository,
    private val uploader: BackupUploader,
    private val snapshot: suspend (ZonedDateTime) -> BackupZipBuilder.Output,
    private val zipDir: File,
    private val clock: () -> ZonedDateTime,
    /** Endpoint resolver; defaults to the validated (https, non-placeholder) setting. */
    private val endpoint: () -> HttpUrl? = settings::endpointHttpUrl,
    private val maxUploadBytes: Long = DEFAULT_MAX_UPLOAD_BYTES,
    private val onRecovered: () -> Unit = {},
    private val onFinalFailure: (BackupFailure) -> Unit = {},
) {
    sealed interface Outcome {
        data class Success(val stored: String?, val bytes: Long) : Outcome
        data class Retry(val failure: BackupFailure) : Outcome
        data class Failed(val failure: BackupFailure) : Outcome
        /** Safety-net run skipped because a recent backup succeeded. */
        data object Skipped : Outcome
    }

    suspend fun run(trigger: BackupTrigger, runAttemptCount: Int): Outcome = LOCK.withLock {
        val nowMs = clock().toInstant().toEpochMilli()
        val before = status.current()
        if (trigger == BackupTrigger.SAFETY) {
            val last = before.lastSuccessAt
            if (last != null && nowMs - last < SAFETY_SKIP_MS) return@withLock Outcome.Skipped
        }
        status.recordAttempt()

        val url = endpoint()
        val key = settings.apiKey()
        if (url == null || key == null) return@withLock finish(BackupFailure.NOT_CONFIGURED, runAttemptCount, trigger)

        val zip = try {
            withContext(Dispatchers.IO) { snapshot(clock()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock finish(BackupFailure.LOCAL, runAttemptCount, trigger)
        }
        if (zip.bytes > maxUploadBytes) {
            // Do not spend mobile data on an upload the server will reject with 413.
            return@withLock finish(BackupFailure.SIZE, runAttemptCount, trigger)
        }

        val outcome = withContext(Dispatchers.IO) {
            uploader.upload(url, key, settings.installId, zip.zip, clock().toLocalDate(), zip.sha256)
        }
        val (failure, response) = when (outcome) {
            is BackupUploader.Outcome.Http -> outcome.response.failure to outcome.response
            is BackupUploader.Outcome.NetworkError -> BackupFailure.NETWORK to null
        }
        if (failure == null) {
            status.recordSuccess(response?.bytes ?: zip.bytes, response?.stored)
            deleteLocalZips(keep = 0)
            if (before.lastWasFailure) onRecovered()
            return@withLock Outcome.Success(response?.stored, response?.bytes ?: zip.bytes)
        }
        finish(failure, runAttemptCount, trigger)
    }

    private suspend fun finish(failure: BackupFailure, runAttemptCount: Int, trigger: BackupTrigger): Outcome {
        deleteLocalZips(keep = KEEP_FAILED_ZIPS)
        // The periodic safety net never retries on its own; it comes back in 12 h.
        val decision = if (trigger == BackupTrigger.SAFETY) BackupErrorPolicy.Decision.Fail(failure)
        else BackupErrorPolicy.decide(failure, runAttemptCount)
        return when (decision) {
            is BackupErrorPolicy.Decision.Retry -> Outcome.Retry(failure)
            else -> {
                status.recordFailure(failure)
                if (failure != BackupFailure.NOT_CONFIGURED) onFinalFailure(failure)
                Outcome.Failed(failure)
            }
        }
    }

    /** doc 15 step 6: keep the newest 3 ZIPs only while uploads fail; none after a success. */
    internal fun deleteLocalZips(keep: Int) {
        val zips = zipDir.listFiles { f -> f.isFile && (f.name.endsWith(".zip") || f.name.endsWith(".part")) }
            ?.sortedByDescending { it.lastModified() } ?: return
        zips.filter { it.name.endsWith(".part") }.forEach { it.delete() }
        zips.filter { it.name.endsWith(".zip") }.drop(keep).forEach { it.delete() }
    }

    companion object {
        const val KEEP_FAILED_ZIPS = 3
        /** Server default max_mb = 200 (server/README.md). */
        const val DEFAULT_MAX_UPLOAD_BYTES = 200L * 1024 * 1024
        const val SAFETY_SKIP_MS = 20L * 60 * 60 * 1000

        /** Serialises the nightly, manual and safety runs inside one process (they share the ZIP path). */
        private val LOCK = Mutex()
    }
}
