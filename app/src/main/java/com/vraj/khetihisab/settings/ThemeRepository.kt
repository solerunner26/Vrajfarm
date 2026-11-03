package com.vraj.khetihisab.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vraj.khetihisab.ui.theme.VkThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App-wide UI preferences DataStore. Glance widgets read the same file (doc 13). */
val Context.vkPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vk_prefs")

data class ThemePrefs(val themeId: VkThemeId, val followSystemDark: Boolean)

/** Persists the chosen theme (doc 01 §7). Not a Room entity by design. */
class ThemeRepository(private val dataStore: DataStore<Preferences>) {

    val prefs: Flow<ThemePrefs> = dataStore.data.map {
        ThemePrefs(VkThemeId.fromKey(it[KEY_THEME]), it[KEY_FOLLOW_SYSTEM] ?: false)
    }

    val themeId: Flow<VkThemeId> = prefs.map { it.themeId }

    suspend fun setTheme(id: VkThemeId) {
        dataStore.edit { it[KEY_THEME] = id.storageKey }
    }

    suspend fun setFollowSystemDark(enabled: Boolean) {
        dataStore.edit { it[KEY_FOLLOW_SYSTEM] = enabled }
    }

    companion object {
        /** DataStore key name shared with Glance widgets. */
        const val THEME_KEY_NAME = "vk_theme"
        val KEY_THEME = stringPreferencesKey(THEME_KEY_NAME)
        val KEY_FOLLOW_SYSTEM = booleanPreferencesKey("vk_theme_follow_system")
    }
}
