package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.panchang.PanchangAlertEngine
import com.vraj.khetihisab.panchang.PanchangAlertType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** November 2026 month grid against the real doc 10 engine (Diwali month). */
class PanchangCalendarModelTest {

    private val nov = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 4)
    private val month = PanchangCalendarModel.build(nov, today)

    @Test
    fun gridStartsOnSundayAndHasFiveWeeks() {
        // 01-11-2026 is a Sunday → no leading blanks; 30 days → 5 rows of 7.
        assertEquals(0, month.leadingBlanks)
        assertEquals(30, month.cells.size)
        assertEquals(5, month.weeks.size)
        assertTrue(month.weeks.all { it.size == 7 })
        assertEquals(LocalDate.of(2026, 11, 30), month.weeks.last()[1]!!.date)
        assertEquals(5, month.weeks.last().count { it == null })
        assertEquals(listOf(today), month.cells.filter { it.isToday }.map { it.date })
    }

    @Test
    fun everyCellHasTithiAndMarkersMatchTheEngine() {
        month.cells.forEach { c ->
            assertNotNull("tithi for ${c.date}", c.shortTithi)
            val engine = PanchangAlertEngine.special(c.date)
            assertEquals("special ${c.date}", engine?.types, c.special?.types)
            assertEquals(c.isSpecial, c.markers.isNotEmpty())
        }
        assertEquals(month.cells.mapNotNull { it.special }.map { it.date }, month.specialDays.map { it.date })
    }

    @Test
    fun novemberHasOneAmasOnePoonamTwoAgiyarasAndDiwali() {
        fun daysWith(m: DayMarker) = month.cells.filter { m in it.markers }.map { it.date.dayOfMonth }
        // Engine (HANDOVER "Foundation notes"): new moon 09-11-2026 → amas on the 9th by the sunrise rule.
        assertEquals(listOf(9), daysWith(DayMarker.AMAS))
        assertEquals(1, daysWith(DayMarker.POONAM).size)
        assertTrue(daysWith(DayMarker.POONAM).single() in 23..25)
        assertEquals(2, daysWith(DayMarker.AGIYARAS).size)
        // Sud Bij only (Vad Bij is off by default): Bhai Bij after Bestu Varas.
        val bij = daysWith(DayMarker.BIJ)
        assertEquals(1, bij.size)
        val bhaiBij = month.cells.first { it.date.dayOfMonth == bij.single() }.special!!
        assertTrue(bhaiBij.festivals.contains("ભાઈબીજ"))
        assertEquals(listOf(DayMarker.FESTIVAL, DayMarker.BIJ), bhaiBij.let(PanchangCalendarModel::markersOf))

        val diwali = month.specialDays.single { "દિવાળી" in it.festivals }
        assertTrue(diwali.date.dayOfMonth in 7..9)
        assertTrue(month.cells.any { it.special?.festivals?.contains("દેવ દિવાળી") == true })
        // Short tithi labels.
        val amasCell = month.cells.first { it.date.dayOfMonth == 9 }
        assertEquals(ShortTithi.Kind.AMAS, amasCell.shortTithi!!.kind)
    }

    @Test
    fun subtitleShowsBothLunarMonthsAndVsYears() {
        val s = month.lunarSubtitle
        assertTrue(s, s.startsWith("આસો / કારતક"))
        assertTrue(s, s.endsWith("વિ.સં. 2082–83"))
    }

    @Test
    fun vadBijAndCustomFixedFestivalAddMarkers() {
        val custom = listOf(CustomFestivalEntity(id = 1, nameGu = "ગામનો મેળો", monthIndex = null, paksha = null, tithiInPaksha = null, fixedMonthDay = "11-15", note = null))
        val withExtras = PanchangCalendarModel.build(nov, today, custom, includeVadBij = true)
        val fair = withExtras.cells.first { it.date.dayOfMonth == 15 }
        assertTrue(DayMarker.FESTIVAL in fair.markers)
        assertTrue(fair.special!!.festivals.contains("ગામનો મેળો"))
        val bijDays = withExtras.cells.count { DayMarker.BIJ in it.markers }
        assertTrue("sud + vad bij", bijDays >= 2)
        assertFalse(month.cells.first { it.date.dayOfMonth == 15 }.markers.contains(DayMarker.FESTIVAL))
    }

    @Test
    fun listTitleAddsTithiTypeToFestival() {
        val diwali = month.specialDays.first { "દિવાળી" in it.festivals }
        val label = PanchangCalendarModel.listTitle(diwali) { it.name }
        if (PanchangAlertType.AMAS in diwali.types) assertEquals("દિવાળી (AMAS)", label) else assertEquals("દિવાળી", label)
        assertEquals(0, PanchangCalendarModel.column(LocalDate.of(2026, 11, 1)))
        assertEquals(6, PanchangCalendarModel.column(LocalDate.of(2026, 11, 7)))
        assertEquals(DayPeriod.MORNING, PanchangCalendarModel.periodOfDay(4))
        assertEquals(DayPeriod.NIGHT, PanchangCalendarModel.periodOfDay(23))
    }
}
