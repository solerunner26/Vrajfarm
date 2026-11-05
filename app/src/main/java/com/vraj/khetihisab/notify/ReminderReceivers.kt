package com.vraj.khetihisab.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.widget.WidgetRefresher

/** Builds and posts reminder notifications (doc 13). */
object ReminderNotifier {
    private const val BASE_ID = 100_000

    fun notificationId(reminderId: Long): Int = BASE_ID + (reminderId % 1_000_000).toInt()

    suspend fun show(context: Context, r: ReminderEntity, now: Long = System.currentTimeMillis()): Boolean {
        if (r.sourceType == com.vraj.khetihisab.data.entity.ReminderSource.LOAN && r.sourceId != null) {
            showLoan(context, r, now)?.let { return it }
        }
        val deadline = Deadline.of(r.dueAt, now).label { res, arg -> context.getString(res, arg) }
        val text = listOfNotNull(deadline, r.note?.takeIf { it.isNotBlank() }).joinToString(" · ")
        val done = VkNotify.broadcast(context, ReminderActionReceiver::class.java, ReminderActionReceiver.ACTION_DONE, "vraj-action://reminder/done/${r.id}") {
            putExtra(ReminderActionReceiver.EXTRA_ID, r.id)
        }
        val snooze = VkNotify.broadcast(context, ReminderActionReceiver::class.java, ReminderActionReceiver.ACTION_SNOOZE, "vraj-action://reminder/snooze/${r.id}") {
            putExtra(ReminderActionReceiver.EXTRA_ID, r.id)
        }
        val b = VkNotify.builder(context, NotificationChannels.REMINDERS, VkNotify.accent(context))
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setWhen(r.dueAt)
            .setShowWhen(true)
            .setContentIntent(VkDeepLinks.pending(context, VkDeepLinks.REMINDERS))
            .addAction(0, context.getString(R.string.nt_done), done)
            .addAction(0, context.getString(R.string.rem_snooze), snooze)
            .setPriority(if (r.priority == Priority.HIGH) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (r.priority == Priority.HIGH) b.setCategory(NotificationCompat.CATEGORY_REMINDER)
        return VkNotify.post(context, notificationId(r.id), b.build())
    }
}

/**
 * Borrowed-asset nudge (owner request 2026-11-05):
 * "રમેશભાઈ પાસે આપણું પાવડો છે · 10 દિવસ માટે લઈ ગયા છે" with
 * [પરત આવી ગયું] [1 અઠવાડિયા પછી] [1 મહિના પછી]. If ignored it comes back every week.
 */
private suspend fun showLoan(context: Context, r: ReminderEntity, now: Long): Boolean? {
    val repos = NotifyGraph.repos(context)
    val lw = repos.assets.loanById(r.sourceId ?: return null) ?: return null
    if (lw.loan.returnedAt != null) return null
    val days = java.time.temporal.ChronoUnit.DAYS.between(lw.loan.handedOverDate, lw.loan.expectedReturnDate).coerceAtLeast(1)
    val title = context.getString(R.string.v3_loan_nt_title, lw.loan.borrowerName, lw.asset.name)
    val text = context.getString(
        R.string.v3_loan_nt_body,
        lw.loan.handedOverDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")),
        days.toInt(),
        lw.loan.expectedReturnDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")),
    )
    fun action(a: String) = VkNotify.broadcast(context, ReminderActionReceiver::class.java, a, "vraj-action://loan/$a/${r.id}") {
        putExtra(ReminderActionReceiver.EXTRA_ID, r.id)
        putExtra(ReminderActionReceiver.EXTRA_LOAN, lw.loan.id)
    }
    val b = VkNotify.builder(context, NotificationChannels.REMINDERS, VkNotify.accent(context))
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(VkDeepLinks.pending(context, VkDeepLinks.REMINDERS))
        .addAction(0, context.getString(R.string.v3_loan_returned), action(ReminderActionReceiver.ACTION_LOAN_RETURNED))
        .addAction(0, context.getString(R.string.v3_loan_week), action(ReminderActionReceiver.ACTION_LOAN_WEEK))
        .addAction(0, context.getString(R.string.v3_loan_month), action(ReminderActionReceiver.ACTION_LOAN_MONTH))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
    val posted = VkNotify.post(context, ReminderNotifier.notificationId(r.id), b.build())
    // Ignored? It comes back in a week.
    ReminderActions.loanLater(context, r.id, days = 7, cancelShown = false, now = now)
    return posted
}

/** Shared by the notification actions, the widget DONE pill and the Reminders screen. */
object ReminderActions {
    suspend fun markDone(context: Context, id: Long) {
        NotifyGraph.repos(context).reminders.markDone(id)
        NotifyGraph.reminderScheduler(context).cancel(id)
        VkNotify.cancel(context, ReminderNotifier.notificationId(id))
        WidgetRefresher.get(context).refreshNow()
    }

