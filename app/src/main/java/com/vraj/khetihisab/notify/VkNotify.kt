package com.vraj.khetihisab.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vraj.khetihisab.MainActivity
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.theme.VkThemes
import com.vraj.khetihisab.widget.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Deep links opened by notifications and widgets. MainActivity routes them (see INTEGRATION.md). */
object VkDeepLinks {
    const val SCHEME = "vraj"
    const val REMINDERS = "vraj://reminders"
    const val SHOPPING = "vraj://shopping"
    const val PANCHANG = "vraj://panchang"

    /** Opens the calendar day sheet for [date]: `vraj://panchang/{epochDay}`. */
    fun panchangDay(date: LocalDate) = "$PANCHANG/${date.toEpochDay()}"

    /** Explicit intent to MainActivity carrying [uri] as data. */
    fun intent(context: Context, uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun pending(context: Context, uri: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            uri.hashCode(),
            intent(context, uri),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** Shared notification plumbing: permission check, small icon, theme accent. */
object VkNotify {
    private const val TAG = "VkNotify"

    /** Monochrome white glyph. A dedicated `ic_stat_vk` (doc 13) can replace it later. */
    val SMALL_ICON: Int = R.drawable.ic_launcher_monochrome

    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    /** Accent = theme primary (doc 13). */
    suspend fun accent(context: Context): Int = VkThemes.palette(WidgetTheme.themeId(context)).p.toArgb()

    fun builder(context: Context, channel: String, accent: Int): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(SMALL_ICON)
            .setColor(accent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

    @SuppressLint("MissingPermission") // checked by canPost
    fun post(context: Context, id: Int, n: Notification): Boolean {
        if (!canPost(context)) return false
        return try {
            NotificationManagerCompat.from(context).notify(id, n)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "notify denied", e)
            false
        }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    /** PendingIntent to one of our receivers; [uri] makes it unique. */
    fun broadcast(context: Context, cls: Class<out BroadcastReceiver>, action: String, uri: String, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, cls).setAction(action).setData(Uri.parse(uri)).apply(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** Runs [block] off the main thread inside `goAsync()` (receivers have ~10 s). */
internal fun BroadcastReceiver.launchAsync(tag: String, block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(tag, "receiver failed", t)
        } finally {
            pending.finish()
        }
    }
}
