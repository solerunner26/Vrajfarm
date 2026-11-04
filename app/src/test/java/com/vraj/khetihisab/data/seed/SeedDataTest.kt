package com.vraj.khetihisab.data.seed

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {
    @Test fun eightFarmsSplitBetweenManagers() {
        assertEquals((1..8).map { "ખેતર $it" }, SeedData.farms.map { it.name })
        assertTrue(SeedData.farms.take(4).all { it.managerRole == ProfileRole.MANAGER_1 })
        assertTrue(SeedData.farms.drop(4).all { it.managerRole == ProfileRole.MANAGER_2 })
    }

    @Test fun familiesAAndB() {
        assertEquals(listOf("A" to "ભાગિયા 1", "B" to "ભાગિયા 2"), SeedData.families.map { it.code to it.displayName })
    }

    @Test fun ownerAndTwoBlankManagers() {
        val owner = SeedData.profiles.single { it.role == ProfileRole.OWNER }
        assertEquals("ગિરવાનસિંહ વાળા", owner.name)
        assertEquals("મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર", owner.address)
        val managers = SeedData.profiles.filter { it.role != ProfileRole.OWNER }
        assertEquals(setOf(ProfileRole.MANAGER_1, ProfileRole.MANAGER_2), managers.map { it.role }.toSet())
        assertTrue(managers.all { it.name.isEmpty() })
    }

    @Test fun cropsAreUnique() {
        assertEquals(10, SeedData.crops.size)
        assertEquals(SeedData.crops.size, SeedData.crops.toSet().size)
    }

    @Test fun categoriesPerSection() {
        fun names(s: AccountSection, t: TxnType) = SeedData.categories.filter { it.section == s && it.type == t }.map { it.name }
        assertEquals(
            listOf("બિયારણ", "ખાતર", "દવા", "મજૂરી", "ડીઝલ", "ભાડું", "સાધન", "પાણી"),
            names(AccountSection.FARM, TxnType.EXPENSE),
        )
        assertEquals(listOf("પાક વેચાણ"), names(AccountSection.FARM, TxnType.INCOME))
        assertEquals(listOf("લાઇટ બિલ", "પાણી વ્યવસ્થા", "CCTV", "રિપેરિંગ"), names(AccountSection.GENERAL, TxnType.EXPENSE))
        assertEquals(6, names(AccountSection.PERSONAL, TxnType.EXPENSE).size)
        assertEquals(listOf("ભાડાની આવક", "અન્ય આવક"), names(AccountSection.PERSONAL, TxnType.INCOME))
        assertEquals(listOf("રોકડ ઉપાડ"), names(AccountSection.FAMILY, TxnType.ADVANCE))
        assertEquals(
            listOf("ઘાસચારો", "ખાણ-દાણ", "પશુ દવા / ડૉક્ટર", "પશુ ખરીદી", "અન્ય પશુ ખર્ચ"),
            names(AccountSection.ANIMAL, TxnType.EXPENSE),
        )
        assertEquals(listOf("દૂધ વેચાણ", "પશુ વેચાણ"), names(AccountSection.ANIMAL, TxnType.INCOME))
        assertEquals(listOf("સાધન"), SeedData.categories.filter { it.isPhysicalTool }.map { it.name })
        assertEquals(29, SeedData.categories.size)
    }

    @Test fun panchangDefaultsCoverAllTypesEnabled() {
        val prefs = PanchangDefaults.all(now = 1L)
        assertEquals(PanchangAlertType.entries.toSet(), prefs.map { it.type }.toSet())
        assertTrue(prefs.all { it.enabled && it.notifyEveningBefore && it.showDialogOnDay })
        assertTrue(prefs.all { it.doText.isNotBlank() })
    }

    @Test fun panchangGuidanceTexts() {
        assertEquals(
            listOf("નવી વાવણી શરૂ કરવી", "નવું સાધન કે જમીન ખરીદવી", "નવું કામ શરૂ કરવું"),
            PanchangDefaults.lines(PanchangDefaults.avoidText(PanchangAlertType.AMAS)),
        )
        assertEquals(listOf("ઉપવાસ", "ભજન-કીર્તન", "દાન"), PanchangDefaults.lines(PanchangDefaults.doText(PanchangAlertType.AGIYARAS)))
        assertEquals("", PanchangDefaults.avoidText(PanchangAlertType.POONAM))
        assertEquals("માન્યતા પ્રમાણે · સેટિંગ્સમાં બદલી શકો", PanchangDefaults.BELIEF_NOTE)
    }

    @Test fun linesTrimsAndDedupes() {
        assertEquals(listOf("a", "b"), PanchangDefaults.lines(" a \n\nb\na\n "))
    }
}
