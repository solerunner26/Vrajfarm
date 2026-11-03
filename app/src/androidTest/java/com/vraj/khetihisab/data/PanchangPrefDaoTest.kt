package com.vraj.khetihisab.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vraj.khetihisab.data.TestDb.NOW
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.repo.PanchangPrefRepository
import com.vraj.khetihisab.data.seed.PanchangDefaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PanchangPrefDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: PanchangPrefRepository

    @Before fun setUp() {
        db = TestDb.create(seed = true)
        repo = PanchangPrefRepository(db.panchangDao(), clock = { NOW + 1 })
    }

    @After fun tearDown() = db.close()

    @Test fun seededDefaultsAllEnabledWithBeliefGuidance() = runBlocking<Unit> {
        val prefs = db.panchangDao().prefs().first().associateBy { it.type }
        assertEquals(5, prefs.size)
        PanchangAlertType.entries.forEach { t ->
            val p = prefs.getValue(t)
            assertTrue(p.enabled && p.notifyEveningBefore && p.showDialogOnDay)
            assertEquals(PanchangDefaults.doText(t), p.doText)
            assertEquals(PanchangDefaults.avoidText(t), p.avoidText)
        }
        assertTrue(prefs.getValue(PanchangAlertType.AMAS).avoidText.contains("નવી વાવણી શરૂ કરવી"))
    }

    @Test fun guidanceEditPersistsAndResetRestoresDefaults() = runBlocking<Unit> {
        repo.updateGuidance(PanchangAlertType.AMAS, "મારું કામ", "મારું ટાળવું")
        repo.setEnabled(PanchangAlertType.AMAS, false)

        val edited = repo.prefsMapNow().getValue(PanchangAlertType.AMAS)
        assertEquals("મારું કામ", edited.doText)
        assertEquals("મારું ટાળવું", edited.avoidText)
        assertFalse(edited.enabled)

        repo.resetGuidance(PanchangAlertType.AMAS)
        val reset = repo.prefsMap().first().getValue(PanchangAlertType.AMAS)
        assertEquals(PanchangDefaults.doText(PanchangAlertType.AMAS), reset.doText)
        assertEquals(PanchangDefaults.avoidText(PanchangAlertType.AMAS), reset.avoidText)
        assertFalse("reset keeps the on/off switch", reset.enabled)
    }

    @Test fun upsertPrefKeepsOneRowPerType() = runBlocking<Unit> {
        val dao = db.panchangDao()
        dao.upsertPref(PanchangDefaults.defaultPref(PanchangAlertType.BIJ, NOW).copy(showDialogOnDay = false))
        assertEquals(5, dao.prefsNow().size)
        assertFalse(dao.pref(PanchangAlertType.BIJ)!!.showDialogOnDay)
    }

    @Test fun customFestivalsOnlyEnabled() = runBlocking<Unit> {
        val dao = db.panchangDao()
        dao.upsertFestival(CustomFestivalEntity(nameGu = "ઉત્તરાયણ", monthIndex = null, paksha = null, tithiInPaksha = null, fixedMonthDay = "01-14", note = null))
        val off = dao.upsertFestival(CustomFestivalEntity(nameGu = "ગામ મેળો", monthIndex = 6, paksha = "SUD", tithiInPaksha = 5, fixedMonthDay = null, note = null, enabled = false))

        assertEquals(listOf("ઉત્તરાયણ"), dao.customFestivals().map { it.nameGu })
        assertEquals(2, dao.allCustomFestivals().first().size)

        dao.deleteFestival(off)
        assertEquals(1, dao.allCustomFestivals().first().size)
    }
}
