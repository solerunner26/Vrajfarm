package com.vraj.khetihisab.ui.assets

import com.vraj.khetihisab.data.model.LoanWithAsset
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Badge state of an active loan (doc 11): "N દિવસ મોડું" / "આજે પરત" / "N દિવસ બાકી". */
sealed interface LoanStatus {
    data class Overdue(val days: Int) : LoanStatus
    data object DueToday : LoanStatus
    data class DaysLeft(val days: Int) : LoanStatus

    companion object {
        fun of(expectedReturn: LocalDate, today: LocalDate): LoanStatus {
            val d = ChronoUnit.DAYS.between(today, expectedReturn).toInt()
            return when {
                d < 0 -> Overdue(-d)
                d == 0 -> DueToday
                else -> DaysLeft(d)
            }
        }

        /** Overdue first (most late first), then by return date, then newest handover. */
        fun sortActive(loans: List<LoanWithAsset>, today: LocalDate): List<LoanWithAsset> =
            loans.sortedWith(
                compareBy<LoanWithAsset> { if (it.loan.expectedReturnDate.isBefore(today)) 0 else 1 }
                    .thenBy { it.loan.expectedReturnDate }
                    .thenByDescending { it.loan.handedOverDate }
                    .thenByDescending { it.loan.id },
            )
    }
}

val LoanStatus.isOverdue: Boolean get() = this is LoanStatus.Overdue
