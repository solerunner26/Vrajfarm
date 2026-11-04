package com.vraj.khetihisab.ui.settings

import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.repo.PanchangPrefRepository
import com.vraj.khetihisab.data.seed.PanchangDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class PanchangGuidanceViewModelTest {

    private lateinit var dao: FakePanchangDao
    private lateinit var vm: PanchangGuidanceViewModel
    private var rescheduled = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = FakePanchangDao(PanchangDefaults.all(0L))
        vm = PanchangGuidanceViewModel(PanchangPrefRepository(dao, clock = { 42L }), reschedule = { rescheduled++ })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsStoredTextsOfSelectedType() = runTest {
        val d = vm.draft.value
        assertEquals(PanchangAlertType.AMAS, d.type)
        assertTrue(d.loaded)
        assertEquals(PanchangDefaults.doText(PanchangAlertType.AMAS), d.doText)
        assertTrue(d.isDefault)

        vm.select(PanchangAlertType.AGIYARAS)
        assertEquals(PanchangDefaults.avoidText(PanchangAlertType.AGIYARAS), vm.draft.value.avoidText)
    }

    @Test
    fun saveTrimsAndDeduplicatesLines() = runTest {
        vm.select(PanchangAlertType.POONAM)
        vm.onDoText("  વાવણી  \n\nવાવણી\nપૂજા ")
        vm.onAvoidText("ખરીદી ટાળો\n")
        vm.save()
        val stored = dao.prefsState.value.getValue(PanchangAlertType.POONAM)
        assertEquals("વાવણી\nપૂજા", stored.doText)
        assertEquals("ખરીદી ટાળો", stored.avoidText)
        assertEquals(42L, stored.updatedAt)
        assertTrue(vm.draft.value.saved)
        assertFalse(vm.draft.value.isDefault)
    }

    @Test
    fun resetRestoresDefaultsInRoomAndEditor() = runTest {
        vm.select(PanchangAlertType.AGIYARAS)
        vm.onDoText("મારી સલાહ")
        vm.onAvoidText("")
        vm.save()
        assertEquals("મારી સલાહ", dao.prefsState.value.getValue(PanchangAlertType.AGIYARAS).doText)

        vm.resetToDefault()

        val stored = dao.prefsState.value.getValue(PanchangAlertType.AGIYARAS)
        assertEquals(PanchangDefaults.doText(PanchangAlertType.AGIYARAS), stored.doText)
        assertEquals(PanchangDefaults.avoidText(PanchangAlertType.AGIYARAS), stored.avoidText)
        // On/off switches survive the reset.
        assertTrue(stored.enabled)
        val d = vm.draft.value
        assertEquals(stored.doText, d.doText)
        assertEquals(stored.avoidText, d.avoidText)
        assertTrue(d.isDefault)
    }

    @Test
    fun resetCreatesMissingRow() = runTest {
        dao.prefsState.value = emptyMap()
        vm.select(PanchangAlertType.BIJ)
        vm.resetToDefault()
        assertEquals(PanchangDefaults.doText(PanchangAlertType.BIJ), dao.prefsState.value.getValue(PanchangAlertType.BIJ).doText)
    }

    @Test
    fun festivalValidationAndSave() = runTest {
        assertEquals(FestivalError.NAME, vm.addFestival(FestivalInput(name = " ")))
        assertEquals(FestivalError.DATE, vm.addFestival(FestivalInput(name = "મેળો", byTithi = false, fixedDate = "02-29")))
        assertEquals(FestivalError.DATE, vm.addFestival(FestivalInput(name = "મેળો", byTithi = false, fixedDate = "31-04")))
        assertTrue(dao.festivalsState.value.isEmpty())

        assertNull(vm.addFestival(FestivalInput(name = " ગામનો મેળો ", byTithi = false, fixedDate = "3-5")))
        val fixed = dao.festivalsState.value.single()
        assertEquals("ગામનો મેળો", fixed.nameGu)
        assertEquals("03-05", fixed.fixedMonthDay)
        assertNull(fixed.monthIndex)

        assertNull(vm.addFestival(FestivalInput(name = "પાટોત્સવ", byTithi = true, monthIndex = 7, vad = true, tithi = 5)))
        val byTithi = dao.festivalsState.value.last()
        assertEquals(7, byTithi.monthIndex)
        assertEquals("VAD", byTithi.paksha)
        assertEquals(5, byTithi.tithiInPaksha)
        assertNull(byTithi.fixedMonthDay)
        assertEquals(2, rescheduled)

        vm.deleteFestival(byTithi.id)
        assertEquals(1, dao.festivalsState.value.size)
    }
}
