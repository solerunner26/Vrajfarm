package com.vraj.khetihisab.ui.reminders

import android.content.Context
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.repo.ReminderRepository
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.notify.ReminderActions
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * What the Reminders screen reads and writes. Faked in unit tests.
 * Every write goes through the same [ReminderActions] the notification "પૂરું થયું" action and the
 * ReminderGlanceWidget DONE pill use, so alarms, notifications and widgets stay in step.
 */
interface ReminderStore {
    fun active(): Flow<List<ReminderEntity>>
    fun archived(): Flow<List<ReminderEntity>>
    suspend fun byId(id: Long): ReminderEntity?

    /** Insert ([ReminderEntity.id] == 0) or update, then re-arm its alarms. Returns the id. */
    suspend fun save(r: ReminderEntity): Long

    /** DONE: archive + cancel alarm + cancel notification + refresh widgets. */
    suspend fun markDone(id: Long)

    /** Undo / "ફરી ચાલુ કરો": back to active and re-armed. */
    suspend fun reopen(id: Long)

    suspend fun delete(id: Long)

    /** `Build.VERSION.SDK_INT < 31 || canScheduleExactAlarms()`. */
    fun canScheduleExact(): Boolean

    /** Removes a stored photo that is no longer referenced (replaced / discarded). */
    fun deletePhoto(path: String)
}

/** Real store on the shared app repositories ([NotifyGraph.repos], installed by VrajApp). */
class RepoReminderStore(context: Context) : ReminderStore {
    private val app = context.applicationContext
    private val repo: ReminderRepository get() = NotifyGraph.repos(app).reminders

    override fun active(): Flow<List<ReminderEntity>> = repo.active()
    override fun archived(): Flow<List<ReminderEntity>> = repo.archived()
    override suspend fun byId(id: Long): ReminderEntity? = repo.byId(id)

    override suspend fun save(r: ReminderEntity): Long {
        val saved = if (r.id == 0L) {
            val id = repo.add(r)
            r.copy(id = id)
        } else {
            repo.update(r)
            repo.byId(r.id) ?: r
        }
        ReminderActions.rearm(app, saved)
        return saved.id
    }

    override suspend fun markDone(id: Long) = ReminderActions.markDone(app, id)
    override suspend fun reopen(id: Long) = ReminderActions.reopen(app, id)
    override suspend fun delete(id: Long) = ReminderActions.delete(app, id)
    override fun canScheduleExact(): Boolean = NotifyGraph.alarmPort(app).canScheduleExact()

    override fun deletePhoto(path: String) {
        val f = File(path)
        runCatching { if (f.canonicalPath.startsWith(app.filesDir.canonicalPath)) f.delete() }
    }
}
