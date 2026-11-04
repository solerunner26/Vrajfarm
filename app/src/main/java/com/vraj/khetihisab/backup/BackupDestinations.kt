package com.vraj.khetihisab.backup

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import com.vraj.khetihisab.R
import java.io.File

/**
 * Where a backup goes (owner request 2026-11-04 — no web server any more):
 * 1. **Google Drive, automatic**: the user picks a file in Drive once (system "Save to" picker); the app
 *    keeps write access to that one file and overwrites it every night = sync. No Google sign-in setup.
 * 2. **WhatsApp**: the .json opens in WhatsApp's share screen; the user picks a chat (e.g. "You").
 * 3. **E-mail**: Gmail opens with the file attached and the saved address filled in.
 * WhatsApp and e-mail only OPEN pre-filled (CLAUDE.md): the user presses send.
 */
class BackupPrefs(private val prefs: SharedPreferences) {
    var driveUri: Uri?
        get() = prefs.getString(K_DRIVE, null)?.let(Uri::parse)
        set(v) { prefs.edit().putString(K_DRIVE, v?.toString()).apply() }

    var driveLabel: String?
        get() = prefs.getString(K_DRIVE_LABEL, null)
        set(v) { prefs.edit().putString(K_DRIVE_LABEL, v).apply() }

    var email: String
        get() = prefs.getString(K_EMAIL, "").orEmpty()
        set(v) { prefs.edit().putString(K_EMAIL, v.trim()).apply() }

    val driveConfigured: Boolean get() = driveUri != null

    companion object {
        const val NAME = "vk_backup_prefs"
        private const val K_DRIVE = "drive_uri"
        private const val K_DRIVE_LABEL = "drive_label"
        private const val K_EMAIL = "email"

        fun of(context: Context) = BackupPrefs(context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE))
    }
}

object BackupShare {
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Opens WhatsApp's chat picker with the backup attached. Falls back to the system share sheet. */
    fun whatsApp(context: Context, file: File): Boolean {
        val uri = uriFor(context, file)
        val base = Intent(Intent.ACTION_SEND).apply {
            type = JsonBackup.MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.v2_bk_share_caption, file.name))
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
            try {
                context.startActivity(Intent(base).setPackage(pkg))
                return true
            } catch (_: ActivityNotFoundException) {
            }
        }
        return chooser(context, base)
    }

    /** Opens the mail app (Gmail) with the address, subject and the backup attached. */
    fun email(context: Context, file: File, to: String): Boolean {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = JsonBackup.MIME
            if (to.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(to.trim()))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.v2_bk_mail_subject, file.name))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.v2_bk_mail_body))
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent(send).setPackage("com.google.android.gm"))
            return true
        } catch (_: ActivityNotFoundException) {
        }
        return chooser(context, send)
    }

    private fun chooser(context: Context, send: Intent): Boolean = try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /** Overwrites the chosen Drive file. False when access was lost (file deleted / Drive signed out). */
    fun writeToDrive(context: Context, uri: Uri, file: File): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } } != null
    }.getOrDefault(false)
}
