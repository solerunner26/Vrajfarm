package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.dao.PanchangDao
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.seed.PanchangDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Panchang alert prefs + custom festivals (doc 10). Values outside Room (evening hour, show-guidance,
 * dialog-dismissed keys, "વદ બીજ પણ") live in SharedPreferences, owned by doc 10's engine/scheduler.
 */
class PanchangPrefRepository(
    private val dao: PanchangDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun prefs(): Flow<List<PanchangAlertPrefEntity>> = dao.prefs()

    /** Map form expected by PanchangAlertEngine.alertFor; missing rows fall back to defaults. */
    fun prefsMap(): Flow<Map<PanchangAlertType, PanchangAlertPrefEntity>> = dao.prefs().map(::withDefaults)

    suspend fun prefsMapNow(): Map<PanchangAlertType, PanchangAlertPrefEntity> = withDefaults(dao.prefsNow())

    private fun withDefaults(rows: List<PanchangAlertPrefEntity>): Map<PanchangAlertType, PanchangAlertPrefEntity> {
        val byType = rows.associateBy { it.type }
        return PanchangAlertType.entries.associateWith { byType[it] ?: PanchangDefaults.defaultPref(it, 0L) }
    }

    suspend fun upsertPref(p: PanchangAlertPrefEntity) = dao.upsertPref(p.copy(updatedAt = clock()))

    suspend fun setEnabled(type: PanchangAlertType, enabled: Boolean) {
        ensureRow(type)
        dao.setEnabled(type, enabled, clock())
    }

    suspend fun updateGuidance(type: PanchangAlertType, doText: String, avoidText: String) {
        ensureRow(type)
        dao.updateGuidance(type, doText.trim(), avoidText.trim(), clock())
    }

    /** "મૂળ સલાહ પાછી લાવો": restore the default "માન્યતા પ્રમાણે" texts; on/off switches are kept. */
    suspend fun resetGuidance(type: PanchangAlertType) {
        ensureRow(type)
        dao.updateGuidance(type, PanchangDefaults.doText(type), PanchangDefaults.avoidText(type), clock())
    }

    private suspend fun ensureRow(type: PanchangAlertType) {
        if (dao.pref(type) == null) dao.upsertPref(PanchangDefaults.defaultPref(type, clock()))
    }

    // ---- custom festivals ----
    suspend fun customFestivals(): List<CustomFestivalEntity> = dao.customFestivals()
    fun allCustomFestivals(): Flow<List<CustomFestivalEntity>> = dao.allCustomFestivals()
    suspend fun upsertFestival(f: CustomFestivalEntity): Long = dao.upsertFestival(f)
    suspend fun deleteFestival(id: Long) = dao.deleteFestival(id)
}
