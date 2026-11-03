package com.vraj.khetihisab.panchang

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * Independent accuracy checks for Astro against published new-moon instants (solar eclipses are
 * timed to the second, so they are a reliable external reference).
 */
class AstroTest {

    private fun assertNewMoon(expectedUtc: String, toleranceMinutes: Long = 3) {
        val expected = Instant.parse(expectedUtc)
        val solved = Astro.solveElongation(0.0, expected.minus(Duration.ofHours(9)))
        val diff = abs(Duration.between(expected, solved).toMinutes())
        assertTrue("new moon $expectedUtc solved $solved (Δ $diff min)", diff <= toleranceMinutes)
    }

    @Test fun newMoon_totalEclipse_2024_04_08() = assertNewMoon("2024-04-08T18:21:00Z")

    @Test fun newMoon_annularEclipse_2024_10_02() = assertNewMoon("2024-10-02T18:49:00Z")

    @Test fun newMoon_diwali_2024_11_01() = assertNewMoon("2024-11-01T12:47:00Z")

    @Test fun newMoon_diwali_2026_11_09() = assertNewMoon("2026-11-09T07:02:00Z")

    @Test
    fun norm_and_wrap() {
        assertEquals(350.0, Astro.norm360(-10.0), 1e-9)
        assertEquals(10.0, Astro.norm360(370.0), 1e-9)
        assertEquals(-10.0, Astro.wrap180(350.0), 1e-9)
        assertEquals(170.0, Astro.wrap180(170.0), 1e-9)
    }

    @Test
    fun julianDay_epoch() {
        assertEquals(2451545.0, Astro.jd(Instant.parse("2000-01-01T12:00:00Z")), 1e-9)
    }

    @Test
    fun deltaT_2026_isAbout69s() {
        val dt = Astro.deltaTSeconds(2026.5)
        assertTrue("ΔT $dt", dt in 65.0..80.0)
    }
}
