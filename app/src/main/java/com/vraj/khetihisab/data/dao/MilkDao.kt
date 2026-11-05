package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vraj.khetihisab.data.entity.MilkEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** દૂધ રજીસ્ટર (DB v2). */
@Dao
interface MilkDao {
    @Insert suspend fun insert(e: MilkEntryEntity): Long
    @Update suspend fun update(e: MilkEntryEntity)
    @Delete suspend fun delete(e: MilkEntryEntity)

    @Query("SELECT * FROM milk_entry WHERE date BETWEEN :from AND :to ORDER BY date DESC, session DESC, animal")
    fun entries(from: LocalDate, to: LocalDate): Flow<List<MilkEntryEntity>>

    @Query("SELECT DISTINCT animal FROM milk_entry ORDER BY animal")
    fun animals(): Flow<List<String>>

    /** The rate used last time, so a new entry starts with it. */
    @Query("SELECT ratePaise FROM milk_entry ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun lastRate(): Long?

    /**
     * What the dairy owes for the period: litres and Σ(litres × rate) in paise.
     * (ROUND per row matches [MilkEntryEntity.amountPaise].)
     */
    @Query(
        """SELECT COALESCE(SUM(litres),0) litres, COALESCE(SUM(ROUND(litres * ratePaise)),0) amountPaise
        FROM milk_entry WHERE date BETWEEN :from AND :to""",
    )
    fun totals(from: LocalDate, to: LocalDate): Flow<MilkTotals>

    /** What the dairy actually paid: ANIMAL income entries in the "દૂધ વેચાણ"-type categories. */
    @Query(
        """SELECT COALESCE(SUM(t.amountPaise),0) FROM txn t JOIN category_master c ON c.id=t.categoryId
        WHERE t.isDeleted=0 AND t.section='ANIMAL' AND t.type='INCOME' AND c.name LIKE '%દૂધ%'
        AND t.date BETWEEN :from AND :to""",
    )
    fun dairyPaid(from: LocalDate, to: LocalDate): Flow<Long>
}

data class MilkTotals(val litres: Double, val amountPaise: Long)
