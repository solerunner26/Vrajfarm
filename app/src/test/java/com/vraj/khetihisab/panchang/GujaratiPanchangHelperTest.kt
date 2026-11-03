package com.vraj.khetihisab.panchang

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * doc 02 test vectors + engine checks.
 *
 * DISCREPANCY (flagged, not faked): three vectors in doc 02 assume Diwali-amas falls on 08-11 at
 * sunrise. The engine follows the doc's own Udaya-tithi rule (§5.3.1) and the astronomy puts the
 * 2026-11-09 new moon at 07:02 UTC = 12:32 IST (verified to the minute against the independent
 * eclipse/new-moon checks in [AstroTest]). So at sunrise:
 *   08-11 → આસો વદ ચૌદશ (amas starts 11:28 IST; Diwali is observed that evening by the pradosh rule, doc 10)
 *   09-11 → આસો વદ અમાસ, VS 2082 (amas until 12:32 IST)
 *   10-11 → કારતક સુદ એકમ, VS 2083 (Bestu Varas by Udaya tithi)
 *   11-11 → કારતક સુદ બીજ (Bhai Bij)
 * The doc vectors are kept below as @Ignore'd tests so the mismatch stays visible; the
 * engine-derived values are asserted in `correctedVectors_*`. Both must be checked against a
 * printed Gujarati panchang (Sandesh / Mahuva jantri) before release, as doc 02 requires.
 */
class GujaratiPanchangHelperTest {

    private fun d(s: String): LocalDate = LocalDate.of(s.substring(6, 10).toInt(), s.substring(3, 5).toInt(), s.substring(0, 2).toInt())

    // ---- doc 02 vectors that pass ----

    @Test
    fun vector_03_11_2026_tuesday_aso_vad_nom_vs2082() {
        val p = GujaratiPanchangHelper.day(d("03-11-2026"))
        assertEquals("મંગળવાર", p.weekdayGu)
        assertEquals("આસો વદ નોમ", p.tithiGu)
        assertEquals(2082, p.vikramSamvat)
        assertEquals(6, p.monthIndex)
        assertEquals(Paksha.VAD, p.paksha)
        assertEquals(9, p.tithiInPaksha)
        assertEquals(24, p.tithiIndex)
        assertEquals("આસો વદ નોમ", GujaratiPanchangHelper.shortLabel(d("03-11-2026")))
    }

    @Test
    fun vector_05_11_2026_aso_vad_agiyaras() {
        assertEquals("આસો વદ અગિયારસ", GujaratiPanchangHelper.day(d("05-11-2026")).tithiGu)
    }

    // ---- doc 02 vectors that do NOT match the Udaya-tithi computation (see class doc) ----

    @Ignore("doc 02 vector conflicts with Udaya tithi: sunrise tithi on 08-11-2026 is વદ ચૌદશ; amas starts 11:28 IST. Verify with printed panchang.")
    @Test
    fun docVector_08_11_2026_amas() {
        val p = GujaratiPanchangHelper.day(d("08-11-2026"))
        assertEquals("આસો વદ અમાસ", p.tithiGu)
        assertEquals(2082, p.vikramSamvat)
    }

    @Ignore("doc 02 vector conflicts with Udaya tithi: amas is in force at sunrise on 09-11-2026 (ends 12:32 IST). Verify with printed panchang.")
    @Test
    fun docVector_09_11_2026_kartak_sud_ekam_vs2083() {
        val p = GujaratiPanchangHelper.day(d("09-11-2026"))
        assertEquals("કારતક સુદ એકમ", p.tithiGu)
        assertEquals(2083, p.vikramSamvat)
    }

    @Ignore("doc 02 vector conflicts with Udaya tithi: sunrise tithi on 10-11-2026 is કારતક સુદ એકમ. Verify with printed panchang.")
    @Test
    fun docVector_10_11_2026_kartak_sud_bij() {
        assertEquals("કારતક સુદ બીજ", GujaratiPanchangHelper.day(d("10-11-2026")).tithiGu)
    }

    // ---- engine-derived values for the Diwali week (pending printed-panchang check) ----

