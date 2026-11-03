package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import kotlinx.coroutines.flow.Flow

/** Panchang alert prefs (one row per type) + user-added festivals (doc 10). */
@Dao
interface PanchangDao {
    @Query("SELECT * FROM panchang_alert_pref")
    fun prefs(): Flow<List<PanchangAlertPrefEntity>>

    @Query("SELECT * FROM panchang_alert_pref")
    suspend fun prefsNow(): List<PanchangAlertPrefEntity>

    @Query("SELECT * FROM panchang_alert_pref WHERE type=:type")
    suspend fun pref(type: PanchangAlertType): PanchangAlertPrefEntity?

    @Upsert suspend fun upsertPref(p: PanchangAlertPrefEntity)

    @Query("UPDATE panchang_alert_pref SET enabled=:enabled, updatedAt=:now WHERE type=:type")
    suspend fun setEnabled(type: PanchangAlertType, enabled: Boolean, now: Long)

    @Query("UPDATE panchang_alert_pref SET doText=:doText, avoidText=:avoidText, updatedAt=:now WHERE type=:type")
    suspend fun updateGuidance(type: PanchangAlertType, doText: String, avoidText: String, now: Long)

    @Query("SELECT * FROM custom_festival WHERE enabled=1")
    suspend fun customFestivals(): List<CustomFestivalEntity>

    @Query("SELECT * FROM custom_festival ORDER BY id")
    fun allCustomFestivals(): Flow<List<CustomFestivalEntity>>

    @Upsert suspend fun upsertFestival(f: CustomFestivalEntity): Long

    @Query("DELETE FROM custom_festival WHERE id=:id")
    suspend fun deleteFestival(id: Long)
}