    /** Undo of DONE / "ફરી ચાલુ કરો" (Reminders screen): back to active, alarms re-armed. */
    suspend fun reopen(context: Context, id: Long) {
        val repo = NotifyGraph.repos(context).reminders
        repo.markUndone(id)
        repo.byId(id)?.let { NotifyGraph.reminderScheduler(context).schedule(it) }
        WidgetRefresher.get(context).refreshNow()
    }

    /**
     * Add/edit sheet saved: drops every old alarm of [r] (including a snooze) and its shown
     * notification, then arms the new ones. A due time in the past arms nothing (doc 13 edge case).
     */
    fun rearm(context: Context, r: ReminderEntity): List<ReminderScheduler.Scheduled> {
        val scheduler = NotifyGraph.reminderScheduler(context)
        scheduler.cancel(r.id)
        VkNotify.cancel(context, ReminderNotifier.notificationId(r.id))
        return scheduler.schedule(r)
    }

    /** Editor "કાઢી નાખો": soft delete + cancel alarms and notification. */
    suspend fun delete(context: Context, id: Long) {
        NotifyGraph.repos(context).reminders.softDelete(id)
        NotifyGraph.reminderScheduler(context).cancel(id)
        VkNotify.cancel(context, ReminderNotifier.notificationId(id))
        WidgetRefresher.get(context).refreshNow()
    }

    /** Loan nudge "1 અઠવાડિયા / 1 મહિના પછી": moves the reminder [days] ahead and re-arms it. */
    suspend fun loanLater(context: Context, id: Long, days: Long, cancelShown: Boolean = true, now: Long = System.currentTimeMillis()) {
        val repo = NotifyGraph.repos(context).reminders
        val r = repo.byId(id) ?: return
        val next = r.copy(dueAt = now + days * 24 * 60 * 60 * 1000L, updatedAt = now)
        repo.update(next)
        val scheduler = NotifyGraph.reminderScheduler(context)
        scheduler.cancel(id)
        scheduler.schedule(next)
        if (cancelShown) VkNotify.cancel(context, ReminderNotifier.notificationId(id))
    }

    /** "પરત આવી ગયું": closes the loan (and its reminder) from the notification. */
    suspend fun loanReturned(context: Context, reminderId: Long, loanId: Long) {
        NotifyGraph.repos(context).assets.markReturned(loanId)
        NotifyGraph.reminderScheduler(context).cancel(reminderId)
        VkNotify.cancel(context, ReminderNotifier.notificationId(reminderId))
        WidgetRefresher.get(context).refreshNow()
    }

    suspend fun snooze(context: Context, id: Long) {
        NotifyGraph.reminderScheduler(context).snooze(id)
        VkNotify.cancel(context, ReminderNotifier.notificationId(id))
    }
}

/** Alarm fired: post the notification unless the reminder is gone or done. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AndroidAlarmPort.EXTRA_ID, -1L)
        if (id < 0) return
        val app = context.applicationContext
        launchAsync(TAG) {
            val r = NotifyGraph.repos(app).reminders.byId(id) ?: return@launchAsync
            if (r.isDone || r.isDeleted) return@launchAsync
            ReminderNotifier.show(app, r)
        }
    }

    private companion object {
        const val TAG = "ReminderReceiver"
    }
}

/** "પૂરું થયું" and "1 કલાક પછી" notification actions. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val app = context.applicationContext
        launchAsync(TAG) {
            when (intent.action) {
                ACTION_DONE -> ReminderActions.markDone(app, id)
                ACTION_SNOOZE -> ReminderActions.snooze(app, id)
                ACTION_LOAN_WEEK -> ReminderActions.loanLater(app, id, days = 7)
                ACTION_LOAN_MONTH -> ReminderActions.loanLater(app, id, days = 30)
                ACTION_LOAN_RETURNED -> {
                    val loan = intent.getLongExtra(EXTRA_LOAN, -1L)
                    if (loan > 0) ReminderActions.loanReturned(app, id, loan)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ReminderAction"
        const val ACTION_DONE = "com.vraj.khetihisab.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.vraj.khetihisab.action.REMINDER_SNOOZE"
        const val EXTRA_ID = "id"
        const val EXTRA_LOAN = "loan"
        const val ACTION_LOAN_WEEK = "com.vraj.khetihisab.action.LOAN_WEEK"
        const val ACTION_LOAN_MONTH = "com.vraj.khetihisab.action.LOAN_MONTH"
        const val ACTION_LOAN_RETURNED = "com.vraj.khetihisab.action.LOAN_RETURNED"
    }
}
