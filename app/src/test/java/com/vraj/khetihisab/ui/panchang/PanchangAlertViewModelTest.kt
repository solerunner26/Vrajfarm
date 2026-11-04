package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.seed.PanchangDefaults
import com.vraj.khetihisab.panchang.AlertPurpose
import com.vraj.khetihisab.panchang.PanchangAlertEngine
import com.vraj.khetihisab.panchang.toPrefType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** doc 10 test: dialog shows → tap સમજાયું → re-evaluate → no dialog the same day. */
@OptIn(ExperimentalCoroutinesApi::class)
class PanchangAlertViewModelTest {

    private val amasDay = LocalDate.of(2026, 11, 9)
    private val dismissed = mutableSetOf<LocalDate>()
    private val reminders = mutableListOf<Triple<LocalDate, String, String>>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(day: LocalDate = amasDay) = PanchangAlertViewModel(
        loadAlert = { d ->
            PanchangAlertEngine.alertFor(d, PanchangDefaults.all(0L).associateBy { it.type }, emptyList(), true, purpose = AlertPurpose.DAY_DIALOG)
        },
        isDismissed = { it in dismissed },
        markDismissed = { dismissed += it },
        createReminder = { d, t, n -> reminders += Triple(d, t, n); ReminderOutcome.CREATED },
        today = { day },
    )

    @Test
    fun showsOnceThenStaysDismissed() = runTest {
        val v = vm()
        assertNotNull(v.alert.value)
        v.dismiss()
        assertNull(v.alert.value)
        assertEquals(setOf(amasDay), dismissed)
        v.refresh()
        assertNull(v.alert.value)
        assertNull(vm().alert.value)
    }

    @Test
    fun reminderActionCreatesAndDismisses() = runTest {
        val v = vm()
        val title = v.alert.value!!.special.titleGu
        v.makeReminder()
        assertEquals(amasDay, reminders.single().first)
        assertEquals(title, reminders.single().second)
        assertEquals(setOf(amasDay), dismissed)
        assertNull(v.alert.value)
    }

    @Test
    fun openCalendarReturnsTheDay() = runTest {
        val v = vm()
        assertEquals(amasDay, v.openCalendar())
        assertNull(v.alert.value)
        assertEquals(setOf(amasDay), dismissed)
    }

    @Test
    fun ordinaryDayHasNoDialogAndPrefTypesMap() = runTest {
        // 12-11-2026 is an ordinary sud day per the engine (after Bhai Bij on the 11th).
        val ordinary = (12..20).map { LocalDate.of(2026, 11, it) }.first { PanchangAlertEngine.special(it) == null }
        assertNull(vm(ordinary).alert.value)
        assertEquals(com.vraj.khetihisab.data.entity.PanchangAlertType.AMAS, com.vraj.khetihisab.panchang.PanchangAlertType.AMAS.toPrefType())
    }
}
