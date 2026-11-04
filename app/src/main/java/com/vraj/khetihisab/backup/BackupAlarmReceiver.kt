package com.vraj.khetihisab.backup

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 23:45 alarm target, plus re-arming after boot / clock changes / exact-alarm permission changes.
 *
 * If the lead prefers the shared `BootReceiver` (doc 13), that receiver can call
 * [BackupAlarmReceiver.reschedule] instead and this receiver only needs [ACTION_RUN] in the manifest.
 */
class BackupAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val scheduler = BackupScheduler(app)
        when (intent.action) {
            ACTION_RUN -> scheduler.enqueueBackup(BackupTrigger.NIGHTLY)
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> Unit
            else -> return
        }
        val pending = goAsync()
        scope.launch {
            try {
                reschedule(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RUN = "com.vraj.khetihisab.backup.RUN"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Re-arms the alarm + safety net and stores the next run time for the Settings card. */
        suspend fun reschedule(context: Context) {
            val s = BackupScheduler(context.applicationContext).ensureScheduled()
            runCatching {
                BackupGraph.deps(context).status.setNextRunAt(s.at.toInstant().toEpochMilli())
            }
        }
    }
}
