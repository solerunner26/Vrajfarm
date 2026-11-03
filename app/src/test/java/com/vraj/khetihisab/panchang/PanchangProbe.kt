package com.vraj.khetihisab.panchang

import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Temporary probe: prints engine output around Diwali 2026. */
class PanchangProbe {
    @Test
    fun probe() {
        var d = LocalDate.of(2026, 10, 30)
        while (d <= LocalDate.of(2026, 11, 12)) {
            val p = GujaratiPanchangHelper.day(d)
            println("PROBE $d ${p.weekdayGu} ${p.tithiGu} idx=${p.tithiIndex} VS=${p.vikramSamvat} rise=${p.sunrise.toLocalTime()} start=${p.tithiStartsAt} end=${p.tithiEndsAt} near=${p.nearBoundary} k=${p.moonIllumination}")
            d = d.plusDays(1)
        }
        for (g in listOf("2024-04-08T12:00:00Z", "2024-10-02T12:00:00Z", "2024-11-01T06:00:00Z", "2026-11-09T00:00:00Z", "2025-10-21T00:00:00Z")) {
            println("PROBE NM near $g -> ${Astro.solveElongation(0.0, Instant.parse(g))}")
        }
        for (s in listOf(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))) {
            println("PROBE sun $s ${GujaratiPanchangHelper.day(s).sunrise} set ${GujaratiPanchangHelper.day(s).sunset}")
        }
        for (s in listOf(LocalDate.of(2026, 3, 19), LocalDate.of(2026, 3, 20), LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 16), LocalDate.of(2026, 1, 15), LocalDate.of(2025, 10, 22), LocalDate.of(2025, 10, 23))) {
            val p = GujaratiPanchangHelper.day(s)
            println("PROBE $s ${p.tithiGu} VS=${p.vikramSamvat} adhik=${p.isAdhik}")
        }
    }
}
