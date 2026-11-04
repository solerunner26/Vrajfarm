package com.vraj.khetihisab.report

import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.data.repo.LedgerRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Loads report data. The default implementation reads Room through the existing DAOs / LedgerRepository (read-only). */
fun interface ReportDataSource {
    suspend fun load(type: ReportType, range: DateRange, today: LocalDate): ReportDoc
}

class RoomReportDataSource(private val db: AppDatabase) : ReportDataSource {
    private val ledger = LedgerRepository(db.txnDao())
    private val master = db.masterDao()
    private val assets = db.assetDao()

    override suspend fun load(type: ReportType, range: DateRange, today: LocalDate): ReportDoc = when (type) {
        ReportType.DAILY_LEDGER ->
            ReportContentBuilder.dailyLedger(range, ledger.entriesNow(range), lookups())

        ReportType.FARM_PNL -> ReportContentBuilder.farmPnl(
            range,
            ledger.farmPnl(range).first(),
            ledger.unassignedFarmTotals(range).first(),
            ledger.farmTotals(range).first(),
            ledger.generalExpense(range).first(),
        )

        ReportType.VIGHA_SCORECARD ->
            ReportContentBuilder.vighaScorecard(range, ledger.vighaRanking(range).first())

        ReportType.FAMILY_ADVANCES -> {
            val families = master.families().first()
            val cols = families.map {
                FamilyColumn(it.id, it.displayName.ifBlank { ReportLabels.FAMILY_FALLBACK.format(it.code) })
            }
            val buckets = families.associate { it.id to ledger.familyAdvanceBuckets(it.id, range).first() }
            ReportContentBuilder.familyAdvances(range, cols, buckets)
        }

        ReportType.PERSONAL -> ReportContentBuilder.personal(
            range,
            ledger.personalTotals(range).first(),
            ledger.entriesNow(range),
            lookups(),
        )

        ReportType.PENDING_UDHAAR ->
            ReportContentBuilder.pendingUdhaar(today, ledger.pendingListAll().first(), lookups())

        ReportType.ASSETS_LOANS ->
            ReportContentBuilder.assetsLoans(today, assets.inventory().first(), assets.activeLoans().first())
    }

    private suspend fun lookups(): LedgerLookups = LedgerLookups(
        farms = master.farmsNow().associate { it.id to it.name },
        families = master.families().first().associate { it.id to it.displayName.ifBlank { it.code } },
        categories = master.allCategories().first().associate { it.id to it.name },
    )
}
