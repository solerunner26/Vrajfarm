package com.vraj.khetihisab.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vraj.khetihisab.data.TestDb.NOW
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = TestDb.create()
    }

    @After fun tearDown() = db.close()

    private fun profile(role: ProfileRole, name: String, phone: String? = null) =
        PersonProfileEntity(role = role, name = name, phone = phone, address = null, photoPath = null, updatedAt = NOW)

    @Test fun uniqueRoles() = runBlocking<Unit> {
        val dao = db.profileDao()
        dao.upsert(profile(ProfileRole.OWNER, "ગિરવાનસિંહ વાળા"))
        dao.upsert(profile(ProfileRole.OWNER, "ગિરવાનસિંહ વાળા", phone = "9800000000"))

        val all = dao.all().first()
        assertEquals(1, all.size)
        // The second upsert (id = 0) must update the existing row, not be silently dropped.
        assertEquals("9800000000", all.single().phone)
    }

    @Test fun oneOwnerAndAtMostTwoManagers() = runBlocking<Unit> {
        val dao = db.profileDao()
        dao.upsert(profile(ProfileRole.OWNER, "માલિક"))
        dao.upsert(profile(ProfileRole.MANAGER_1, "મેનેજર 1"))
        dao.upsert(profile(ProfileRole.MANAGER_2, "મેનેજર 2"))
        dao.upsert(profile(ProfileRole.MANAGER_1, "નવા મેનેજર 1"))
        dao.upsert(profile(ProfileRole.MANAGER_2, "નવા મેનેજર 2"))

        val all = dao.all().first()
        assertEquals(listOf(ProfileRole.OWNER, ProfileRole.MANAGER_1, ProfileRole.MANAGER_2), all.map { it.role })
        assertEquals(listOf("માલિક", "નવા મેનેજર 1", "નવા મેનેજર 2"), all.map { it.name })
    }

    @Test fun seededProfilesUpdateInPlace() = runBlocking<Unit> {
        val seeded = TestDb.create(seed = true)
        try {
            val dao = seeded.profileDao()
            val ownerId = dao.byRoleNow(ProfileRole.OWNER)!!.id
            dao.upsert(profile(ProfileRole.MANAGER_1, "રાજુભાઈ", phone = "9811111111"))
            dao.updatePhoto(ProfileRole.OWNER, "people_photos/OWNER.webp", NOW)

            assertEquals(3, dao.count())
            assertEquals("રાજુભાઈ", dao.byRoleNow(ProfileRole.MANAGER_1)!!.name)
            assertEquals(ownerId, dao.byRoleNow(ProfileRole.OWNER)!!.id)
            assertEquals("people_photos/OWNER.webp", dao.byRoleNow(ProfileRole.OWNER)!!.photoPath)
        } finally {
            seeded.close()
        }
    }

    @Test fun managerFarmAssignmentIsExclusive() = runBlocking<Unit> {
        val seeded = TestDb.create(seed = true)
        try {
            val master = seeded.masterDao()
            val farms = master.farmsNow()
            // Give farms 1..5 to MANAGER_1 — farm 5 moves away from MANAGER_2.
            master.setManagerFarms(ProfileRole.MANAGER_1, farms.take(5).map { it.id }.toSet(), NOW)

            assertEquals(5, master.farmsForManager(ProfileRole.MANAGER_1).first().size)
            assertEquals(listOf("ખેતર 6", "ખેતર 7", "ખેતર 8"), master.farmsForManager(ProfileRole.MANAGER_2).first().map { it.name })
        } finally {
            seeded.close()
        }
    }
}
