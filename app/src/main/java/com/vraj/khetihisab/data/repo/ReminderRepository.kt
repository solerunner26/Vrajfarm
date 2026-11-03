package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.dao.ReminderDao
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ReminderSource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

class ReminderRepository(
    private val dao: ReminderDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
) {
    fun active(): Flow<List<ReminderEntity>> = dao.active()
    fun archived(): Flow<List<ReminderEntity>> = dao.archived()
    fun urgent(n: Int = 3): Flow<List<ReminderEntity>> = dao.urgent(n)
    suspend fun activeNow(): List<ReminderEntity> = dao.activeNow()
    suspend fun byId(id: Long): ReminderEntity? = dao.byId(id)

    suspend fun add(r: ReminderEntity): Long = dao.insert(r).also { listener.onDataChanged() }

    suspend fun update(r: ReminderEntity) {
        dao.update(r.copy(updatedAt = clock()))
        listener.onDataChanged()
    }

    suspend fun markDone(id: Long) {
        dao.markDone(id, clock())
        listener.onDataChanged()
    }

    suspend fun markUndone(id: Long) {
        dao.markUndone(id, clock())
        listener.onDataChanged()
    }

    suspend fun softDelete(id: Long) {
        dao.softDelete(id, clock())
        listener.onDataChanged()
    }

    sealed interface CreateResult {
        data class Created(val id: Long) : CreateResult
        /** "રિમાઇન્ડર પહેલેથી છે" */
        data class AlreadyExists(val existing: ReminderEntity) : CreateResult
    }

    /** Panchang "રિમાઇન્ડર બનાવો": one reminder per day (sourceId = epochDay), due 07:00 IST. */
    suspend fun createPanchangReminder(date: LocalDate, titleGu: String, tithiGu: String): CreateResult {
        val epochDay = date.toEpochDay()
        dao.findBySource(ReminderSource.PANCHANG, epochDay)?.let { return CreateResult.AlreadyExists(it) }
        val now = clock()
        val id = dao.insert(
            ReminderEntity(
                title = titleGu,
                note = tithiGu,
                imagePath = null,
                priority = Priority.NORMAL,
                dueAt = LedgerMath.istMillis(date, LocalTime.of(7, 0)),
                sourceType = ReminderSource.PANCHANG,
                sourceId = epochDay,
                createdAt = now,
                updatedAt = now,
            ),
        )
        listener.onDataChanged()
        return CreateResult.Created(id)
    }
}
