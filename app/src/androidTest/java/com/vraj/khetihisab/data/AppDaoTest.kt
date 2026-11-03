package com.vraj.khetihisab.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vraj.khetihisab.data.TestDb.NOW
import com.vraj.khetihisab.data.TestDb.family
import com.vraj.khetihisab.data.TestDb.farm
import com.vraj.khetihisab.data.TestDb.txn
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.MonthTotal
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.data.repo.LedgerMath
import com.vraj.khetihisab.data.seed.SeedCallback
import com.vraj.khetihisab.data.seed.SeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppDaoTest {
    private lateinit var db: AppDatabase
    private val from = LocalDate.of(2026, 1, 1)
    private val to = LocalDate.of(2026, 12, 31)

    @Before fun setUp() {
        db = TestDb.create()
    }

    @After fun tearDown() = db.close()

    // ---------------- isolation: farm P&L ----------------

    @Test fun personalExcludedFromFarmPnl() = runBlocking<Unit> {
        val farmId = db.masterDao().insertFarm(farm("ખેતર 1", area = 2.0))
        val txn = db.txnDao()
        txn.insert(txn(AccountSection.FARM, TxnType.INCOME, 1_000, farmId = farmId))
        // A PERSONAL row even carrying a farmId must never reach the farm P&L.
        txn.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 500, farmId = farmId))
        txn.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 700))

        val row = txn.farmPnl(from, to).first().single()
        assertEquals(1_000_00L, row.incomePaise)
        assertEquals(0L, row.expensePaise)
        assertEquals(1_000_00L, row.netPaise)

        val totals = txn.farmTotals(from, to).first()
        assertEquals(1_000_00L, totals.income)
        assertEquals(0L, totals.expense)
        assertEquals(1_000_00L, totals.net)

        assertEquals(1_200_00L, txn.personalTotals(from, to).first().expense)
    }

    @Test fun familyAndGeneralNeverChangeFarmProfit() = runBlocking<Unit> {
        val farmId = db.masterDao().insertFarm(farm("ખેતર 1"))
        val famId = db.masterDao().insertFamily(family("A"))
        val txn = db.txnDao()
        txn.insert(txn(AccountSection.FARM, TxnType.INCOME, 5_000, farmId = farmId))
        txn.insert(txn(AccountSection.FARM, TxnType.EXPENSE, 2_000, farmId = farmId))
        txn.insert(txn(AccountSection.GENERAL, TxnType.EXPENSE, 900, farmId = farmId))
        txn.insert(txn(AccountSection.FAMILY, TxnType.ADVANCE, 3_000, farmId = farmId, familyId = famId))
        txn.insert(txn(AccountSection.FAMILY, TxnType.EXPENSE, 400, familyId = famId))

        val row = txn.farmPnl(from, to).first().single()
        assertEquals(5_000_00L, row.incomePaise)
        assertEquals(2_000_00L, row.expensePaise)
        assertEquals(3_000_00L, txn.farmTotals(from, to).first().net)
        // GENERAL shows on its own card.
        assertEquals(900_00L, txn.generalExpense(from, to).first())
    }

    @Test fun farmPnlFiltersDateRangeAndSoftDelete() = runBlocking<Unit> {
        val farmId = db.masterDao().insertFarm(farm("ખેતર 1"))
        val txn = db.txnDao()
        txn.insert(txn(AccountSection.FARM, TxnType.INCOME, 100, farmId = farmId, date = LocalDate.of(2025, 12, 31)))
        val deleted = txn.insert(txn(AccountSection.FARM, TxnType.INCOME, 200, farmId = farmId))
        txn.insert(txn(AccountSection.FARM, TxnType.INCOME, 300, farmId = farmId, date = from))
        txn.softDelete(deleted, NOW)

        assertEquals(300_00L, txn.farmPnl(from, to).first().single().incomePaise)
        assertEquals(300_00L, txn.farmTotals(from, to).first().income)

        txn.restore(deleted, NOW)
        assertEquals(500_00L, txn.farmTotals(from, to).first().income)
    }

    @Test fun farmPnlListsEveryFarmInSortOrderWithZeros() = runBlocking<Unit> {
        val master = db.masterDao()
        master.insertFarm(farm("ખેતર 2", sortOrder = 2))
        val f1 = master.insertFarm(farm("ખેતર 1", sortOrder = 1))
        db.txnDao().insert(txn(AccountSection.FARM, TxnType.EXPENSE, 50, farmId = f1))

        val rows = db.txnDao().farmPnl(from, to).first()
        assertEquals(listOf("ખેતર 1", "ખેતર 2"), rows.map { it.name })
        assertEquals(0L, rows[1].incomePaise)
        assertEquals(0L, rows[1].expensePaise)
        // area 0 → profit per vigha 0
        assertEquals(0L, LedgerMath.profitPerVigha(rows[0]))
    }

    @Test fun deletedFarmCascadesCyclesAndKeepsTxnWithNullFarm() = runBlocking<Unit> {
        val master = db.masterDao()
        val farmId = master.insertFarm(farm("ખેતર 1"))
        val cropId = master.insertCrop(CropMasterEntity(name = "કપાસ"))
        master.startCycle(FarmCropCycleEntity(farmId = farmId, cropId = cropId, season = "KHARIF", year = 2026, startDate = from, endDate = null, isActive = true))
        val tId = db.txnDao().insert(txn(AccountSection.FARM, TxnType.INCOME, 1_000, farmId = farmId))
        assertEquals("કપાસ", master.activeCycleFor(farmId).first()?.crop?.name)

        master.deleteFarm(master.farmById(farmId)!!)

        assertTrue(master.cyclesFor(farmId).first().isEmpty())
        assertNull(db.txnDao().byId(tId)!!.farmId)
        assertEquals(1_000_00L, db.txnDao().unassignedFarmTotals(from, to).first().income)
        assertTrue(db.txnDao().farmPnl(from, to).first().isEmpty())
    }

    // ---------------- family isolation ----------------

    @Test fun familyIsolation() = runBlocking<Unit> {
        val a = db.masterDao().insertFamily(family("A"))
        val b = db.masterDao().insertFamily(family("B"))
        val txn = db.txnDao()
        val bBefore = txn.familyAdvanceTotal(b, from, to).first()

        txn.insert(txn(AccountSection.FAMILY, TxnType.ADVANCE, 5_000, familyId = a))

        assertEquals(5_000_00L, txn.familyAdvanceTotal(a, from, to).first())
        assertEquals(bBefore, txn.familyAdvanceTotal(b, from, to).first())
        assertEquals(0L, txn.familyAdvanceTotal(b, from, to).first())
        assertEquals(1, txn.familyEntries(a, from, to).first().size)
        assertTrue(txn.familyEntries(b, from, to).first().isEmpty())
    }

    @Test fun familyAdvanceByMonth() = runBlocking<Unit> {
        val a = db.masterDao().insertFamily(family("A"))
        val txn = db.txnDao()
        txn.insert(txn(AccountSection.FAMILY, TxnType.ADVANCE, 1_000, familyId = a, date = LocalDate.of(2026, 1, 31)))
        txn.insert(txn(AccountSection.FAMILY, TxnType.ADVANCE, 2_000, familyId = a, date = LocalDate.of(2026, 1, 1)))
        txn.insert(txn(AccountSection.FAMILY, TxnType.ADVANCE, 500, familyId = a, date = LocalDate.of(2026, 11, 3)))
        // not an advance → excluded
        txn.insert(txn(AccountSection.FAMILY, TxnType.EXPENSE, 999, familyId = a, date = LocalDate.of(2026, 11, 3)))

        assertEquals(
            listOf(MonthTotal("2026-01", 3_000_00), MonthTotal("2026-11", 500_00)),
            txn.familyAdvanceByMonth(a, from, to).first(),
        )
        val nov = DateRange.month(java.time.YearMonth.of(2026, 11))
        assertEquals(500_00L, txn.familyAdvanceTotal(a, nov.from, nov.to).first())
    }

    // ---------------- udhaar ----------------

    @Test fun pendingUdhaarExcludesPersonal() = runBlocking<Unit> {
        val txn = db.txnDao()
        txn.insert(txn(AccountSection.FARM, TxnType.EXPENSE, 1_000, payStatus = PayStatus.PENDING_PAYABLE))
        txn.insert(txn(AccountSection.GENERAL, TxnType.EXPENSE, 300, payStatus = PayStatus.PENDING_PAYABLE))
        txn.insert(txn(AccountSection.FARM, TxnType.INCOME, 2_000, payStatus = PayStatus.PENDING_RECEIVABLE))
        txn.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 9_999, payStatus = PayStatus.PENDING_PAYABLE))
        txn.insert(txn(AccountSection.PERSONAL, TxnType.INCOME, 8_888, payStatus = PayStatus.PENDING_RECEIVABLE))
        txn.insert(txn(AccountSection.FARM, TxnType.EXPENSE, 50)) // PAID

        val u = txn.pendingUdhaar().first()
        assertEquals(1_300_00L, u.payable)
        assertEquals(2_000_00L, u.receivable)
        assertEquals(3, u.count)
        assertEquals(3, txn.pendingList().first().size)
        assertTrue(txn.pendingList().first().none { it.section == AccountSection.PERSONAL })

        val p = txn.personalUdhaar().first()
        assertEquals(9_999_00L, p.payable)
        assertEquals(8_888_00L, p.receivable)
        assertEquals(2, txn.personalPendingList().first().size)
    }

    @Test fun settleRemovesFromUdhaar() = runBlocking<Unit> {
        val txn = db.txnDao()
        val id = txn.insert(txn(AccountSection.FARM, TxnType.EXPENSE, 1_000, payStatus = PayStatus.PENDING_PAYABLE))
        txn.settle(id, NOW)
        val u = txn.pendingUdhaar().first()
        assertEquals(0L, u.payable)
        assertEquals(0, u.count)
        assertTrue(u.isEmpty)
    }

    @Test fun personalByCategoryAndRecent() = runBlocking<Unit> {
        val master = db.masterDao()
        val groc = master.insertCategory(
            com.vraj.khetihisab.data.entity.CategoryMasterEntity(name = "કરિયાણું", section = AccountSection.PERSONAL, type = TxnType.EXPENSE),
        )
        val txn = db.txnDao()
        txn.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 400, categoryId = groc))
        txn.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 600, categoryId = groc))
        txn.insert(txn(AccountSection.FARM, TxnType.EXPENSE, 10_000))

        val cats = txn.personalByCategory(from, to).first()
        assertEquals(1, cats.size)
        assertEquals(1_000_00L, cats.single().total)
        assertTrue(txn.personalRecent(20).first().all { it.section == AccountSection.PERSONAL })
        assertEquals(3, txn.recent(20).first().size)
    }

    // ---------------- shopping ----------------

    @Test fun personalShoppingNeverInFarmTabAndBoughtUndo() = runBlocking<Unit> {
        val shop = db.shoppingDao()
        val day = LocalDate.of(2026, 11, 3)
        fun item(tab: ShopTab, name: String) =
            ShoppingItemEntity(tab = tab, name = name, forDate = day, farmId = null, estPricePaise = 100_00, createdAt = NOW, updatedAt = NOW)
        val dap = shop.insert(item(ShopTab.FARM, "DAP ખાતર 4 થેલી"))
        shop.insert(item(ShopTab.PERSONAL, "કરિયાણું"))

        assertEquals(listOf("DAP ખાતર 4 થેલી"), shop.active(ShopTab.FARM).first().map { it.name })
        assertEquals(listOf("કરિયાણું"), shop.active(ShopTab.PERSONAL).first().map { it.name })
        assertEquals(2, shop.activeCount().first())

        shop.setBought(dap, true, NOW)
        assertTrue(shop.active(ShopTab.FARM).first().isEmpty())
        assertEquals(1, shop.activeCount().first())

        shop.setBought(dap, false, null) // undo
        assertEquals(1, shop.active(ShopTab.FARM).first().size)
        assertNull(shop.byId(dap)!!.boughtAt)

        shop.softDelete(dap, NOW)
        assertTrue(shop.active(ShopTab.FARM).first().isEmpty())
    }

    // ---------------- reminders ----------------

    @Test fun remindersActiveThenArchived() = runBlocking<Unit> {
        val dao = db.reminderDao()
        fun r(title: String, p: Priority, due: Long) =
            ReminderEntity(title = title, note = null, imagePath = null, priority = p, dueAt = due, createdAt = NOW, updatedAt = NOW)
        val n1 = dao.insert(r("normal early", Priority.NORMAL, 1))
        val h2 = dao.insert(r("high late", Priority.HIGH, 20))
        dao.insert(r("high early", Priority.HIGH, 10))

        assertEquals(listOf("high early", "high late", "normal early"), dao.active().first().map { it.title })
        assertEquals(listOf("high early", "high late"), dao.urgent(3).first().map { it.title })

        dao.markDone(n1, 100)
        dao.markDone(h2, 200)
        assertEquals(listOf("high early"), dao.active().first().map { it.title })
        assertEquals(listOf("high late", "normal early"), dao.archived().first().map { it.title })

        dao.markUndone(h2, 300)
        assertEquals(2, dao.active().first().size)
    }

    // ---------------- seeding ----------------

    @Test fun seedingProducesExactDefaults() = runBlocking<Unit> {
        val seeded = TestDb.create(seed = true)
        try {
            assertSeeded(seeded)
        } finally {
            seeded.close()
        }
    }

    @Test fun seedingRunsOnlyOnce() = runBlocking<Unit> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val name = "seed_once_test.db"
        ctx.deleteDatabase(name)
        fun open() = Room.databaseBuilder(ctx, AppDatabase::class.java, name).addCallback(SeedCallback { NOW }).build()
        try {
            open().also { assertSeeded(it) }.close()
            open().also { assertSeeded(it) }.close() // re-open: onCreate does not run again
        } finally {
            ctx.deleteDatabase(name)
        }
    }

    private suspend fun assertSeeded(d: AppDatabase) {
        val farms = d.masterDao().farms().first()
        assertEquals((1..8).map { "ખેતર $it" }, farms.map { it.name })
        assertTrue(farms.all { it.areaInVigha == 0.0 })
        assertEquals(List(4) { ProfileRole.MANAGER_1 } + List(4) { ProfileRole.MANAGER_2 }, farms.map { it.managerRole })

        val families = d.masterDao().families().first()
        assertEquals(listOf("પરિવાર A", "પરિવાર B"), families.map { it.displayName })
        assertTrue(families.all { it.whatsappPhone == null })

        val profiles = d.profileDao().all().first()
        assertEquals(3, profiles.size)
        val owner = profiles.single { it.role == ProfileRole.OWNER }
        assertEquals("ગિરવાનસિંહ વાળા", owner.name)
        assertEquals("મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર", owner.address)
        assertNull(owner.phone)
        assertTrue(profiles.filter { it.role != ProfileRole.OWNER }.all { it.name.isEmpty() })

        assertEquals(SeedData.crops, d.masterDao().allCrops().first().map { it.name })
        val cats = d.masterDao().allCategories().first()
        assertEquals(SeedData.categories.size, cats.size)
        assertEquals(listOf("સાધન"), cats.filter { it.isPhysicalTool }.map { it.name })

        val prefs = d.panchangDao().prefsNow()
        assertEquals(PanchangAlertType.entries.toSet(), prefs.map { it.type }.toSet())
        assertTrue(prefs.all { it.enabled })
    }
}
