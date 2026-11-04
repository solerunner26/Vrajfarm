package com.vraj.khetihisab.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A backup file the user opened from WhatsApp / Gmail / Files ("Open with Vraj Kheti"). The Backup
 * screen picks it up and asks before restoring. Read access lasts while the activity is alive.
 */
object RestoreInbox {
    val uri = MutableStateFlow<Uri?>(null)

    /** True when [intent] carried a file for us (it is then parked in [uri]). */
    fun accept(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val data = intent.data ?: return false
        // content:// only: a file:// link could point at our own private files.
        if (data.scheme != "content") return false
        uri.value = data
        return true
    }

    /** Relaunches the app so every screen re-reads the restored database. */
    fun restartApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launch)
        Runtime.getRuntime().exit(0)
    }
}
