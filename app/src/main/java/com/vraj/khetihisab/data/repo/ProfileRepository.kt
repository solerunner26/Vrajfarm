package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.dao.ProfileDao
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import kotlinx.coroutines.flow.Flow

/** Owner + two manager profiles (doc 14). Farm assignment lives in [MasterRepository.setManagerFarms]. */
class ProfileRepository(
    private val dao: ProfileDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
) {
    fun all(): Flow<List<PersonProfileEntity>> = dao.all()
    fun owner(): Flow<PersonProfileEntity?> = dao.byRole(ProfileRole.OWNER)
    fun byRole(role: ProfileRole): Flow<PersonProfileEntity?> = dao.byRole(role)
    suspend fun byRoleNow(role: ProfileRole): PersonProfileEntity? = dao.byRoleNow(role)

    /** Insert-or-update keyed by role (never creates a second row for a role). */
    suspend fun upsert(p: PersonProfileEntity) {
        dao.upsert(p.copy(updatedAt = clock()))
        listener.onDataChanged()
    }

    suspend fun updatePhoto(role: ProfileRole, path: String?) {
        dao.updatePhoto(role, path, clock())
        listener.onDataChanged()
    }
}
