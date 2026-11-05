package com.vraj.khetihisab.backup

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vraj.khetihisab.R

/** Silent notifications on the "backup" channel (doc 15 step 5). */
class BackupNotifier(context: Context) {
    private val ctx = context.applicationContext

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, ctx.getString(R.string.bk_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = ctx.getString(R.string.bk_channel_desc)
                setShowBadge(false)
            },
        )
    }

    /** Shown only when the previous status was a failure. */
    fun notifyRecovered() = post(ID_RESULT, ctx.getString(R.string.bk_notif_ok_title), ctx.getString(R.string.bk_notif_ok_body))

    /** Nightly, when no Drive file is chosen: the backup is ready — tap to send (opens the Backup screen). */
    fun notifyReadyToSend() = post(ID_RESULT, ctx.getString(R.string.v2_bk_notif_ready_title), ctx.getString(R.string.v2_bk_notif_ready_body))

    /** 21:30: no backup today — a normal-priority, visible reminder that opens the Backup screen. */
    fun notifyBackupDue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel()
        ensureReminderChannel()
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(ctx, REMIND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vk_backup)
            .setContentTitle(ctx.getString(R.string.v3_bk_due_title))
            .setContentText(ctx.getString(R.string.v3_bk_due_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(ctx.getString(R.string.v3_bk_due_body)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx, 2,
                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BACKUP_DEEP_LINK)).setPackage(ctx.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        try {
            nm.notify(ID_DUE, n)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(REMIND_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(REMIND_CHANNEL_ID, ctx.getString(R.string.v3_bk_due_channel), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    fun notifyFailed(failure: BackupFailure) =
        post(ID_RESULT, ctx.getString(R.string.bk_notif_failed_title), ctx.getString(failure.messageRes))

    /** Ongoing notification for expedited work on API < 31 (WorkManager runs it as a foreground service). */
    fun progressNotification(): Notification {
        ensureChannel()
        return builder(ctx.getString(R.string.bk_notif_running), null)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    private fun post(id: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel()
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        try {
            nm.notify(id, builder(title, body, backupScreen = true).setAutoCancel(true).build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently.
        }
    }

    private fun builder(title: String, body: String?, backupScreen: Boolean = false): NotificationCompat.Builder {
        val launch = if (backupScreen) {
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BACKUP_DEEP_LINK)).setPackage(ctx.packageName)
        } else {
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        }
        val pi = launch?.let {
            PendingIntent.getActivity(ctx, if (backupScreen) 1 else 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vk_backup)
            .setContentTitle(title)
            .apply { if (body != null) setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)) }
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pi)
    }

    companion object {
        const val CHANNEL_ID = "backup"
        const val ID_RESULT = 4501
        const val ID_PROGRESS = 4502
        const val ID_DUE = 4503
        const val REMIND_CHANNEL_ID = "backup_reminder"
        const val BACKUP_DEEP_LINK = "vraj://backup"
    }
}
