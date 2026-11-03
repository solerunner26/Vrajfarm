package com.vraj.khetihisab.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vraj.khetihisab.data.TestDb.NOW
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.entity.ReminderSource
import com.vraj.khetihisab.data.repo.AssetRepository
import com.vraj.khetihisab.data.repo.LoanSearchQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AssetLoanDaoTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 11, 3)

    @Before fun setUp() {
        db = TestDb.create()
    }

    @After fun tearDown() = db.close()

    private suspend fun asset(name: String) = db.assetDao().insertAsset(
        AssetEntity(name = name, iconKey = "asset", purchaseDate = null, pricePaise = null, photoPath = null, note = null, createdAt = NOW, updatedAt = NOW),
    )

    private fun loan(assetId: Long, borrower: String, handed: LocalDate, expected: LocalDate) = AssetLoanEntity(
        assetId = assetId,
        borrowerName = borrower,
        borrowerPhone = "9800000000",
        borrowerPhotoPath = "borrower_photos/x.webp",
        handedOverDate = handed,
        handedOverTithi = "કારતક સુદ ત્રીજ",
        expectedReturnDate = expected,
        createdAt = NOW,
        updatedAt = NOW,
    )

    @Test fun overdueExcludesReturned() = runBlocking<Unit> {
        val dao = db.assetDao()
        val rot = asset("રોટાવેટર")
        val pump = asset("પંપ")
        val tr = asset("ટ્રેક્ટર")
        val overdueOpen = dao.insertLoan(loan(rot, "રમેશભાઈ", today.minusDays(5), today.minusDays(2)))
        val overdueReturned = dao.insertLoan(loan(pump, "સુરેશભાઈ", today.minusDays(5), today.minusDays(1)))
        dao.insertLoan(loan(tr, "મહેશભાઈ", today, today)) // due today: not overdue
        dao.markReturned(overdueReturned, NOW)

        val overdue = dao.overdueLoans(today).first()
        assertEquals(listOf(overdueOpen), overdue.map { it.loan.id })
        assertEquals("રોટાવેટર", overdue.single().asset.name)
        assertEquals(listOf(overdueOpen), dao.overdueLoansNow(today).map { it.loan.id })

        val active = dao.activeLoans().first()
        assertEquals(2, active.size)
        assertTrue(active.none { it.loan.id == overdueReturned })
        assertEquals(2, dao.activeLoanCount().first())
    }

    @Test fun searchHistoryByDate() = runBlocking<Unit> {
        val dao = db.assetDao()
        val tr = asset("ટ્રેક્ટર")
        val pump = asset("પંપ")
        val nov23 = LocalDate.of(2025, 11, 23)
        val trLoan = dao.insertLoan(loan(tr, "રમેશભાઈ", nov23, nov23.plusDays(3)))
        dao.insertLoan(loan(pump, "સુરેશભાઈ", nov23.plusDays(1), nov23.plusDays(4)))
        dao.markReturned(trLoan, NOW) // returned loans stay in history

        val byDate = LoanSearchQuery.parse("23-11-2025")
        val hits = dao.searchHistory(byDate.text, byDate.epochDay).first()
        assertEquals(listOf(trLoan), hits.map { it.loan.id })
        assertEquals("ટ્રેક્ટર", hits.single().asset.name)
        assertNotNull(hits.single().loan.borrowerPhotoPath)

        val byAsset = LoanSearchQuery.parse("ટ્રેક્ટર")
        assertEquals(listOf(trLoan), dao.searchHistory(byAsset.text, byAsset.epochDay).first().map { it.loan.id })

        val byName = LoanSearchQuery.parse("સુરેશ")
        assertEquals(1, dao.searchHistory(byName.text, byName.epochDay).first().size)
    }

    @Test fun repositorySearchAndSoftDeletedAssetStaysInHistory() = runBlocking<Unit> {
        val repo = AssetRepository(db, clock = { NOW })
        val tr = asset("ટ્રેક્ટર")
        val id = db.assetDao().insertLoan(loan(tr, "રમેશભાઈ", LocalDate.of(2025, 11, 23), LocalDate.of(2025, 11, 26)))
        repo.markReturned(id)
        assertEquals(AssetRepository.DeleteResult.Deleted, repo.deleteAsset(tr))

        assertTrue(db.assetDao().inventory().first().isEmpty())
        assertEquals(1, repo.searchLoanHistory("23-11-2025").first().size)
        assertEquals(1, repo.searchLoanHistory("ટ્રેક્ટર").first().size)
        assertEquals(1, repo.searchLoanHistory("   ").first().size)
    }

    @Test fun inventoryIsLentAndDeleteGuard() = runBlocking<Unit> {
        val repo = AssetRepository(db, clock = { NOW })
        val tr = asset("ટ્રેક્ટર")
        asset("પંપ")
        val loanId = repo.lend(loan(tr, "રમેશભાઈ", today, today.plusDays(3)))

        val inv = db.assetDao().inventory().first().associate { it.asset.name to it.isLent }
        assertEquals(mapOf("પંપ" to false, "ટ્રેક્ટર" to true), inv)
        assertTrue(repo.isCurrentlyLent(tr))
        assertEquals(AssetRepository.DeleteResult.BlockedByActiveLoan, repo.deleteAsset(tr))

        // notifyOnDue → linked HIGH reminder; returning the asset marks it done.
        val reminder = db.reminderDao().findBySource(ReminderSource.LOAN, loanId)
        assertNotNull(reminder)
        assertEquals("ટ્રેક્ટર પાછું લાવવું", reminder!!.title)
        assertFalse(reminder.isDone)

        repo.markReturned(loanId)
        assertTrue(db.reminderDao().findBySource(ReminderSource.LOAN, loanId)!!.isDone)
        assertFalse(repo.isCurrentlyLent(tr))
        assertEquals(1, repo.loansForAsset(tr).first().size)
    }

    @Test fun markRemindedAndCascadeDelete() = runBlocking<Unit> {
        val dao = db.assetDao()
        val tr = asset("ટ્રેક્ટર")
        val id = dao.insertLoan(loan(tr, "રમેશભાઈ", today, today.plusDays(1)))
        dao.markReminded(id, 1234L)
        assertEquals(1234L, dao.loanById(id)!!.loan.lastWhatsAppReminderAt)

        // Hard delete of the asset row cascades its loans (FK CASCADE).
        db.openHelper.writableDatabase.execSQL("DELETE FROM asset WHERE id=$tr")
        assertTrue(dao.allLoans().first().isEmpty())
    }
}
