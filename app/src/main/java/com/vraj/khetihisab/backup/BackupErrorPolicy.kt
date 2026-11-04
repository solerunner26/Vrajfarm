package com.vraj.khetihisab.backup

import androidx.annotation.StringRes
import com.vraj.khetihisab.R

/**
 * Why a nightly backup did not finish. [code] is persisted in DataStore.
 * (The GoDaddy server and its HTTP error codes were removed on 2026-11-04.)
 */
enum class BackupFailure(val code: String, @StringRes val messageRes: Int) {
    /** No automatic destination chosen yet (Drive not picked) — the file is made, nothing is wrong. */
    NOT_CONFIGURED("not_configured", R.string.v2_bk_not_configured),
    /** Could not build the .json (disk full, database busy …). */
    LOCAL("local", R.string.bk_err_local),
    /** The chosen Google Drive file can no longer be written (deleted, or Drive signed out). */
    DRIVE("drive", R.string.v2_bk_err_drive),
    ;

    companion object {
        fun fromCode(code: String?): BackupFailure? = entries.firstOrNull { it.code == code }
    }
}
