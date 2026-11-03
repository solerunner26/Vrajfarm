package com.vraj.khetihisab.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Exactly one row per [ProfileRole] (unique index). Seeded: OWNER + 2 blank managers. */
@Entity(tableName = "person_profile", indices = [Index(value = ["role"], unique = true)])
data class PersonProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: ProfileRole,
    val name: String,
    val phone: String?,
    val address: String?,
    val photoPath: String?,
    val updatedAt: Long,
)

/** Per-type Panchang alert settings. Guidance texts are newline-separated lines, framed "માન્યતા પ્રમાણે". */
@Entity(tableName = "panchang_alert_pref")
data class PanchangAlertPrefEntity(
    @PrimaryKey val type: PanchangAlertType,
    val enabled: Boolean = true,
    val notifyEveningBefore: Boolean = true,
    val showDialogOnDay: Boolean = true,
    /** કરવું — user-editable guidance, one item per line. */
    val doText: String,
    /** ટાળવું — user-editable guidance, one item per line. */
    val avoidText: String,
    val updatedAt: Long,
)

@Entity(tableName = "custom_festival")
data class CustomFestivalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameGu: String,
    /** 0..11 amanta (0 = ચૈત્ર). */
    val monthIndex: Int?,
    /** "SUD" | "VAD" */
    val paksha: String?,
    val tithiInPaksha: Int?,
    /** "MM-dd" for solar dates, e.g. "01-14". */
    val fixedMonthDay: String?,
    val note: String?,
    val enabled: Boolean = true,
)
