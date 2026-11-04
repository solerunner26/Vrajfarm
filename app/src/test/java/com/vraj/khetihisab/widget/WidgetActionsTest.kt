package com.vraj.khetihisab.widget

import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** doc 13: ToggleShoppingAction / MarkReminderDoneAction bodies with a fake source, plus widget content rules. */
class WidgetActionsTest {

    private class FakeSource : WidgetDataSource {
        val log = mutableListOf<String>()
        var shopping = listOf<ShoppingItemEntity>()
        var reminders = listOf<ReminderEntity>()
        override suspend fun pendingShopping() = shopping
        override suspend fun setBought(itemId: Long) {
            log += "bought:$itemId"
            shopping = shopping.map { if (it.id == itemId) it.copy(isBought = true) else it }
        }
        override suspend fun activeReminders() = reminders
        override suspend fun markReminderDone(reminderId: Long) {
            log += "done:$reminderId"
            reminders = reminders.map { if (it.id == reminderId) it.copy(isDone = true) else it }
        }
    }

    private fun item(id: Long, tab: ShopTab, bought: Boolean = false) =
        ShoppingItemEntity(id = id, tab = tab, name = "i$id", forDate = LocalDate.of(2026, 11, 4), farmId = null, estPricePaise = null, isBought = bought, createdAt = 0, updatedAt = 0)

    private fun rem(id: Long, p: Priority, due: Long) =
        ReminderEntity(id = id, title = "r$id", note = null, imagePath = null, priority = p, dueAt = due, createdAt = 0, updatedAt = 0)

    @Test fun toggleShopping_setsBoughtThenRefreshes() = runTest {
        val src = FakeSource().apply { shopping = listOf(item(1, ShopTab.FARM), item(2, ShopTab.FARM)) }
        WidgetActions.toggleShopping(src, 2) { src.log += "refresh" }
        assertEquals(listOf("bought:2", "refresh"), src.log)
        assertEquals(1, WidgetLogic.shoppingState(src.pendingShopping()).pendingCount)
    }

    @Test fun markReminderDone_archivesThenRefreshes_andRowLeavesWidget() = runTest {
        val src = FakeSource().apply { reminders = listOf(rem(1, Priority.HIGH, 10), rem(2, Priority.NORMAL, 5)) }
        WidgetActions.markReminderDone(src, 1) { src.log += "refresh" }
        assertEquals(listOf("done:1", "refresh"), src.log)
        assertEquals(listOf(2L), WidgetLogic.reminderRows(src.activeReminders()).map { it.id })
    }

    @Test fun reminderRows_highFirstThenEarliest_cappedAtThree() {
        val rows = WidgetLogic.reminderRows(
            listOf(rem(1, Priority.NORMAL, 1), rem(2, Priority.HIGH, 30), rem(3, Priority.HIGH, 20), rem(4, Priority.NORMAL, 2)),
        )
        assertEquals(listOf(3L, 2L, 1L), rows.map { it.id })
    }

    @Test fun shoppingState_farmFirst_excludesBought_capsAtFive() {
        val items = listOf(item(1, ShopTab.PERSONAL), item(2, ShopTab.FARM, bought = true)) + (3L..8L).map { item(it, ShopTab.FARM) }
        val s = WidgetLogic.shoppingState(items)
        assertEquals(7, s.pendingCount)
        assertEquals(listOf(3L, 4L, 5L, 6L, 7L), s.rows.map { it.id })
    }
}
