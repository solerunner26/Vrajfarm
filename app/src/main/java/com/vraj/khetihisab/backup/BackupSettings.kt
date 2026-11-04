package com.vraj.khetihisab.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

/**
 * Backup endpoint + API key (doc 15 "Settings"). Production storage is EncryptedSharedPreferences
 * (`vk_backup_secure`); tests pass any [SharedPreferences].
 *
 * The key is never hard-coded, never logged and never included in [toString]. There is no default key:
 * until the owner types one in Settings, [isConfigured] is false and the worker records
 * [BackupFailure.NOT_CONFIGURED] without touching the network.
 */
class BackupSettings(private val prefs: SharedPreferences) {

    /** Endpoint URL; [DEFAULT_URL] (a placeholder) until the owner enters the real domain. */
    val endpointUrl: String get() = prefs.getString(KEY_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_URL

    fun hasApiKey(): Boolean = !prefs.getString(KEY_API, null).isNullOrBlank()

    /** The secret key, for the uploader only. Do not log or display it. */
    internal fun apiKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    /** Stable random id sent as `X-Vraj-Device` (server log / email only). Not a secret. */
    val installId: String
        get() = prefs.getString(KEY_INSTALL, null) ?: synchronized(this) {
            prefs.getString(KEY_INSTALL, null) ?: UUID.randomUUID().toString().also {
                prefs.edit().putString(KEY_INSTALL, it).apply()
            }
        }

    /** Validates and saves the endpoint. Returns the problem instead of saving when invalid. */
    fun setEndpoint(url: String): EndpointProblem? {
        val trimmed = url.trim()
        validateEndpoint(trimmed)?.let { return it }
        prefs.edit().putString(KEY_URL, trimmed).apply()
        return null
    }

    /** Validates and saves the key (trimmed). `null` / blank clears it. */
    fun setApiKey(key: String?): KeyProblem? {
        val k = key?.trim().orEmpty()
        if (k.isEmpty()) {
            prefs.edit().remove(KEY_API).apply()
            return null
        }
        validateKey(k)?.let { return it }
        prefs.edit().putString(KEY_API, k).apply()
        return null
    }

    /** True when a real (non-placeholder) https URL and a key are both set. */
    fun isConfigured(): Boolean = hasApiKey() && validateEndpoint(endpointUrl) == null

    /** Parsed endpoint, or null when not usable. */
    fun endpointHttpUrl(): HttpUrl? = endpointUrl.takeIf { validateEndpoint(it) == null }?.toHttpUrlOrNull()

    override fun toString(): String = "BackupSettings(url=$endpointUrl, key=${if (hasApiKey()) "<set>" else "<none>"})"

    enum class EndpointProblem { INVALID, NOT_HTTPS, PLACEHOLDER }
    enum class KeyProblem { TOO_SHORT, INVALID_CHARS }

    companion object {
        /** Placeholder until the owner provides the GoDaddy domain (HANDOVER open question 6). */
        const val DEFAULT_URL = "https://example.com/vraj/backup_receiver.php"
        const val PREFS_NAME = "vk_backup_secure"
        /** Server rejects keys shorter than 32 characters (server/README.md step 4). */
        const val MIN_KEY_LENGTH = 32

        private const val KEY_URL = "endpoint_url"
        private const val KEY_API = "api_key"
        private const val KEY_INSTALL = "install_id"

        fun validateEndpoint(url: String): EndpointProblem? {
            val u = url.trim().toHttpUrlOrNull() ?: return EndpointProblem.INVALID
            if (!u.isHttps) return EndpointProblem.NOT_HTTPS // server returns 403 "https" for plain http
            if (u.host == "example.com" || u.host.endsWith(".example.com")) return EndpointProblem.PLACEHOLDER
            return null
        }

        fun validateKey(key: String): KeyProblem? {
            if (key.length < MIN_KEY_LENGTH) return KeyProblem.TOO_SHORT
            // Must be a legal header value: printable ASCII, no spaces.
            if (key.any { it.code !in 0x21..0x7E }) return KeyProblem.INVALID_CHARS
            return null
        }

        /** Production instance on EncryptedSharedPreferences. Call off the main thread if possible. */
        fun create(context: Context): BackupSettings {
            val app = context.applicationContext
            val masterKey = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            @Suppress("DEPRECATION")
            val prefs = EncryptedSharedPreferences.create(
                app,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            return BackupSettings(prefs)
        }
    }
}
