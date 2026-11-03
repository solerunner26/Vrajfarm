package com.vraj.khetihisab.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinStoreTest {

    private var clock = 1_000_000L

    /** Low iteration count keeps lockout tests fast; [hashVerify_withProductionIterations] covers 120 000. */
    private fun store(prefs: FakeSharedPreferences = FakeSharedPreferences(), iterations: Int = 1_000) =
        PinStore(prefs, now = { clock }, iterations = iterations)

    @Test
    fun hashVerify_withProductionIterations() {
        val s = PinStore(FakeSharedPreferences(), now = { clock })
        assertFalse(s.hasPin())
        assertEquals(PinStore.VerifyResult.NoPin, s.verify("1234"))
        s.setPin("1234")
        assertTrue(s.hasPin())
        assertEquals(PinStore.VerifyResult.Success, s.verify("1234"))
        assertTrue(s.verify("4321") is PinStore.VerifyResult.Wrong)
    }

    @Test
    fun saltIsRandom_andHashDiffers() {
        val p1 = FakeSharedPreferences()
        val p2 = FakeSharedPreferences()
        store(p1).setPin("1234")
        store(p2).setPin("1234")
        assertNotEquals(p1.getString(PinStore.KEY_SALT, null), p2.getString(PinStore.KEY_SALT, null))
        assertNotEquals(p1.getString(PinStore.KEY_HASH, null), p2.getString(PinStore.KEY_HASH, null))
        // The raw PIN is never stored.
        assertFalse(p1.all.values.any { it == "1234" })
    }

    @Test
    fun lockoutSchedule_doublesAndCaps() {
        assertEquals(0L, PinStore.lockoutMillisFor(4))
        assertEquals(30_000L, PinStore.lockoutMillisFor(5))
        assertEquals(0L, PinStore.lockoutMillisFor(6))
        assertEquals(60_000L, PinStore.lockoutMillisFor(10))
        assertEquals(120_000L, PinStore.lockoutMillisFor(15))
        assertEquals(240_000L, PinStore.lockoutMillisFor(20))
        assertEquals(480_000L, PinStore.lockoutMillisFor(25))
        assertEquals(900_000L, PinStore.lockoutMillisFor(30))
        assertEquals(900_000L, PinStore.lockoutMillisFor(100))
    }

    @Test
    fun fiveWrong_locksOut30s_thenDoubles() {
        val s = store()
        s.setPin("1234")
        repeat(4) { assertTrue(s.verify("0000") is PinStore.VerifyResult.Wrong) }
        val fifth = s.verify("0000")
        assertEquals(PinStore.VerifyResult.LockedOut(30_000L), fifth)

        // Even the right PIN is refused during lockout.
        clock += 10_000
        val during = s.verify("1234")
        assertTrue(during is PinStore.VerifyResult.LockedOut)
        assertEquals(20_000L, (during as PinStore.VerifyResult.LockedOut).remainingMillis)

        clock += 20_000
        repeat(4) { assertTrue(s.verify("0000") is PinStore.VerifyResult.Wrong) }
        assertEquals(PinStore.VerifyResult.LockedOut(60_000L), s.verify("0000"))
    }

    @Test
    fun success_resetsFailures() {
        val s = store()
        s.setPin("2468")
        repeat(3) { s.verify("1111") }
        assertEquals(3, s.failedAttempts())
        assertEquals(PinStore.VerifyResult.Success, s.verify("2468"))
        assertEquals(0, s.failedAttempts())
    }

    @Test(expected = IllegalArgumentException::class)
    fun setPin_rejectsNonFourDigit() {
        store().setPin("12a4")
    }

    @Test
    fun biometricFlag_persists() {
        val prefs = FakeSharedPreferences()
        store(prefs).biometricEnabled = true
        assertTrue(store(prefs).biometricEnabled)
    }

    @Test
    fun formatMmSs() {
        assertEquals("0:28", formatMmSs(28_000))
        assertEquals("0:30", formatMmSs(29_001))
        assertEquals("15:00", formatMmSs(900_000))
    }
}
