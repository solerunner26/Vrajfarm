package com.vraj.khetihisab.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.repo.MasterRepository
import com.vraj.khetihisab.data.repo.ProfileRepository
import com.vraj.khetihisab.share.PhoneNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Data port of the Profiles screen (JVM-testable with a fake). */
interface ProfilesStore {
    fun profiles(): Flow<List<PersonProfileEntity>>
    fun farms(): Flow<List<FarmEntity>>
    suspend fun byRole(role: ProfileRole): PersonProfileEntity?
    suspend fun upsert(p: PersonProfileEntity)
    suspend fun updatePhoto(role: ProfileRole, path: String?)
    /** Exclusive: assigning a farm to one manager unassigns it from the other (MasterDao.setManagerFarms). */
    suspend fun setManagerFarms(role: ProfileRole, farmIds: Set<Long>)
}

class RepoProfilesStore(private val profiles: ProfileRepository, private val master: MasterRepository) : ProfilesStore {
    override fun profiles() = profiles.all()
    override fun farms() = master.farms()
    override suspend fun byRole(role: ProfileRole) = profiles.byRoleNow(role)
    override suspend fun upsert(p: PersonProfileEntity) = profiles.upsert(p)
    override suspend fun updatePhoto(role: ProfileRole, path: String?) = profiles.updatePhoto(role, path)
    override suspend fun setManagerFarms(role: ProfileRole, farmIds: Set<Long>) = master.setManagerFarms(role, farmIds)
}

/** One card on the screen. */
data class PersonCard(
    val role: ProfileRole,
    val name: String,
    val phone: String?,
    val address: String?,
    val photoPath: String?,
    val updatedAt: Long,
    val farms: List<FarmEntity>,
) {
    val hasPhone: Boolean get() = !phone.isNullOrBlank()
}

data class ProfilesUi(
    val owner: PersonCard? = null,
    val managers: List<PersonCard> = emptyList(),
    val allFarms: List<FarmEntity> = emptyList(),
)

/** Edit dialog input. [farmIds] is ignored for the owner; [address] is owner-only. */
data class ProfileEdit(
    val role: ProfileRole,
    val name: String,
    val phone: String,
    val address: String,
    val farmIds: Set<Long>,
)

enum class ProfileError { NAME, PHONE }

sealed interface ProfileEvent {
    data object Saved : ProfileEvent
    data object PhotoFailed : ProfileEvent
}

/**
 * doc 14 ProfilesScreen: owner + two managers, photos, exclusive farm assignment.
 * Writes go through [ProfilesStore]; Home, Help and WhatsApp signatures read the same rows live.
 */
class ProfilesViewModel(
    private val store: ProfilesStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val deletePhotoFile: (String) -> Unit = {},
) : ViewModel() {

    val ui: StateFlow<ProfilesUi> = combine(store.profiles(), store.farms()) { people, farms ->
        fun card(role: ProfileRole): PersonCard {
            val p = people.firstOrNull { it.role == role }
            return PersonCard(
                role = role,
                name = p?.name.orEmpty(),
                phone = p?.phone,
                address = p?.address,
                photoPath = p?.photoPath,
                updatedAt = p?.updatedAt ?: 0L,
                farms = farms.filter { it.managerRole == role }.sortedBy { it.sortOrder },
            )
        }
        ProfilesUi(
            owner = card(ProfileRole.OWNER),
            managers = listOf(card(ProfileRole.MANAGER_1), card(ProfileRole.MANAGER_2)),
            allFarms = farms.sortedBy { it.sortOrder },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUi())

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    /** Validates; on success saves (trimmed, phone kept as typed) and returns null. */
    fun save(e: ProfileEdit): ProfileError? {
        val error = validate(e)
        if (error != null) return error
        viewModelScope.launch {
            val existing = store.byRole(e.role)
            store.upsert(
                PersonProfileEntity(
                    id = existing?.id ?: 0,
                    role = e.role,
                    name = e.name.trim(),
                    phone = e.phone.trim().ifEmpty { null },
                    address = if (e.role == ProfileRole.OWNER) e.address.trim().ifEmpty { null } else existing?.address,
                    photoPath = existing?.photoPath,
                    updatedAt = clock(),
                ),
            )
            if (e.role != ProfileRole.OWNER) store.setManagerFarms(e.role, e.farmIds)
            _events.emit(ProfileEvent.Saved)
        }
        return null
    }

    /** New processed photo at [path] (or null = processing failed: keep the old photo, doc 14 edge case). */
    fun onPhoto(role: ProfileRole, path: String?) {
        viewModelScope.launch {
            if (path == null) {
                _events.emit(ProfileEvent.PhotoFailed)
                return@launch
            }
            val old = store.byRole(role)?.photoPath
            ensureRow(role)
            store.updatePhoto(role, path)
            if (old != null && old != path) deletePhotoFile(old)
        }
    }

    fun removePhoto(role: ProfileRole) {
        viewModelScope.launch {
            val old = store.byRole(role)?.photoPath ?: return@launch
            store.updatePhoto(role, null)
            deletePhotoFile(old)
        }
    }

    /** updatePhoto is an UPDATE: make sure the role's row exists (seeded normally). */
    private suspend fun ensureRow(role: ProfileRole) {
        if (store.byRole(role) == null) {
            store.upsert(PersonProfileEntity(role = role, name = "", phone = null, address = null, photoPath = null, updatedAt = clock()))
        }
    }

    companion object {
        fun validate(e: ProfileEdit): ProfileError? {
            if (e.role == ProfileRole.OWNER && e.name.isBlank()) return ProfileError.NAME
            if (e.role != ProfileRole.OWNER && e.name.isBlank() && (e.phone.isNotBlank() || e.farmIds.isNotEmpty())) return ProfileError.NAME
            if (e.phone.isNotBlank() && PhoneNormalizer.toE164NoPlus(e.phone) == null) return ProfileError.PHONE
            return null
        }
    }
}
