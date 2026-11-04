package com.vraj.khetihisab.notify

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Panchang alert values that are not in Room (doc 10; HANDOVER "Database notes"). */
data class PanchangAlertSettings(
    /** "આગલી સાંજે સૂચના" time, IST. Default 19:00. */
    val eveningHour: Int = PanchangAlertSettingsStore.DEFAULT_HOUR,
    val eveningMinute: Int = 0,
    /** "કામની સલાહ બતાવો". */
    val showGuidance: Boolean = true,
    /** "વદ બીજ પણ". */
    val includeVadBij: Boolean = false,
)

/**
 * Stored in the shared `vk_prefs` DataStore (`Context.vkPrefsDataStore`). Keys:
 * `panchang_evening_hour`, `panchang_evening_minute`, `panchang_show_guidance`, `panchang_vad_bij`,
 * `panchang_last_notified_day` (epochDay of the last evening notification's target day) and
 * `panchang_dialog_dismissed_<epochDay>` (once-per-day Home dialog, doc 10 scheduling §5).
 */
class PanchangAlertSettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<PanchangAlertSettings> = dataStore.data.map(::read)

    suspend fun current(): PanchangAlertSettings = settings.first()

    suspend fun setEveningTime(hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59) { "bad time $hour:$minute" }
        dataStore.edit {
            it[KEY_HOUR] = hour
            it[KEY_MINUTE] = minute
        }
    }

    suspend fun setShowGuidance(on: Boolean) = dataStore.edit { it[KEY_GUIDANCE] = on }.let { }

    suspend fun setIncludeVadBij(on: Boolean) = dataStore.edit { it[KEY_VAD_BIJ] = on }.let { }

    // ---- once-per-day dialog flag ----

    /** True if the Home dialog for [date] was already acted on. */
    suspend fun isDialogDismissed(date: LocalDate): Boolean =
        dataStore.data.first()[dismissedKey(date.toEpochDay())] == true

    fun dialogDismissed(date: LocalDate): Flow<Boolean> =
        dataStore.data.map { it[dismissedKey(date.toEpochDay())] == true }

    /** Write on any dialog action; also removes keys older than [KEEP_DAYS] days. */
    suspend fun markDialogDismissed(date: LocalDate) {
        val today = date.toEpochDay()
        dataStore.edit { prefs ->
            prefs[dismissedKey(today)] = true
            staleDismissKeys(prefs.asMap().keys.map { it.name }, today).forEach { name ->
                prefs.remove(booleanPreferencesKey(name))
            }
        }
    }

    // ---- evening notification bookkeeping ----

    suspend fun lastNotifiedTargetDay(): Long? = dataStore.data.first()[KEY_LAST_NOTIFIED]

    suspend fun setLastNotifiedTargetDay(epochDay: Long) = dataStore.edit { it[KEY_LAST_NOTIFIED] = epochDay }.let { }

    companion object {
        const val DEFAULT_HOUR = 19
        const val KEEP_DAYS = 60L
        const val DISMISSED_PREFIX = "panchang_dialog_dismissed_"

        val KEY_HOUR = intPreferencesKey("panchang_evening_hour")
        val KEY_MINUTE = intPreferencesKey("panchang_evening_minute")
        val KEY_GUIDANCE = booleanPreferencesKey("panchang_show_guidance")
        val KEY_VAD_BIJ = booleanPreferencesKey("panchang_vad_bij")
        val KEY_LAST_NOTIFIED = longPreferencesKey("panchang_last_notified_day")

        fun dismissedKey(epochDay: Long) = booleanPreferencesKey("$DISMISSED_PREFIX$epochDay")

        /** Names of `panchang_dialog_dismissed_<day>` keys older than [KEEP_DAYS] relative to [today]. */
        fun staleDismissKeys(names: Collection<String>, today: Long): List<String> = names.filter { n ->
            n.startsWith(DISMISSED_PREFIX) &&
                (n.removePrefix(DISMISSED_PREFIX).toLongOrNull()?.let { today - it > KEEP_DAYS } ?: true)
        }

        fun read(p: Preferences) = PanchangAlertSettings(
            eveningHour = (p[KEY_HOUR] ?: DEFAULT_HOUR).coerceIn(0, 23),
            eveningMinute = (p[KEY_MINUTE] ?: 0).coerceIn(0, 59),
            showGuidance = p[KEY_GUIDANCE] ?: true,
            includeVadBij = p[KEY_VAD_BIJ] ?: false,
        )
    }
}
