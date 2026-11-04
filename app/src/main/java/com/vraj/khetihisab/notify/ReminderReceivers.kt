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

/** Shared by the notification actions, the widget DONE pill and the Reminders screen. */
object ReminderActions {
    suspend fun markDone(context: Context, id: Long) {
        NotifyGraph.repos(context).reminders.markDone(id)
        NotifyGraph.reminderScheduler(context).cancel(id)
        VkNotify.cancel(context, ReminderNotifier.notificationId(id))
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
            }
        }
    }

    companion object {
        private const val TAG = "ReminderAction"
        const val ACTION_DONE = "com.vraj.khetihisab.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.vraj.khetihisab.action.REMINDER_SNOOZE"
        const val EXTRA_ID = "id"
    }
}
