package com.vraj.khetihisab.backup

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vraj.khetihisab.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Separate DataStore file so backup state never contends with UI prefs. */
val Context.backupStatusDataStore: DataStore<Preferences> by preferencesDataStore(name = "vk_backup_status")

/** What the Home pill shows. */
enum class BackupState(@StringRes val pillRes: Int) {
    /** Last attempt succeeded within [BackupStatus.STALE_AFTER_MS]. "બેકઅપ OK" */
    OK(R.string.bk_pill_ok),
    /** The most recent attempt failed after the last success. "બેકઅપ નિષ્ફળ" */
    FAILED(R.string.bk_pill_failed),
    /** Succeeded before, but not for more than 48 h (e.g. phone off). "બેકઅપ જૂનો" */
    STALE(R.string.bk_pill_stale),
    /** Never backed up (usually: URL / key not set yet). "બેકઅપ બાકી" */
    NEVER(R.string.bk_pill_never),
}

data class BackupStatus(
    val lastSuccessAt: Long? = null,
    val lastBytes: Long? = null,
    val lastStoredName: String? = null,
    val lastFailureAt: Long? = null,
    val lastFailure: BackupFailure? = null,
    val lastAttemptAt: Long? = null,
    /** Next scheduled alarm (epoch millis), for the Settings "આગલો" row. */
    val nextRunAt: Long? = null,
) {
    fun stateAt(nowMillis: Long): BackupState {
        val success = lastSuccessAt
        val failure = lastFailureAt
        return when {
            failure != null && (success == null || failure > success) -> BackupState.FAILED
            success == null -> BackupState.NEVER
            nowMillis - success > STALE_AFTER_MS -> BackupState.STALE
            else -> BackupState.OK
        }
    }

    val lastWasFailure: Boolean get() = lastFailureAt != null && (lastSuccessAt == null || lastFailureAt > lastSuccessAt)

    companion object {
        const val STALE_AFTER_MS = 48L * 60 * 60 * 1000
    }
}

/** Persists backup outcomes (doc 15 step 5) for the Home "બેકઅપ OK" pill and the Settings backup card. */
class BackupStatusRepository(
    private val store: DataStore<Preferences>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val status: Flow<BackupStatus> = store.data.map(::read)

    suspend fun current(): BackupStatus = status.first()

    suspend fun recordAttempt() {
        store.edit { it[K_ATTEMPT] = clock() }
    }

    suspend fun recordSuccess(bytes: Long, storedName: String?) {
        store.edit {
            it[K_SUCCESS] = clock()
            it[K_BYTES] = bytes
            if (storedName != null) it[K_STORED] = storedName else it.remove(K_STORED)
        }
    }

    suspend fun recordFailure(failure: BackupFailure) {
        store.edit {
            it[K_FAILURE_AT] = clock()
            it[K_FAILURE] = failure.code
        }
    }

    suspend fun setNextRunAt(millis: Long?) {
        store.edit { if (millis != null) it[K_NEXT] = millis else it.remove(K_NEXT) }
    }

    private fun read(p: Preferences) = BackupStatus(
        lastSuccessAt = p[K_SUCCESS],
        lastBytes = p[K_BYTES],
        lastStoredName = p[K_STORED],
        lastFailureAt = p[K_FAILURE_AT],
        lastFailure = BackupFailure.fromCode(p[K_FAILURE]),
        lastAttemptAt = p[K_ATTEMPT],
        nextRunAt = p[K_NEXT],
    )

    companion object {
        private val K_SUCCESS = longPreferencesKey("last_success_at")
        private val K_BYTES = longPreferencesKey("last_bytes")
        private val K_STORED = stringPreferencesKey("last_stored")
        private val K_FAILURE_AT = longPreferencesKey("last_failure_at")
        private val K_FAILURE = stringPreferencesKey("last_failure")
        private val K_ATTEMPT = longPreferencesKey("last_attempt_at")
        private val K_NEXT = longPreferencesKey("next_run_at")
    }
}
