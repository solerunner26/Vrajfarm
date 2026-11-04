package com.vraj.khetihisab.backup

import androidx.annotation.StringRes
import com.vraj.khetihisab.R

/**
 * Why a backup did not succeed. [code] is persisted in DataStore; [retryable] drives `Result.retry()`.
 * Server codes come from server/README.md ("API" table).
 */
enum class BackupFailure(val code: String, val retryable: Boolean, @StringRes val messageRes: Int) {
    /** No key / placeholder URL — nothing was sent. */
    NOT_CONFIGURED("not_configured", false, R.string.bk_err_not_configured),
    /** 401 auth — wrong or missing key. Retrying only burns the 10/h rate limit. */
    AUTH("auth", false, R.string.bk_err_auth),
    /** 403 https — server refuses plain http. */
    HTTPS("https", false, R.string.bk_err_https),
    /** 413 size — ZIP over max_mb / PHP limits. */
    SIZE("size", false, R.string.bk_too_big),
    /** 429 rate — too many requests this hour. */
    RATE("rate", true, R.string.bk_err_rate),
    /** 400 hash — bytes changed in transit; a fresh upload usually fixes it. */
    HASH("hash", true, R.string.bk_err_transfer),
    /** 400 file / upload — interrupted upload. */
    UPLOAD("upload", true, R.string.bk_err_transfer),
    /** 400 date / sha256 / type and other 4xx — a client bug; retrying sends the same thing. */
    BAD_REQUEST("bad_request", false, R.string.bk_err_bad_request),
    /** 404 / 405 — wrong URL (or a redirect turned the POST into a GET). */
    WRONG_URL("wrong_url", false, R.string.bk_err_wrong_url),
    /** 500 config — owner must fix vraj_config.php. */
    SERVER_CONFIG("config", false, R.string.bk_err_server_config),
    /** 507 disk — hosting disk is full. */
    DISK("disk", false, R.string.bk_err_disk),
    /** Other 5xx (store / server / gateway). */
    SERVER("server", true, R.string.bk_err_server),
    /** IOException: no network, DNS, TLS, timeout. */
    NETWORK("network", true, R.string.bk_err_network),
    /** Local failure building the ZIP (disk full, DB locked …). */
    LOCAL("local", true, R.string.bk_err_local),
    /** 200 without ok:true, or anything unexpected. */
    UNKNOWN("unknown", true, R.string.bk_err_unknown),
    ;

    companion object {
        fun fromCode(code: String?): BackupFailure? = entries.firstOrNull { it.code == code }
    }
}

/** Pure mapping of server responses to outcomes, and of outcomes to WorkManager results. */
object BackupErrorPolicy {
    /** doc 15: exponential backoff from 10 min, up to 6 attempts in total. */
    const val MAX_ATTEMPTS = 6
    const val BACKOFF_MINUTES = 10L

    sealed interface Decision {
        data object Success : Decision
        data class Retry(val failure: BackupFailure) : Decision
        data class Fail(val failure: BackupFailure) : Decision
    }

    /**
     * Classifies an HTTP response. Returns null for success (200 + `"ok":true`).
     * The JSON `error` field is the reliable signal (server/README.md); the status code is the fallback.
     */
    fun classify(httpCode: Int, ok: Boolean, errorField: String?): BackupFailure? {
        if (httpCode in 200..299 && ok) return null
        when (errorField) {
            "auth" -> return BackupFailure.AUTH
            "https" -> return BackupFailure.HTTPS
            "size" -> return BackupFailure.SIZE
            "rate" -> return BackupFailure.RATE
            "hash" -> return BackupFailure.HASH
            "file", "upload" -> return BackupFailure.UPLOAD
            "date", "sha256", "type" -> return BackupFailure.BAD_REQUEST
            "method" -> return BackupFailure.WRONG_URL
            "config" -> return BackupFailure.SERVER_CONFIG
            "disk" -> return BackupFailure.DISK
            "store", "server" -> return BackupFailure.SERVER
        }
        return when (httpCode) {
            in 200..299 -> BackupFailure.UNKNOWN
            // Redirects are not followed (a redirected POST becomes a GET): the URL needs fixing.
            in 300..399 -> BackupFailure.WRONG_URL
            401 -> BackupFailure.AUTH
            403 -> BackupFailure.HTTPS
            404, 405 -> BackupFailure.WRONG_URL
            408 -> BackupFailure.NETWORK
            413 -> BackupFailure.SIZE
            429 -> BackupFailure.RATE
            507 -> BackupFailure.DISK
            in 400..499 -> BackupFailure.BAD_REQUEST
            in 500..599 -> BackupFailure.SERVER
            else -> BackupFailure.UNKNOWN
        }
    }

    /**
     * @param runAttemptCount WorkManager's `runAttemptCount` (0 on the first run).
     * @return Retry while the failure is retryable and attempts remain; Fail otherwise.
     */
    fun decide(failure: BackupFailure?, runAttemptCount: Int, maxAttempts: Int = MAX_ATTEMPTS): Decision = when {
        failure == null -> Decision.Success
        failure.retryable && runAttemptCount + 1 < maxAttempts -> Decision.Retry(failure)
        else -> Decision.Fail(failure)
    }
}
