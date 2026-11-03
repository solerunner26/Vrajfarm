package com.vraj.khetihisab.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockManagerTest {

    private var now = 0L
    private val lm = LockManager(elapsedRealtime = { now })

    @Test
    fun coldStart_isLocked() {
        assertTrue(lm.locked.value)
        lm.onAppForeground()
        assertTrue(lm.locked.value)
    }

    @Test
    fun shortBackground_staysUnlocked() {
        lm.unlock()
        lm.onAppBackground()
        now += 30_000 // exactly 30 s: not more than the timeout
        lm.onAppForeground()
        assertFalse(lm.locked.value)
    }

    @Test
    fun longBackground_relocks() {
        lm.unlock()
        lm.onAppBackground()
        now += 30_001
        lm.onAppForeground()
        assertTrue(lm.locked.value)
    }

    @Test
    fun suppressNextLock_skipsOnce() {
        lm.unlock()
        lm.suppressNextLock = true // e.g. camera / WhatsApp launched
        lm.onAppBackground()
        now += 120_000
        lm.onAppForeground()
        assertFalse(lm.locked.value)
        assertFalse(lm.suppressNextLock)

        lm.onAppBackground()
        now += 120_000
        lm.onAppForeground()
        assertTrue(lm.locked.value)
    }
}
