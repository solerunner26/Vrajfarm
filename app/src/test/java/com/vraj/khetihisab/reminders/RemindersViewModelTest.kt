package com.vraj.khetihisab.reminders

import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ReminderSource
import com.vraj.khetihisab.notify.Deadline
import com.vraj.khetihisab.notify.ReminderTiming
import com.vraj.khetihisab.ui.reminders.ReminderOrigin
import com.vraj.khetihisab.ui.reminders.ReminderStore
import com.vraj.khetihisab.ui.reminders.ReminderTab
import com.vraj.khetihisab.ui.reminders.ReminderTag
import com.vraj.khetihisab.ui.reminders.RemindersEvent
import com.vraj.khetihisab.ui.reminders.RemindersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    /** 04-11-2026 10:00 IST. */
    private val now = ReminderTiming.istMillis(LocalDateTime.of(2026, 11, 4, 10, 0))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeStore(initial: List<ReminderEntity>, private val now: Long) : ReminderStore {
        val rows = MutableStateFlow(initial)
        val calls = mutableListOf<String>()
        val deletedPhotos = mutableListOf<String>()
        var exact = true
        private var nextId = 100L

        override fun active(): Flow<List<ReminderEntity>> = rows.map { l -> l.filter { !it.isDone && !it.isDeleted } }
        override fun archived(): Flow<List<ReminderEntity>> = rows.map { l -> l.filter { it.isDone && !it.isDeleted } }
        override suspend fun byId(id: Long) = rows.value.firstOrNull { it.id == id }

        override suspend fun save(r: ReminderEntity): Long {
            val id = if (r.id == 0L) nextId++ else r.id
            rows.value = rows.value.filter { it.id != id } + r.copy(id = id)
            calls += "save:$id"
            return id
        }

        override suspend fun markDone(id: Long) {
            calls += "done:$id"
            rows.value = rows.value.map { if (it.id == id) it.copy(isDone = true, doneAt = now) else it }
        }

        override suspend fun reopen(id: Long) {
            calls += "reopen:$id"
            rows.value = rows.value.map { if (it.id == id) it.copy(isDone = false, doneAt = null) else it }
        }

        override suspend fun delete(id: Long) {
            calls += "delete:$id"
            rows.value = rows.value.map { if (it.id == id) it.copy(isDeleted = true) else it }
        }

        override fun canScheduleExact() = exact
        override fun deletePhoto(path: String) {
            deletedPhotos += path
        }
    }

    private fun rem(id: Long, title: String, priority: Priority = Priority.NORMAL, dueAt: Long = now + 86_400_000L, source: String? = ReminderSource.MANUAL, img: String? = null, done: Boolean = false) =
        ReminderEntity(id = id, title = title, note = null, imagePath = img, priority = priority, dueAt = dueAt, isDone = done, doneAt = if (done) now - 1 else null, sourceType = source, createdAt = 1, updatedAt = 1)

    private fun TestScope.vmWith(store: FakeStore, needsPermission: Boolean = false): Pair<RemindersViewModel, MutableList<RemindersEvent>> {
        val vm = RemindersViewModel(store, clock = { now }, tickMillis = 0, notificationPermissionNeeded = { needsPermission })
        val events = mutableListOf<RemindersEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.eventFlow.collect { events += it } }
        return vm to events
    }

    @Test fun done_movesCardToArchive_andUndoBringsItBack() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(1, "લાઇટ બિલ ભરવાનું", Priority.HIGH), rem(2, "CCTV સર્વિસ")), now)
        val (vm, events) = vmWith(store)
        assertEquals(listOf(1L, 2L), vm.state.value.active.map { it.id })
        assertTrue(vm.state.value.archived.isEmpty())

        vm.markDone(1, "લાઇટ બિલ ભરવાનું")
        assertEquals(listOf(2L), vm.state.value.active.map { it.id })
        assertEquals(listOf(1L), vm.state.value.archived.map { it.id })
        assertTrue(vm.state.value.archived.single().archived)
        assertEquals(RemindersEvent.Done(1, "લાઇટ બિલ ભરવાનું"), events.single())

        vm.undoDone(1)
        assertEquals(setOf(1L, 2L), vm.state.value.active.map { it.id }.toSet())
        assertTrue(vm.state.value.archived.isEmpty())
        assertEquals(listOf("done:1", "reopen:1"), store.calls)
    }

    @Test fun reopenFromArchive_emitsReopened() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(5, "મગફળી APMC વેચાણ", done = true)), now)
        val (vm, events) = vmWith(store)
        vm.selectTab(ReminderTab.ARCHIVE)
        assertEquals(listOf(5L), vm.state.value.visible.map { it.id })
        vm.reopen(5)
        assertTrue(vm.state.value.visible.isEmpty())
        assertEquals(listOf(5L), vm.state.value.active.map { it.id })
        assertEquals(RemindersEvent.Reopened, events.single())
    }

    @Test fun tabs_switchVisibleList() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(1, "a"), rem(2, "b", done = true)), now)
        val (vm, _) = vmWith(store)
        assertEquals(listOf(1L), vm.state.value.visible.map { it.id })
        vm.selectTab(ReminderTab.ARCHIVE)
        assertEquals(listOf(2L), vm.state.value.visible.map { it.id })
    }

    @Test fun tagsAndOrigins() = runTest(dispatcher) {
        val store = FakeStore(
            listOf(
                rem(1, "high", Priority.HIGH),
                rem(2, "normal"),
                rem(3, "ચોપડા પૂજન", source = ReminderSource.PANCHANG),
                rem(4, "રોટાવેટર", Priority.HIGH, dueAt = now - 1000, source = ReminderSource.LOAN),
            ),
            now,
        )
        val (vm, _) = vmWith(store)
        val byId = vm.state.value.active.associateBy { it.id }
        assertEquals(ReminderTag.HIGH, byId.getValue(1).tag)
        assertEquals(ReminderTag.NORMAL, byId.getValue(2).tag)
        assertEquals(ReminderTag.PANCHANG, byId.getValue(3).tag)
        assertEquals(ReminderOrigin.LOAN, byId.getValue(4).origin)
        assertNull(byId.getValue(1).origin)
        assertTrue(byId.getValue(4).deadline is Deadline.Overdue)
        assertTrue(byId.getValue(1).deadline is Deadline.Tomorrow)
    }

    @Test fun saveNew_buildsManualEntityWithIstDueTime_andClosesSheet() = runTest(dispatcher) {
        val store = FakeStore(emptyList(), now)
        val (vm, events) = vmWith(store, needsPermission = true)
        vm.openNew()
        val d = vm.state.value.editor!!
        assertTrue(d.isNew)
        assertEquals(LocalDate.of(2026, 11, 5), d.date)
        assertEquals(LocalTime.of(9, 0), d.time)

        vm.onTitle("  લાઇટ બિલ ભરવાનું ")
        vm.onNote("PGVCL")
        vm.onPriority(Priority.HIGH)
        vm.onTime(LocalTime.of(17, 0))
        vm.save()

        assertNull(vm.state.value.editor)
        val saved = store.rows.value.single()
        assertEquals("લાઇટ બિલ ભરવાનું", saved.title)
        assertEquals("PGVCL", saved.note)
        assertEquals(Priority.HIGH, saved.priority)
        assertEquals(ReminderSource.MANUAL, saved.sourceType)
        assertEquals(ReminderTiming.istMillis(LocalDateTime.of(2026, 11, 5, 17, 0)), saved.dueAt)
        assertEquals(listOf(RemindersEvent.AskNotificationPermission, RemindersEvent.Saved), events)
    }

    @Test fun saveBlankTitle_showsErrorAndSavesNothing() = runTest(dispatcher) {
        val store = FakeStore(emptyList(), now)
        val (vm, _) = vmWith(store)
        vm.openNew()
        vm.onTitle("   ")
        vm.save()
        assertTrue(vm.state.value.editor!!.titleError)
        assertTrue(store.calls.isEmpty())
        vm.onTitle("x")
        assertFalse(vm.state.value.editor!!.titleError)
    }

    @Test fun edit_keepsIdAndSource_andDeletesReplacedPhotoAfterSave() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(7, "ઘઉં વાવણી", source = ReminderSource.LOAN, img = "/f/reminder_photos/old.webp")), now)
        val (vm, _) = vmWith(store)
        vm.openEdit(7)
        val d = vm.state.value.editor!!
        assertFalse(d.isNew)
        assertEquals("ઘઉં વાવણી", d.title)
        vm.onPhoto("/f/reminder_photos/new.webp")
        vm.save()
        val saved = store.rows.value.single()
        assertEquals(7L, saved.id)
        assertEquals(ReminderSource.LOAN, saved.sourceType)
        assertEquals("/f/reminder_photos/new.webp", saved.imagePath)
        assertEquals(listOf("/f/reminder_photos/old.webp"), store.deletedPhotos)
        assertEquals(listOf("save:7"), store.calls)
    }

    @Test fun dismiss_deletesUnsavedPhotosOnly() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(7, "x", img = "/f/old.webp")), now)
        val (vm, _) = vmWith(store)
        vm.openEdit(7)
        vm.onPhoto("/f/a.webp")
        vm.onPhoto("/f/b.webp") // replaces a → a is dropped right away
        assertEquals(listOf("/f/a.webp"), store.deletedPhotos)
        vm.dismissEditor()
        assertEquals(listOf("/f/a.webp", "/f/b.webp"), store.deletedPhotos)
        assertNull(vm.state.value.editor)
        assertEquals("/f/old.webp", store.rows.value.single().imagePath)
    }

    @Test fun delete_softDeletesAndLeavesBothLists() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(3, "x")), now)
        val (vm, events) = vmWith(store)
        vm.openEdit(3)
        vm.delete()
        assertTrue(vm.state.value.active.isEmpty())
        assertTrue(vm.state.value.archived.isEmpty())
        assertEquals(listOf("delete:3"), store.calls)
        assertEquals(RemindersEvent.Deleted, events.single())
    }

    @Test fun openEdit_ignoresArchived() = runTest(dispatcher) {
        val store = FakeStore(listOf(rem(3, "x", done = true)), now)
        val (vm, _) = vmWith(store)
        vm.openEdit(3)
        assertNull(vm.state.value.editor)
    }

    @Test fun voiceAppendsToTitle() = runTest(dispatcher) {
        val (vm, _) = vmWith(FakeStore(emptyList(), now))
        vm.openNew()
        vm.onTitle("લાઇટ")
        vm.onVoiceText(" બિલ ભરવાનું ")
        assertEquals("લાઇટ બિલ ભરવાનું", vm.state.value.editor!!.title)
    }

    @Test fun exactAllowedRefreshes() = runTest(dispatcher) {
        val store = FakeStore(emptyList(), now)
        val (vm, _) = vmWith(store)
        assertTrue(vm.state.value.exactAllowed)
        store.exact = false
        vm.refreshExactAllowed()
        assertFalse(vm.state.value.exactAllowed)
    }
}