    @Test
    fun correctedVectors_diwaliWeek2026() {
        val p8 = GujaratiPanchangHelper.day(d("08-11-2026"))
        assertEquals("આસો વદ ચૌદશ", p8.tithiGu)
        // Amas begins on the 8th (late morning), so it is in force at pradosh that evening (Diwali).
        assertEquals(30, Astro.tithiAt(p8.sunset.toInstant()))

        val p9 = GujaratiPanchangHelper.day(d("09-11-2026"))
        assertEquals("આસો વદ અમાસ", p9.tithiGu)
        assertEquals(2082, p9.vikramSamvat)
        assertEquals(LocalTime.of(12, 32), p9.tithiEndsAt.toLocalTime().withSecond(0).withNano(0))

        val p10 = GujaratiPanchangHelper.day(d("10-11-2026"))
        assertEquals("કારતક સુદ એકમ", p10.tithiGu)
        assertEquals(2083, p10.vikramSamvat)
        assertEquals(7, p10.monthIndex)

        assertEquals("કારતક સુદ બીજ", GujaratiPanchangHelper.day(d("11-11-2026")).tithiGu)
    }

    @Test
    fun vikramSamvat_switchesOnKartakSudEkam() {
        assertEquals(2082, GujaratiPanchangHelper.day(d("01-01-2026")).vikramSamvat)
        assertEquals(2082, GujaratiPanchangHelper.day(d("09-11-2026")).vikramSamvat)
        assertEquals(2083, GujaratiPanchangHelper.day(d("10-11-2026")).vikramSamvat)
        assertEquals(2083, GujaratiPanchangHelper.day(d("31-12-2026")).vikramSamvat)
        // Gujarati new year 2025 (Bestu Varas) was 22-10-2025.
        assertEquals(2081, GujaratiPanchangHelper.day(d("21-10-2025")).vikramSamvat)
        assertEquals("કારતક સુદ એકમ", GujaratiPanchangHelper.day(d("22-10-2025")).tithiGu)
        assertEquals(2082, GujaratiPanchangHelper.day(d("22-10-2025")).vikramSamvat)
    }

    @Test
    fun adhikJeth2026_isDetected() {
        // 2026 has an Adhik Jyeshtha lunation (≈ 17-05 → 15-06-2026).
        val p = GujaratiPanchangHelper.day(d("20-05-2026"))
        assertTrue(p.isAdhik)
        assertEquals(2, p.monthIndex)
        assertTrue(p.tithiGu.startsWith("અધિક જેઠ"))
        val nija = GujaratiPanchangHelper.day(d("20-06-2026"))
        assertFalse(nija.isAdhik)
        assertEquals("જેઠ", nija.monthGu)
    }

    @Test
    fun weekday_sevenConsecutiveDays() {
        val expected = listOf("સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર", "રવિવાર")
        val monday = d("02-11-2026")
        expected.forEachIndexed { i, name -> assertEquals(name, GujaratiPanchangHelper.day(monday.plusDays(i.toLong())).weekdayGu) }
    }

    @Test
    fun tithiIndex_toPakshaMapping() {
        val cases = mapOf(
            1 to (Paksha.SUD to 1), 2 to (Paksha.SUD to 2), 11 to (Paksha.SUD to 11), 15 to (Paksha.SUD to 15),
            16 to (Paksha.VAD to 1), 26 to (Paksha.VAD to 11), 30 to (Paksha.VAD to 15),
        )
        cases.forEach { (idx, exp) -> assertEquals("idx $idx", exp, PanchangRules.pakshaOf(idx)) }
        assertEquals("પૂનમ", PanchangNames.tithiName(Paksha.SUD, 15))
        assertEquals("અમાસ", PanchangNames.tithiName(Paksha.VAD, 15))
        assertEquals("અગિયારસ", PanchangNames.tithiName(Paksha.VAD, 11))
    }

    @Test
    fun moonIllumination_poonamNearOne_amasNearZero() {
        val days = GujaratiPanchangHelper.month(2026, 11)
        val poonam = days.first { it.isPoonam }
        assertTrue("poonam k=${poonam.moonIllumination}", poonam.moonIllumination > 0.97f)
        assertTrue(poonam.waxing)
        val amas = GujaratiPanchangHelper.day(d("09-11-2026"))
        assertTrue(amas.isAmas)
        assertTrue("amas k=${amas.moonIllumination}", amas.moonIllumination < 0.03f)
    }

