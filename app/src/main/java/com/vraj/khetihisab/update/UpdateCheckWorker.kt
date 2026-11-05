package com.vraj.khetihisab.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vraj.khetihisab.R
import java.util.concurrent.TimeUnit

/**
 * Twice a day, in the background: is there a newer release on GitHub? If so, a notification
 * "નવું અપડેટ આવ્યું છે" opens the app, where the one-button update dialog is waiting.
 * (Owner 2026-11-05: the Pixel never showed the update while the app stayed open in the background.)
 */
class UpdateCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val u = runCatching { AppUpdater.check() }.getOrNull() ?: return Result.success()
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // One notification per version, not every 12 hours.
        if (prefs.getInt(K_NOTIFIED, 0) >= u.versionCode) return Result.success()
        if (notify(ctx, u)) prefs.edit().putInt(K_NOTIFIED, u.versionCode).apply()
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "vk_update_check"
        private const val CHANNEL = "app_update"
        private const val ID = 4601
        private const val PREFS = "vk_update"
        private const val K_NOTIFIED = "notified_code"

        fun ensureScheduled(context: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        private fun notify(ctx: Context, u: UpdateInfo): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.v4_up_channel), NotificationManager.IMPORTANCE_DEFAULT))
            }
            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: return false
            val pi = PendingIntent.getActivity(ctx, 7, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_vk_settings)
                .setContentTitle(ctx.getString(R.string.v2_up_title))
                .setContentText(ctx.getString(R.string.v4_up_nt_body, u.versionName))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            return try {
                NotificationManagerCompat.from(ctx).notify(ID, n)
                true
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
