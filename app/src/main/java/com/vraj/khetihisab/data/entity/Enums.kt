package com.vraj.khetihisab.data.entity

/** Ledger section. PERSONAL money must never reach any farm P&L query. */
/**
 * Every section except PERSONAL counts in the overall business P&L:
 * FARM (crops), ANIMAL (પશુપાલન — cows, buffaloes: feed, vet, milk), FAMILY (farm managers' salary/ઉપાડ),
 * GENERAL (light, water, CCTV). PERSONAL is always excluded. Stored by name, so order is free.
 */
enum class AccountSection { FARM, ANIMAL, FAMILY, GENERAL, PERSONAL }

/** ADVANCE = ઉપાડ (Family). */
enum class TxnType { INCOME, EXPENSE, ADVANCE }

/** PENDING_PAYABLE = દેવાના બાકી, PENDING_RECEIVABLE = લેવાના બાકી. */
enum class PayStatus { PAID, PENDING_PAYABLE, PENDING_RECEIVABLE }

enum class ShopTab { FARM, PERSONAL }

enum class Priority { HIGH, NORMAL }

enum class ProfileRole { OWNER, MANAGER_1, MANAGER_2 }

enum class PanchangAlertType { BIJ, AGIYARAS, POONAM, AMAS, FESTIVAL }

/** Values for [ReminderEntity.sourceType]. Stored as plain strings per doc 03. */
object ReminderSource {
    const val LOAN = "LOAN"
    const val PANCHANG = "PANCHANG"
    const val MANUAL = "MANUAL"
}

/** Values for [FarmCropCycleEntity.season]. */
object Season {
    const val KHARIF = "KHARIF"
    const val RABI = "RABI"
    const val SUMMER = "SUMMER"
}
