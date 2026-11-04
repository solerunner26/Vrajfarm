package com.vraj.khetihisab.widget

import android.content.Context
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.notify.ReminderActions

/** What the widgets read and write. Faked in unit tests; never exposes money (doc 13). */
interface WidgetDataSource {
    suspend fun pendingShopping(): List<ShoppingItemEntity>
    suspend fun setBought(itemId: Long)
    suspend fun activeReminders(): List<ReminderEntity>
    suspend fun markReminderDone(reminderId: Long)
}

/** Real source on the app repositories. */
class RepoWidgetDataSource(private val context: Context) : WidgetDataSource {
    private val repos get() = NotifyGraph.repos(context)
    override suspend fun pendingShopping() = repos.shopping.activeNow()
    override suspend fun setBought(itemId: Long) = repos.shopping.setBought(itemId, true)
    override suspend fun activeReminders() = repos.reminders.activeNow()

    /** Also cancels the alarm + notification (same path as the notification DONE action). */
    override suspend fun markReminderDone(reminderId: Long) = ReminderActions.markDone(context, reminderId)
}

object WidgetGraph {
    /** Test hook. */
    @Volatile var sourceOverride: WidgetDataSource? = null

    fun source(context: Context): WidgetDataSource = sourceOverride ?: RepoWidgetDataSource(context.applicationContext)
}

data class ShopRow(val id: Long, val name: String)
data class ShoppingWidgetState(val pendingCount: Int, val rows: List<ShopRow>)

data class ReminderRow(val id: Long, val title: String, val priority: Priority, val dueAt: Long)

/** Pure widget content rules (doc 13 widget table). */
object WidgetLogic {
    const val SHOP_MAX = 5
    const val REMINDER_MAX = 3

    /** Up to [max] pending items, Farm tab first (each tab keeps the DAO order: newest date first). */
    fun shoppingState(items: List<ShoppingItemEntity>, max: Int = SHOP_MAX): ShoppingWidgetState {
        val pending = items.filter { !it.isBought && !it.isDeleted }
        val ordered = pending.filter { it.tab == ShopTab.FARM } + pending.filter { it.tab != ShopTab.FARM }
        return ShoppingWidgetState(pending.size, ordered.take(max).map { ShopRow(it.id, it.name) })
    }

    /** Up to [max]: HIGH first, then NORMAL; earliest due first within each. */
    fun reminderRows(items: List<ReminderEntity>, max: Int = REMINDER_MAX): List<ReminderRow> =
        items.asSequence()
            .filter { !it.isDone && !it.isDeleted }
            .sortedWith(compareBy<ReminderEntity> { if (it.priority == Priority.HIGH) 0 else 1 }.thenBy { it.dueAt })
            .take(max)
            .map { ReminderRow(it.id, it.title, it.priority, it.dueAt) }
            .toList()
}

/** Widget action bodies, separated from Glance so they can be unit-tested with a fake source. */
object WidgetActions {
    suspend fun toggleShopping(source: WidgetDataSource, itemId: Long, refresh: suspend () -> Unit) {
        source.setBought(itemId)
        refresh()
    }

    suspend fun markReminderDone(source: WidgetDataSource, reminderId: Long, refresh: suspend () -> Unit) {
        source.markReminderDone(reminderId)
        refresh()
    }
}
