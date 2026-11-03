package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vraj.khetihisab.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=0 ORDER BY priority='HIGH' DESC, dueAt")
    fun active(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=1 ORDER BY doneAt DESC")
    fun archived(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=0 AND priority='HIGH' ORDER BY dueAt LIMIT :n")
    fun urgent(n: Int = 3): Flow<List<ReminderEntity>>

    /** For rescheduling alarms after boot. */
    @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=0 ORDER BY dueAt")
    suspend fun activeNow(): List<ReminderEntity>

    @Query("SELECT * FROM reminder WHERE id=:id")
    suspend fun byId(id: Long): ReminderEntity?

    /** Dedupe check for PANCHANG / LOAN reminders ("રિમાઇન્ડર પહેલેથી છે"). */
    @Query("SELECT * FROM reminder WHERE isDeleted=0 AND sourceType=:sourceType AND sourceId=:sourceId LIMIT 1")
    suspend fun findBySource(sourceType: String, sourceId: Long): ReminderEntity?

    @Query("UPDATE reminder SET isDone=1, doneAt=:at, updatedAt=:at WHERE id=:id")
    suspend fun markDone(id: Long, at: Long)

    /** Undo for [markDone]. */
    @Query("UPDATE reminder SET isDone=0, doneAt=NULL, updatedAt=:now WHERE id=:id")
    suspend fun markUndone(id: Long, now: Long)

    /** Loan returned -> its linked reminder is done. */
    @Query(
        """UPDATE reminder SET isDone=1, doneAt=:at, updatedAt=:at
    WHERE isDone=0 AND sourceType=:sourceType AND sourceId=:sourceId""",
    )
    suspend fun markDoneBySource(sourceType: String, sourceId: Long, at: Long)

    @Query("UPDATE reminder SET isDeleted=1, updatedAt=:now WHERE id=:id")
    suspend fun softDelete(id: Long, now: Long)

    @Insert suspend fun insert(r: ReminderEntity): Long

    @Update suspend fun update(r: ReminderEntity)
}
