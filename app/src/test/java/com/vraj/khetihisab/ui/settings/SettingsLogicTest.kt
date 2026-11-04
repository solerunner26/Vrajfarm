package com.vraj.khetihisab.ui.settings

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.TxnType
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
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsLogicTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun clockAndBackupLabels() {
        assertEquals("7:00 PM", SettingsFormat.clock12(19, 0))
        assertEquals("6:05 AM", SettingsFormat.clock12(6, 5))
        val today = LocalDate.of(2026, 11, 4)
        fun at(d: LocalDate, h: Int, m: Int) = LocalDateTime.of(d, java.time.LocalTime.of(h, m)).atZone(SettingsFormat.IST).toInstant().toEpochMilli()
        assertEquals("T · 11:45 PM", SettingsFormat.backupWhen(at(today, 23, 45), today, "T", "N"))
        assertEquals("N · 11:45 PM", SettingsFormat.backupWhen(at(today.plusDays(1), 23, 45), today, "T", "N"))
        assertEquals("02-11 · 11:45 PM", SettingsFormat.backupWhen(at(LocalDate.of(2026, 11, 2), 23, 45), today, "T", "N"))
    }

    @Test
    fun vighaParsingAndBounds() {
        assertEquals(7.5, SettingsFormat.parseVigha(" 7,5 ")!!, 0.0)
        assertNull(SettingsFormat.parseVigha(""))
        assertTrue(SettingsFormat.vighaValid(0.0))
        assertTrue(SettingsFormat.vighaValid(500.0))
        assertFalse(SettingsFormat.vighaValid(500.5))
        assertFalse(SettingsFormat.vighaValid(-1.0))
        assertEquals("12", SettingsFormat.vighaText(12.0))
        assertEquals("7.5", SettingsFormat.vighaText(7.5))
    }

    @Test
    fun animalSectionCategoriesAreGroupedAndInactiveLast() {
        val all = listOf(
            CategoryMasterEntity(1, "ખાણ-દાણ", AccountSection.ANIMAL, TxnType.EXPENSE, isActive = false),
            CategoryMasterEntity(2, "દવા", AccountSection.ANIMAL, TxnType.EXPENSE),
            CategoryMasterEntity(3, "દૂધ વેચાણ", AccountSection.ANIMAL, TxnType.INCOME),
            CategoryMasterEntity(4, "બિયારણ", AccountSection.FARM, TxnType.EXPENSE),
        )
        val g = MasterSettingsViewModel.groupCategories(all, AccountSection.ANIMAL)
        assertEquals(listOf(TxnType.EXPENSE, TxnType.INCOME), g.keys.toList())
        assertEquals(listOf(2L, 1L), g.getValue(TxnType.EXPENSE).map { it.id })
        assertEquals(listOf(3L), g.getValue(TxnType.INCOME).map { it.id })
        assertTrue(AccountSection.ANIMAL in MasterSettingsViewModel.SECTIONS)
        // Family offers ઉપાડ first and keeps empty offered groups (so "add" is always possible).
        assertEquals(listOf(TxnType.ADVANCE, TxnType.EXPENSE, TxnType.INCOME), MasterSettingsViewModel.groupCategories(all, AccountSection.FAMILY).keys.toList())
    }

    @Test
    fun pinChangeFlow() = runTest {
        var stored: String? = null
        val vm = PinChangeViewModel(
            verify = { p -> (if (p == "1234") PinCheck.OK else PinCheck.WRONG) to 0L },
            store = { stored = it },
            bioGet = { false },
            bioSet = {},
        )
        "9999".forEach(vm::onDigit)
        assertEquals(PinStep.CURRENT, vm.state.value.step)
        assertEquals(PinMessage.WRONG, vm.state.value.message)

        "1234".forEach(vm::onDigit)
        assertEquals(PinStep.NEW, vm.state.value.step)
        "5678".forEach(vm::onDigit)
        assertEquals(PinStep.CONFIRM, vm.state.value.step)
        "5679".forEach(vm::onDigit)
        assertEquals(PinStep.NEW, vm.state.value.step)
        assertEquals(PinMessage.MISMATCH, vm.state.value.message)

        "5678".forEach(vm::onDigit)
        vm.onDigit('5'); vm.onBackspace(); "5678".forEach(vm::onDigit)
        assertEquals(PinStep.DONE, vm.state.value.step)
        assertEquals("5678", stored)
    }

    @Test
    fun pinLockoutIsReported() = runTest {
        val vm = PinChangeViewModel(verify = { PinCheck.LOCKED to 28_000L }, store = {}, bioGet = { true }, bioSet = {}, io = UnconfinedTestDispatcher())
        "0000".forEach(vm::onDigit)
        assertEquals(PinMessage.LOCKED, vm.state.value.message)
        assertEquals("0:28", formatMmSs(vm.state.value.lockedMillis))
        assertTrue(vm.biometric.value)
    }
}
