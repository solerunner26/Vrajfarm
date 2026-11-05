package com.vraj.khetihisab.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vraj.khetihisab.data.repo.LedgerMath
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Pure schedule maths (JVM-tested). IST has no DST, but the maths goes through ZonedDateTime anyway. */
object BackupTime {
    val ZONE: ZoneId = LedgerMath.IST
    val RUN_AT: LocalTime = LocalTime.of(23, 45)
    /** Inexact fallback: `setWindow(23:30, 30 min)`. */
    val WINDOW_START: LocalTime = LocalTime.of(23, 30)
    val WINDOW_LENGTH: Duration = Duration.ofMinutes(30)
    /** A run must be at least this far ahead, so an alarm that fires a little early never re-arms for the same night. */
    val MIN_LEAD: Duration = Duration.ofMinutes(5)

    /** First [at] (in [zone]) strictly later than `now + minLead`. */
    fun nextRun(now: Instant, zone: ZoneId = ZONE, at: LocalTime = RUN_AT, minLead: Duration = MIN_LEAD): ZonedDateTime {
        val earliest = now.plus(minLead)
        val local = earliest.atZone(zone)
        var candidate = local.toLocalDate().atTime(at).atZone(zone)
        if (!candidate.toInstant().isAfter(earliest)) candidate = local.toLocalDate().plusDays(1).atTime(at).atZone(zone)
        return candidate
    }

    /** Window start for the night of [run] (23:30 that evening). */
    fun windowStartFor(run: ZonedDateTime, start: LocalTime = WINDOW_START): ZonedDateTime =
        run.toLocalDate().atTime(start).atZone(run.zone)
}

/** Why a backup run was started (worker input data). */
enum class BackupTrigger { NIGHTLY, MANUAL, SAFETY;

    companion object {
        const val KEY = "trigger"
        fun from(value: String?): BackupTrigger = entries.firstOrNull { it.name == value } ?: NIGHTLY
    }
}

/**
 * 23:45 IST nightly schedule (doc 15 Part B):
 * - AlarmManager `setExactAndAllowWhileIdle` when exact alarms are allowed (`canScheduleExactAlarms()` on 12+),
 *   otherwise `setWindow(23:30, 30 min)`.
 * - The alarm ([BackupAlarmReceiver]) enqueues the unique one-time work "nightly_backup" and re-arms the next night.
 * - A 12 h periodic WorkManager job is the safety net if the alarm is lost (force-stop, OEM killers); the worker
 *   skips it when a backup already succeeded in the last 20 h.
 */
class BackupScheduler(
    context: Context,
    private val clock: () -> Instant = Instant::now,
) {
    private val ctx = context.applicationContext
    private val alarms: AlarmManager? get() = ctx.getSystemService(AlarmManager::class.java)

    data class Scheduled(val at: ZonedDateTime, val exact: Boolean)

    /** Arms the next 23:45 alarm (replacing any previous one). */
    fun scheduleNextAlarm(): Scheduled {
        val run = BackupTime.nextRun(clock())
        val am = alarms ?: return Scheduled(run, false)
        val pi = alarmPendingIntent()
        val exact = canScheduleExact()
        if (exact) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, run.toInstant().toEpochMilli(), pi)
                return Scheduled(run, true)
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call: fall through to the window.
            }
        }
        val start = BackupTime.windowStartFor(run)
        am.setWindow(AlarmManager.RTC_WAKEUP, start.toInstant().toEpochMilli(), BackupTime.WINDOW_LENGTH.toMillis(), pi)
        return Scheduled(run, false)
    }

    fun cancelAlarm() {
        alarms?.cancel(alarmPendingIntent())
    }

    /**
     * 21:30 daily "બેકઅપ લો" reminder (owner request 2026-11-05). The receiver checks the status and
     * only notifies when no backup was taken in the last 24 h.
     */
    fun scheduleReminder(): ZonedDateTime {
        val at = BackupTime.nextRun(clock(), at = REMIND_AT, minLead = java.time.Duration.ofMinutes(1))
        val am = alarms ?: return at
        val pi = PendingIntent.getBroadcast(
            ctx,
            REMIND_REQUEST_CODE,
            Intent(ctx, BackupAlarmReceiver::class.java).setAction(BackupAlarmReceiver.ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val t = at.toInstant().toEpochMilli()
        if (canScheduleExact()) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
                return at
            } catch (_: SecurityException) {
            }
        }
        am.setWindow(AlarmManager.RTC_WAKEUP, t, 10 * 60_000L, pi)
        return at
    }

    fun canScheduleExact(): Boolean {
        val am = alarms ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** Settings deep link to grant "Alarms & reminders" (Android 12+); null below 12. */
    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else null

    /** Unique one-time backup work (REPLACE). Used by the alarm and by "હમણાં બેકઅપ લો". */
    fun enqueueBackup(trigger: BackupTrigger) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, oneTimeRequest(trigger))
    }

    /** Idempotent (KEEP) periodic safety net. */
    fun ensureSafetyNet() {
        val req = PeriodicWorkRequestBuilder<NightlyBackupWorker>(SAFETY_PERIOD_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setInputData(workDataOf(BackupTrigger.KEY to BackupTrigger.SAFETY.name))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(UNIQUE_SAFETY_WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Alarm + safety net. Call at app start, on boot / time change and after saving backup settings. */
    fun ensureScheduled(): Scheduled {
        ensureSafetyNet()
        scheduleReminder()
        return scheduleNextAlarm()
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        REQUEST_CODE,
        Intent(ctx, BackupAlarmReceiver::class.java).setAction(BackupAlarmReceiver.ACTION_RUN),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val UNIQUE_WORK = "nightly_backup"
        const val UNIQUE_SAFETY_WORK = "nightly_backup_safety"
        const val TAG = "vk_backup"
        const val SAFETY_PERIOD_HOURS = 12L
        private const val REQUEST_CODE = 2345
        private const val REMIND_REQUEST_CODE = 2346
        val REMIND_AT: java.time.LocalTime = java.time.LocalTime.of(21, 30)

        fun oneTimeRequest(trigger: BackupTrigger): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<NightlyBackupWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.MINUTES)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(BackupTrigger.KEY to trigger.name))
                .addTag(TAG)
                .build()
    }
}
