package com.vraj.khetihisab.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App auto-lock (doc 04 §2). Register [observer] on `ProcessLifecycleOwner.get().lifecycle`.
 *
 * - Starts locked (cold start).
 * - onStop records `elapsedRealtime`; onStart re-locks when more than [timeoutMs] passed.
 *   elapsedRealtime is immune to device date/time changes.
 * - Camera / voice / WhatsApp launches set [suppressNextLock] so returning does not re-prompt.
 */
class LockManager(
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Set right before launching an external intent that returns to the app. Cleared on next start. */
    @Volatile
    var suppressNextLock: Boolean = false

    private var backgroundedAt: Long? = null

    fun unlock() {
        _locked.value = false
    }

    fun lock() {
        _locked.value = true
    }

    fun onAppBackground() {
        backgroundedAt = elapsedRealtime()
    }

    fun onAppForeground() {
        val since = backgroundedAt
        backgroundedAt = null
        if (suppressNextLock) {
            suppressNextLock = false
            return
        }
        if (since == null) return // cold start: already locked
        if (elapsedRealtime() - since > timeoutMs) _locked.value = true
    }

    val observer: DefaultLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = onAppForeground()
        override fun onStop(owner: LifecycleOwner) = onAppBackground()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 30_000L
    }
}
