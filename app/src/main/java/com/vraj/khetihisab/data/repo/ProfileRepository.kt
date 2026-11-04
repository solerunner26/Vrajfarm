package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.dao.MasterDao
import com.vraj.khetihisab.data.dao.ProfileDao
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import kotlinx.coroutines.flow.Flow

/**
 * Owner + two ભાગિયા (farm-manager) profiles (doc 14). Farm assignment lives in [MasterRepository.setManagerFarms].
 *
 * ભાગિયા = farm manager = the old "પરિવાર A/B": ONE person, edited only here. The ledger keeps its
 * `family` rows (A ↔ [ProfileRole.MANAGER_1], B ↔ [ProfileRole.MANAGER_2]) as the advance account key,
 * and [syncBhagiya] mirrors the profile's name and phone into them, so there is no second place to edit.
 */
class ProfileRepository(
    private val dao: ProfileDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
    private val master: MasterDao? = null,
) {
    fun all(): Flow<List<PersonProfileEntity>> = dao.all()
    fun owner(): Flow<PersonProfileEntity?> = dao.byRole(ProfileRole.OWNER)
    fun byRole(role: ProfileRole): Flow<PersonProfileEntity?> = dao.byRole(role)
    suspend fun byRoleNow(role: ProfileRole): PersonProfileEntity? = dao.byRoleNow(role)

    /** Insert-or-update keyed by role (never creates a second row for a role). */
    suspend fun upsert(p: PersonProfileEntity) {
        dao.upsert(p.copy(updatedAt = clock()))
        if (p.role != ProfileRole.OWNER) syncBhagiya(p.role)
        listener.onDataChanged()
    }

    suspend fun updatePhoto(role: ProfileRole, path: String?) {
        dao.updatePhoto(role, path, clock())
        listener.onDataChanged()
    }

    /** Saves a ભાગિયા's WhatsApp number from the ઉપાડ screen (same person, same field). */
    suspend fun updateBhagiyaPhone(familyCode: String, phone: String?) {
        val role = roleForFamilyCode(familyCode) ?: return
        val now = clock()
        val p = dao.byRoleNow(role) ?: PersonProfileEntity(role = role, name = "", phone = null, address = null, photoPath = null, updatedAt = now)
        dao.upsert(p.copy(phone = phone?.trim()?.ifEmpty { null }, updatedAt = now))
        syncBhagiya(role)
        listener.onDataChanged()
    }

    /**
     * Mirrors manager profiles into the `family` rows. A phone typed earlier on the family row and
     * missing on the profile is first copied to the profile, so no number is lost by the merge.
     */
    suspend fun syncBhagiya(only: ProfileRole? = null) {
        val m = master ?: return
        for (role in listOf(ProfileRole.MANAGER_1, ProfileRole.MANAGER_2)) {
            if (only != null && only != role) continue
            val fam = m.familyByCode(familyCodeFor(role)) ?: continue
            var p = dao.byRoleNow(role)
            if (p != null && p.phone.isNullOrBlank() && !fam.whatsappPhone.isNullOrBlank()) {
                p = p.copy(phone = fam.whatsappPhone, updatedAt = clock())
                dao.upsert(p)
            }
            val name = p?.name?.trim().orEmpty().ifEmpty { defaultName(role) }
            val phone = p?.phone?.trim()?.ifEmpty { null } ?: fam.whatsappPhone
            if (fam.displayName != name || fam.whatsappPhone != phone) {
                m.updateFamily(fam.copy(displayName = name, whatsappPhone = phone, updatedAt = clock()))
            }
        }
    }

    companion object {
        fun familyCodeFor(role: ProfileRole): String = if (role == ProfileRole.MANAGER_2) "B" else "A"
        fun roleForFamilyCode(code: String): ProfileRole? = when (code.uppercase()) {
            "A" -> ProfileRole.MANAGER_1
            "B" -> ProfileRole.MANAGER_2
            else -> null
        }
        fun defaultName(role: ProfileRole): String = if (role == ProfileRole.MANAGER_2) "ભાગિયા 2" else "ભાગિયા 1"
    }
}
