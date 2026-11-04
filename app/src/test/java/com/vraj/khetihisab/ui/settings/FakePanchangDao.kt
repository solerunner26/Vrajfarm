package com.vraj.khetihisab.ui.settings

import com.vraj.khetihisab.data.dao.PanchangDao
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory PanchangDao for JVM ViewModel tests. */
class FakePanchangDao(initial: List<PanchangAlertPrefEntity> = emptyList()) : PanchangDao {
    val prefsState = MutableStateFlow(initial.associateBy { it.type })
    val festivalsState = MutableStateFlow<List<CustomFestivalEntity>>(emptyList())
    private var nextId = 1L

    override fun prefs(): Flow<List<PanchangAlertPrefEntity>> = prefsState.map { it.values.toList() }
    override suspend fun prefsNow(): List<PanchangAlertPrefEntity> = prefsState.value.values.toList()
    override suspend fun pref(type: PanchangAlertType): PanchangAlertPrefEntity? = prefsState.value[type]
    override suspend fun upsertPref(p: PanchangAlertPrefEntity) {
        prefsState.value = prefsState.value + (p.type to p)
    }

    override suspend fun setEnabled(type: PanchangAlertType, enabled: Boolean, now: Long) {
        val p = prefsState.value[type] ?: return
        upsertPref(p.copy(enabled = enabled, updatedAt = now))
    }

    override suspend fun updateGuidance(type: PanchangAlertType, doText: String, avoidText: String, now: Long) {
        val p = prefsState.value[type] ?: return
        upsertPref(p.copy(doText = doText, avoidText = avoidText, updatedAt = now))
    }

    override suspend fun customFestivals(): List<CustomFestivalEntity> = festivalsState.value.filter { it.enabled }
    override fun allCustomFestivals(): Flow<List<CustomFestivalEntity>> = festivalsState
    override suspend fun upsertFestival(f: CustomFestivalEntity): Long {
        val id = if (f.id == 0L) nextId++ else f.id
        festivalsState.value = festivalsState.value.filter { it.id != id } + f.copy(id = id)
        return id
    }

    override suspend fun deleteFestival(id: Long) {
        festivalsState.value = festivalsState.value.filter { it.id != id }
    }
}
