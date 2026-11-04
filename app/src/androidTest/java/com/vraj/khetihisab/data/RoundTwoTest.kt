package com.vraj.khetihisab.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vraj.khetihisab.backup.JsonBackup
import com.vraj.khetihisab.data.TestDb.farm
import com.vraj.khetihisab.data.TestDb.txn
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.repo.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/** Owner feedback round 2026-11-04: new queries, ભાગિયા sync, JSON backup, master delete. */
@RunWith(AndroidJUnit4::class)
class RoundTwoTest {
    private lateinit var db: AppDatabase
    private val day = LocalDate.of(2026, 11, 3)
    private val from = LocalDate.of(2026, 11, 1)
    private val to = LocalDate.of(2026, 11, 30)

    @Before fun setUp() {
        db = TestDb.create(seed = true)
    }

    @After fun tearDown() = db.close()

    // ---------------- sectionTotals (Samanya / Personal screens) ----------------

    @Test fun sectionTotalsCountsOnlyThatSection() = runBlocking<Unit> {
        val t = db.txnDao()
        t.insert(txn(AccountSection.GENERAL, TxnType.EXPENSE, 300))
        t.insert(txn(AccountSection.GENERAL, TxnType.EXPENSE, 200))
        t.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 9_999))
        t.insert(txn(AccountSection.FARM, TxnType.INCOME, 5_000))
        val g = t.sectionTotals(AccountSection.GENERAL, from, to).first()
        assertEquals(0L, g.income)
        assertEquals(50_000L, g.expense)
        val p = t.sectionTotals(AccountSection.PERSONAL, from, to).first()
        assertEquals(999_900L, p.expense)
    }

    @Test fun sectionUdhaarSplitsPayableAndReceivable() = runBlocking<Unit> {
        val t = db.txnDao()
        t.insert(txn(AccountSection.GENERAL, TxnType.EXPENSE, 400, payStatus = PayStatus.PENDING_PAYABLE))
        t.insert(txn(AccountSection.GENERAL, TxnType.INCOME, 100, payStatus = PayStatus.PENDING_RECEIVABLE))
        t.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 700, payStatus = PayStatus.PENDING_PAYABLE))
        val u = t.sectionUdhaar(AccountSection.GENERAL).first()
        assertEquals(40_000L, u.payable)
        assertEquals(10_000L, u.receivable)
        assertEquals(2, u.count)
    }

    // ---------------- dailyTotals (calendar green / red) ----------------

    @Test fun dailyTotalsGroupByDateAndCountAdvancesAsOutgoing() = runBlocking<Unit> {
        val t = db.txnDao()
        val fam = db.masterDao().familyByCode("A")!!.id
        t.insert(txn(AccountSection.FARM, TxnType.INCOME, 1_000, date = day))
        t.insert(txn(AccountSection.FARM, TxnType.EXPENSE, 250, date = day))
        t.insert(txn(AccountSection.FAMILY, TxnType.ADVANCE, 500, date = day.plusDays(1), familyId = fam))
        val deleted = t.insert(txn(AccountSection.FARM, TxnType.INCOME, 77, date = day.plusDays(2)))
        t.softDelete(deleted, TestDb.NOW)
        val rows = t.dailyTotals(from, to).first().associateBy { it.date }
        assertEquals(100_000L, rows.getValue(day).income)
        assertEquals(25_000L, rows.getValue(day).expense)
        assertEquals(50_000L, rows.getValue(day.plusDays(1)).expense)
        assertNull("deleted rows never colour a day", rows[day.plusDays(2)])
    }

    // ---------------- ભાગિયા = farm manager: one person ----------------

    @Test fun profileNameAndPhoneMirrorIntoFamilyRow() = runBlocking<Unit> {
        val repo = ProfileRepository(db.profileDao(), { TestDb.NOW }, master = db.masterDao())
        repo.upsert(PersonProfileEntity(role = ProfileRole.MANAGER_1, name = "રમેશભાઈ", phone = "9876543210", address = null, photoPath = null, updatedAt = 0))
        val a = db.masterDao().familyByCode("A")!!
        assertEquals("રમેશભાઈ", a.displayName)
        assertEquals("9876543210", a.whatsappPhone)
        // Blank name falls back to "ભાગિયા 2".
        repo.syncBhagiya()
        assertEquals("ભાગિયા 2", db.masterDao().familyByCode("B")!!.displayName)
    }

    @Test fun phoneSavedOnAdvanceScreenLandsOnProfile() = runBlocking<Unit> {
        val repo = ProfileRepository(db.profileDao(), { TestDb.NOW }, master = db.masterDao())
        repo.updateBhagiyaPhone("B", "9000000002")
        assertEquals("9000000002", db.profileDao().byRoleNow(ProfileRole.MANAGER_2)!!.phone)
        assertEquals("9000000002", db.masterDao().familyByCode("B")!!.whatsappPhone)
    }

    // ---------------- master delete ----------------

    @Test fun deletingUsedCategoryKeepsTheEntry() = runBlocking<Unit> {
        val m = db.masterDao()
        val id = m.insertCategory(CategoryMasterEntity(name = "ટેસ્ટ પ્રકાર", section = AccountSection.FARM, type = TxnType.EXPENSE))
        val farmId = m.insertFarm(farm("ખેતર X"))
        val txnId = db.txnDao().insert(txn(AccountSection.FARM, TxnType.EXPENSE, 120, farmId = farmId, categoryId = id))
        assertEquals(1, m.txnCountForCategory(id))
        m.deleteCategory(m.allCategories().first().single { it.id == id })
        val t = db.txnDao().byId(txnId)!!
        assertNull(t.categoryId)
        assertEquals(12_000L, t.amountPaise)
    }

    // ---------------- JSON backup round trip ----------------

    @Test fun jsonBackupRestoresEveryRowAndPhoto() = runBlocking<Unit> {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val files = File(ctx.cacheDir, "rt_files").apply { deleteRecursively(); mkdirs() }
        File(files, "bill_photos").mkdirs()
        File(files, "bill_photos/b1.webp").writeBytes(byteArrayOf(1, 2, 3, 4))
        val t = db.txnDao()
        t.insert(txn(AccountSection.FARM, TxnType.INCOME, 48_800))
        t.insert(txn(AccountSection.PERSONAL, TxnType.EXPENSE, 9_999))
        val sdb = db.openHelper.writableDatabase

        val out = ByteArrayOutputStream()
        val sum = JsonBackup.export(sdb, AppDatabase.VERSION, "test", files, out, TestDb.NOW)
        assertEquals(2L, sum.counts["txn"])
        assertEquals(1, sum.photoCount)
        val bytes = out.toByteArray()
        assertEquals(2L, JsonBackup.inspect(ByteArrayInputStream(bytes))!!.entries)

        // Wipe, then restore into the same db + a new files dir.
        sdb.execSQL("DELETE FROM txn")
        File(files, "bill_photos").deleteRecursively()
        val r = JsonBackup.restore(sdb, AppDatabase.VERSION, files, ByteArrayInputStream(bytes))
        assertTrue(r is JsonBackup.RestoreResult.Ok)
        val back = t.entriesInRangeNow(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(setOf(4_880_000L, 999_900L), back.map { it.amountPaise }.toSet())
        assertEquals(listOf<Byte>(1, 2, 3, 4), File(files, "bill_photos/b1.webp").readBytes().toList())
        // Seed rows survive too (farms, families, categories are in the file).
        assertEquals(8, db.masterDao().farmsNow().size)
    }

    @Test fun restoreRejectsForeignFile() {
        val r = JsonBackup.restore(db.openHelper.writableDatabase, AppDatabase.VERSION, File("/tmp"), ByteArrayInputStream("""{"app":"other","tables":{}}""".toByteArray()))
        assertTrue(r is JsonBackup.RestoreResult.NotABackup)
    }
}
