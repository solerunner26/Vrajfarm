package com.vraj.khetihisab.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 4-digit PIN storage (doc 04 §Data). Backed by EncryptedSharedPreferences (AES256_GCM master key)
 * in the app; tests pass any [SharedPreferences].
 *
 * - `pin_hash` = PBKDF2WithHmacSHA256(pin, salt, 120 000 iterations, 256 bit), Base64
 * - `pin_salt` = 16 random bytes, Base64
 * - lockout: after every 5th consecutive failure → 30 s, doubling per further 5 failures, max 15 min.
 *   `lock_until` uses wall-clock millis so it survives process death.
 */
class PinStore(
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
    private val iterations: Int = ITERATIONS,
) {
    sealed interface VerifyResult {
        data object Success : VerifyResult
        data class Wrong(val failedAttempts: Int, val attemptsBeforeLockout: Int) : VerifyResult
        data class LockedOut(val remainingMillis: Long) : VerifyResult
        data object NoPin : VerifyResult
    }

    fun hasPin(): Boolean = prefs.getString(KEY_HASH, null) != null

    fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }) { "PIN must be $PIN_LENGTH digits" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, b64(salt))
            .putString(KEY_HASH, b64(hash(pin, salt)))
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    /** Remaining lockout in millis (0 when not locked out). */
    fun lockoutRemainingMillis(): Long = (prefs.getLong(KEY_LOCK_UNTIL, 0L) - now()).coerceAtLeast(0L)

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED, 0)

    fun verify(pin: String): VerifyResult {
        val storedHash = prefs.getString(KEY_HASH, null) ?: return VerifyResult.NoPin
        val salt = prefs.getString(KEY_SALT, null)?.let(::unb64) ?: return VerifyResult.NoPin
        val remaining = lockoutRemainingMillis()
        if (remaining > 0) return VerifyResult.LockedOut(remaining)

        if (MessageDigest.isEqual(hash(pin, salt), unb64(storedHash))) {
            prefs.edit().putInt(KEY_FAILED, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
            return VerifyResult.Success
        }
        val failed = failedAttempts() + 1
        val lockMs = lockoutMillisFor(failed)
        prefs.edit()
            .putInt(KEY_FAILED, failed)
            .putLong(KEY_LOCK_UNTIL, if (lockMs > 0) now() + lockMs else 0L)
            .apply()
        return if (lockMs > 0) VerifyResult.LockedOut(lockMs)
        else VerifyResult.Wrong(failed, FAILURES_PER_STEP - failed % FAILURES_PER_STEP)
    }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIO, false)
        set(value) = prefs.edit().putBoolean(KEY_BIO, value).apply()

    internal fun saltForTest(): String? = prefs.getString(KEY_SALT, null)

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun b64(b: ByteArray) = Base64.getEncoder().encodeToString(b)
    private fun unb64(s: String) = Base64.getDecoder().decode(s)

    companion object {
        const val PIN_LENGTH = 4
        const val ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        const val FAILURES_PER_STEP = 5
        const val BASE_LOCKOUT_MS = 30_000L
        const val MAX_LOCKOUT_MS = 15 * 60_000L

        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_BIO = "biometric_enabled"
        const val KEY_FAILED = "failed_attempts"
        const val KEY_LOCK_UNTIL = "lock_until"

        /** 5 → 30 s, 10 → 60 s, 15 → 120 s … capped at 15 min; 0 when not on a 5-step boundary. */
        fun lockoutMillisFor(failedAttempts: Int): Long {
            if (failedAttempts <= 0 || failedAttempts % FAILURES_PER_STEP != 0) return 0L
            val step = failedAttempts / FAILURES_PER_STEP - 1 // 0-based
            if (step >= 5) return MAX_LOCKOUT_MS // 30 s · 2^5 = 16 min > cap
            return (BASE_LOCKOUT_MS shl step).coerceAtMost(MAX_LOCKOUT_MS)
        }

        /** Production instance on EncryptedSharedPreferences. Call off the main thread if possible. */
        fun create(context: Context): PinStore {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            @Suppress("DEPRECATION")
            val prefs = EncryptedSharedPreferences.create(
                context,
                "vk_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            return PinStore(prefs)
        }
    }
}