    @Test
    fun sunrise_ahmedabad_solstices() {
        val june = GujaratiPanchangHelper.day(d("21-06-2026")).sunrise.toLocalTime()
        assertTrue("june sunrise $june", june in LocalTime.of(5, 52)..LocalTime.of(6, 3))
        val dec = GujaratiPanchangHelper.day(d("21-12-2026")).sunrise.toLocalTime()
        assertTrue("december sunrise $dec", dec in LocalTime.of(7, 12)..LocalTime.of(7, 23))
    }

    @Test
    fun adhik_syntheticLunation() {
        // r0 == r1: no sankranti → Adhik, named after the next month.
        val adhik = PanchangRules.monthFromRashis(r0 = 1, r1 = 1)
        assertTrue(adhik.isAdhik)
        assertEquals(2, adhik.monthIndex)
        assertEquals("અધિક જેઠ", PanchangNames.month(adhik.monthIndex, adhik.isAdhik))
        // Normal: sun in Kanya at the opening new moon → આસો; Tula → કારતક; Meena → ચૈત્ર.
        assertEquals(6, PanchangRules.monthFromRashis(5, 6).monthIndex)
        assertEquals(7, PanchangRules.monthFromRashis(6, 7).monthIndex)
        assertEquals(0, PanchangRules.monthFromRashis(11, 0).monthIndex)
        assertFalse(PanchangRules.monthFromRashis(11, 0).isAdhik)
        // Kshaya (r1 = r0 + 2): still (r0 + 1) with a warning.
        assertTrue(PanchangRules.monthFromRashis(8, 10).isKshayaWarning)
        assertEquals(9, PanchangRules.monthFromRashis(8, 10).monthIndex)
    }

    @Test
    fun tithiBoundaries_bracketSunrise() {
        var date = d("01-01-2026")
        repeat(60) {
            val p = GujaratiPanchangHelper.day(date)
            assertTrue(!p.tithiStartsAt.isAfter(p.sunrise) && p.tithiEndsAt.isAfter(p.sunrise))
            val len = Duration.between(p.tithiStartsAt, p.tithiEndsAt).toMinutes()
            assertTrue("tithi length $len min on $date", len in (19 * 60L)..(27 * 60L))
            date = date.plusDays(1)
        }
    }

    @Test
    fun calendarMonth_hasAllDays_andConsistentSequence() {
        val nov = GujaratiPanchangHelper.month(2026, 11)
        assertEquals(30, nov.size)
        // Consecutive sunrise tithis advance by 1 (normal), 2 (kshaya skipped) or 0 (vriddhi).
        nov.zipWithNext().forEach { (a, b) ->
            val step = ((b.tithiIndex - a.tithiIndex) % 30 + 30) % 30
            assertTrue("step $step ${a.date}", step in 0..2)
        }
    }

    @Test
    fun displayFormats() {
        assertEquals("03 નવેમ્બર 2026 | મંગળવાર | આસો વદ નોમ", GujaratiPanchangHelper.bannerLabel(d("03-11-2026")))
        assertEquals("02-11-2026 · આસો વદ આઠમ", GujaratiPanchangHelper.listLabel(d("02-11-2026")))
        assertEquals("વિ.સં. 2082", GujaratiPanchangHelper.day(d("03-11-2026")).vsLabel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRange_throws() {
        GujaratiPanchangHelper.day(LocalDate.of(1989, 12, 31))
    }

    @Test
    fun performance_cachedAndCold() {
        GujaratiPanchangHelper.clearCache()
        val date = d("15-08-2026")
        val t0 = System.nanoTime()
        GujaratiPanchangHelper.day(date)
        val coldMs = (System.nanoTime() - t0) / 1e6
        val t1 = System.nanoTime()
        repeat(100) { GujaratiPanchangHelper.day(date) }
        val cachedMs = (System.nanoTime() - t1) / 1e6 / 100
        // JVM budget is generous (first call includes class loading); S22 target is < 40 ms cold / < 5 ms cached.
        assertTrue("cold $coldMs ms", coldMs < 500)
        assertTrue("cached $cachedMs ms", cachedMs < 5)
    }
}
