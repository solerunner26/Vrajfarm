package com.vraj.khetihisab.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * દૂધ રજીસ્ટર (milk register), DB v2 (2026-11-05). One row = one animal's milk at one milking.
 * Money stays in the ledger: the dairy's payment is an ANIMAL INCOME transaction ("દૂધ વેચાણ"), and the
 * register only shows what the dairy SHOULD have paid, so nothing is counted twice in any P&L.
 */
@Entity(tableName = "milk_entry", indices = [Index(value = ["date"])])
data class MilkEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    /** [MilkSession.MORNING] (સવાર) or [MilkSession.EVENING] (સાંજ). */
    val session: String,
    /** Animal name as the family calls it ("ગંગા ગાય", "ભેંસ 2"). Free text, chips offer earlier names. */
    val animal: String,
    val litres: Double,
    /** Fat %, optional (dairies in Gujarat price by fat). */
    val fatPercent: Double? = null,
    /** Dairy rate in paise per litre for this milking. */
    val ratePaise: Long,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Litres × rate, rounded to the paisa. */
    val amountPaise: Long get() = Math.round(litres * ratePaise)
}

object MilkSession {
    const val MORNING = "AM"
    const val EVENING = "PM"
}
