package com.vraj.khetihisab.ui.profiles

import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {

    /** Mirrors ProfileDao (one row per role) + MasterDao.setManagerFarms (exclusive). */
    private class FakeStore : ProfilesStore {
        val people = MutableStateFlow(
            listOf(
                PersonProfileEntity(1, ProfileRole.OWNER, "ગિરવાનસિંહ વાળા", null, "મુ. પો. થોરાળા", "/files/people_photos/owner_1.webp", 1L),
                PersonProfileEntity(2, ProfileRole.MANAGER_1, "", null, null, null, 1L),
            ),
        )
        val farmList = MutableStateFlow((1..8).map { FarmEntity(id = it.toLong(), name = "ખેતર $it", areaInVigha = 0.0, managerRole = if (it <= 4) ProfileRole.MANAGER_1 else null, sortOrder = it, createdAt = 0, updatedAt = 0) })

        override fun profiles(): Flow<List<PersonProfileEntity>> = people
        override fun farms(): Flow<List<FarmEntity>> = farmList
        override suspend fun byRole(role: ProfileRole) = people.value.firstOrNull { it.role == role }
        override suspend fun upsert(p: PersonProfileEntity) {
            val existing = byRole(p.role)
            val row = if (existing != null) p.copy(id = existing.id) else p.copy(id = (people.value.maxOf { it.id } + 1))
            people.value = people.value.filter { it.role != p.role } + row
        }
        override suspend fun updatePhoto(role: ProfileRole, path: String?) {
            people.value = people.value.map { if (it.role == role) it.copy(photoPath = path) else it }
        }
        override suspend fun setManagerFarms(role: ProfileRole, farmIds: Set<Long>) {
            farmList.value = farmList.value.map { f ->
                when {
                    f.id in farmIds -> f.copy(managerRole = role)
                    f.managerRole == role -> f.copy(managerRole = null)
                    else -> f
                }
            }
        }
    }

    private lateinit var store: FakeStore
    private val deleted = mutableListOf<String>()
    private lateinit var vm: ProfilesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = FakeStore()
        vm = ProfilesViewModel(store, clock = { 99L }, deletePhotoFile = { deleted += it })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun saveManagerTrimsAndAssignsFarmsExclusively() = runTest {
        val err = vm.save(ProfileEdit(ProfileRole.MANAGER_2, "  રમેશભાઈ ", " 98250 12345 ", "ignored", setOf(4L, 5L, 6L)))
        assertNull(err)
        val m2 = store.byRole(ProfileRole.MANAGER_2)!!
        assertEquals("રમેશભાઈ", m2.name)
        assertEquals("98250 12345", m2.phone)
        assertNull("address is owner-only", m2.address)
        assertEquals(99L, m2.updatedAt)
        val farms = store.farmList.value.associate { it.id to it.managerRole }
        assertEquals(ProfileRole.MANAGER_2, farms[4L])
        assertEquals(ProfileRole.MANAGER_2, farms[5L])
        assertEquals(ProfileRole.MANAGER_1, farms[1L])
        assertNull(farms[7L])
        // Manager 1 lost farm 4 (exclusive).
        assertEquals(listOf(1L, 2L, 3L), store.farmList.value.filter { it.managerRole == ProfileRole.MANAGER_1 }.map { it.id })
    }

    @Test
    fun saveOwnerKeepsPhotoAndAddress() = runTest {
        assertNull(vm.save(ProfileEdit(ProfileRole.OWNER, "ગિરવાનસિંહ વાળા", "", " મુ. પો. થોરાળા, તા. મહુવા ", emptySet())))
        val o = store.byRole(ProfileRole.OWNER)!!
        assertEquals(1L, o.id)
        assertEquals("મુ. પો. થોરાળા, તા. મહુવા", o.address)
        assertEquals("/files/people_photos/owner_1.webp", o.photoPath)
        assertNull(o.phone)
        // Owner save never touches farm assignment.
        assertEquals(4, store.farmList.value.count { it.managerRole == ProfileRole.MANAGER_1 })
    }

    @Test
    fun validation() {
        assertEquals(ProfileError.NAME, ProfilesViewModel.validate(ProfileEdit(ProfileRole.OWNER, " ", "", "", emptySet())))
        assertEquals(ProfileError.PHONE, ProfilesViewModel.validate(ProfileEdit(ProfileRole.MANAGER_1, "રમેશ", "12345", "", emptySet())))
        assertEquals(ProfileError.NAME, ProfilesViewModel.validate(ProfileEdit(ProfileRole.MANAGER_1, "", "9825012345", "", emptySet())))
        assertNull(ProfilesViewModel.validate(ProfileEdit(ProfileRole.MANAGER_1, "", "", "", emptySet())))
        assertNull(ProfilesViewModel.validate(ProfileEdit(ProfileRole.MANAGER_1, "રમેશ", "+91 98250 12345", "", setOf(1L))))
    }

    @Test
    fun newPhotoReplacesAndDeletesOld_failedKeepsOld() = runTest {
        val events = mutableListOf<ProfileEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.onPhoto(ProfileRole.OWNER, null)
        assertEquals("/files/people_photos/owner_1.webp", store.byRole(ProfileRole.OWNER)!!.photoPath)
        assertEquals(listOf<ProfileEvent>(ProfileEvent.PhotoFailed), events)

        vm.onPhoto(ProfileRole.OWNER, "/files/people_photos/owner_2.webp")
        assertEquals("/files/people_photos/owner_2.webp", store.byRole(ProfileRole.OWNER)!!.photoPath)
        assertEquals(listOf("/files/people_photos/owner_1.webp"), deleted)

        vm.removePhoto(ProfileRole.OWNER)
        assertNull(store.byRole(ProfileRole.OWNER)!!.photoPath)
        assertEquals(2, deleted.size)

        // A missing row is created before the photo update.
        vm.onPhoto(ProfileRole.MANAGER_2, "/files/people_photos/manager_2_1.webp")
        assertEquals("/files/people_photos/manager_2_1.webp", store.byRole(ProfileRole.MANAGER_2)!!.photoPath)
        job.cancel()
    }

    @Test
    fun uiGroupsOwnerManagersAndFarms() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        val ui = vm.ui.first { it.owner != null }
        assertEquals(ProfileRole.OWNER, ui.owner!!.role)
        assertEquals(listOf(ProfileRole.MANAGER_1, ProfileRole.MANAGER_2), ui.managers.map { it.role })
        assertEquals(4, ui.managers[0].farms.size)
        assertTrue(ui.managers[1].farms.isEmpty())
        assertEquals(8, ui.allFarms.size)
        job.cancel()
    }

    @Test
    fun centreSquareAndSampling() {
        assertEquals(Triple(240, 0, 1080), ProfilePhoto.centreSquare(1560, 1080))
        assertEquals(Triple(0, 100, 600), ProfilePhoto.centreSquare(600, 800))
        assertEquals(4, ProfilePhoto.sampleFor(4000, 3000))
        assertEquals(1, ProfilePhoto.sampleFor(600, 600))
        assertTrue(ProfilePhoto.fileName(ProfileRole.MANAGER_1, 5L).startsWith("manager_1_"))
    }
}
