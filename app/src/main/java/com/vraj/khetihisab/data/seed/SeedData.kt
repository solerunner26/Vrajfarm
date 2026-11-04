package com.vraj.khetihisab.data.seed

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TxnType

/** Pure seed definitions (doc 03 "Seeding"). Applied once by [SeedCallback.onCreate]. */
object SeedData {
    data class FarmSeed(val name: String, val sortOrder: Int, val managerRole: ProfileRole)
    data class FamilySeed(val code: String, val displayName: String)
    data class ProfileSeed(val role: ProfileRole, val name: String, val address: String?)
    data class CategorySeed(
        val name: String,
        val section: AccountSection,
        val type: TxnType,
        val isPhysicalTool: Boolean = false,
    )

    const val OWNER_NAME = "ગિરવાનસિંહ વાળા"
    const val OWNER_ADDRESS = "મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર"

    /** ખેતર 1 … ખેતર 8, area 0.0 (set in Settings). 1–4 → MANAGER_1, 5–8 → MANAGER_2. */
    val farms: List<FarmSeed> = (1..8).map { n ->
        FarmSeed(
            name = "ખેતર $n",
            sortOrder = n,
            managerRole = if (n <= 4) ProfileRole.MANAGER_1 else ProfileRole.MANAGER_2,
        )
    }

    val families: List<FamilySeed> = listOf(
        FamilySeed("A", "ભાગિયા 1"),
        FamilySeed("B", "ભાગિયા 2"),
    )

    val profiles: List<ProfileSeed> = listOf(
        ProfileSeed(ProfileRole.OWNER, OWNER_NAME, OWNER_ADDRESS),
        ProfileSeed(ProfileRole.MANAGER_1, "", null),
        ProfileSeed(ProfileRole.MANAGER_2, "", null),
    )

    val crops: List<String> = listOf(
        "કપાસ", "મગફળી", "ડુંગળી", "ઘઉં", "બાજરી", "જીરું", "તલ", "ચણા", "એરંડા", "જુવાર",
    )

    val categories: List<CategorySeed> = buildList {
        val farmExp = AccountSection.FARM to TxnType.EXPENSE
        listOf("બિયારણ", "ખાતર", "દવા", "મજૂરી", "ડીઝલ", "ભાડું").forEach {
            add(CategorySeed(it, farmExp.first, farmExp.second))
        }
        add(CategorySeed("સાધન", AccountSection.FARM, TxnType.EXPENSE, isPhysicalTool = true))
        add(CategorySeed("પાણી", AccountSection.FARM, TxnType.EXPENSE))

        add(CategorySeed("પાક વેચાણ", AccountSection.FARM, TxnType.INCOME))

        // પશુપાલન (animal husbandry): counted in the business P&L like FARM.
        listOf("ઘાસચારો", "ખાણ-દાણ", "પશુ દવા / ડૉક્ટર", "પશુ ખરીદી", "અન્ય પશુ ખર્ચ").forEach {
            add(CategorySeed(it, AccountSection.ANIMAL, TxnType.EXPENSE))
        }
        listOf("દૂધ વેચાણ", "પશુ વેચાણ").forEach {
            add(CategorySeed(it, AccountSection.ANIMAL, TxnType.INCOME))
        }

        listOf("લાઇટ બિલ", "પાણી વ્યવસ્થા", "CCTV", "રિપેરિંગ").forEach {
            add(CategorySeed(it, AccountSection.GENERAL, TxnType.EXPENSE))
        }

        listOf("કરિયાણું", "શિક્ષણ", "દવા", "તહેવાર", "કપડાં", "અન્ય").forEach {
            add(CategorySeed(it, AccountSection.PERSONAL, TxnType.EXPENSE))
        }

        listOf("ભાડાની આવક", "અન્ય આવક").forEach {
            add(CategorySeed(it, AccountSection.PERSONAL, TxnType.INCOME))
        }

        add(CategorySeed("રોકડ ઉપાડ", AccountSection.FAMILY, TxnType.ADVANCE))
    }
}
