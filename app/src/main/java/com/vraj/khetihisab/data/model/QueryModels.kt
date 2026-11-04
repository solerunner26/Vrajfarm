package com.vraj.khetihisab.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity

/** One farm's FARM-section totals for a period. PERSONAL / FAMILY / GENERAL rows never contribute. */
data class FarmPnlRow(
    val farmId: Long,
    val name: String,
    val areaInVigha: Double,
    val incomePaise: Long,
    val expensePaise: Long,
) {
    val netPaise: Long get() = incomePaise - expensePaise
}

data class Totals(
    val income: Long,
    val expense: Long,
) {
    val net: Long get() = income - expense

    companion object {
        val ZERO = Totals(0, 0)
    }
}

data class UdhaarSummary(
    /** દેવાના બાકી (we owe). */
    val payable: Long,
    /** લેવાના બાકી (owed to us). */
    val receivable: Long,
    val count: Int,
) {
    val isEmpty: Boolean get() = payable == 0L && receivable == 0L && count == 0

    companion object {
        val EMPTY = UdhaarSummary(0, 0, 0)
    }
}

data class CategoryTotal(
    val name: String,
    val total: Long,
)

/**
 * One section's share of the business P&L. Expense includes FAMILY ADVANCE (farm managers' salary/ઉપાડ).
 * PERSONAL never appears here.
 */
data class SectionTotals(
    val section: com.vraj.khetihisab.data.entity.AccountSection,
    val incomePaise: Long,
    val expensePaise: Long,
) {
    val netPaise: Long get() = incomePaise - expensePaise
}

/** Raw month bucket from SQL; [yearMonth] is "yyyy-MM". */
data class MonthTotal(
    val yearMonth: String,
    val totalPaise: Long,
)

data class AssetWithStatus(
    @Embedded val asset: AssetEntity,
    val isLent: Boolean,
)

data class LoanWithAsset(
    @Embedded val loan: AssetLoanEntity,
    @Relation(parentColumn = "assetId", entityColumn = "id")
    val asset: AssetEntity,
)

data class CycleWithCrop(
    @Embedded val cycle: FarmCropCycleEntity,
    @Relation(parentColumn = "cropId", entityColumn = "id")
    val crop: CropMasterEntity,
)

/** One calendar day's money: income (green) and outgoing = expense + ઉપાડ (red). */
data class DayTotal(
    val date: java.time.LocalDate,
    val income: Long,
    val expense: Long,
)
