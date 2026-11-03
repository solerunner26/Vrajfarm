package com.vraj.khetihisab.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One ledger row. Money is always Long paise.
 *
 * - `section` is the account section (FARM | FAMILY | GENERAL | PERSONAL).
 * - "isPending" from the root specs is modelled as [payStatus] != PAID (extension [isPending]).
 */
@Entity(
    tableName = "txn",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = FarmCropCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryMasterEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("date"),
        Index("section"),
        Index("farmId"),
        Index("familyId"),
        Index("payStatus"),
        Index("cycleId"),
        Index("categoryId"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    /** Cached tithi label, e.g. "આસો વદ નોમ". */
    val tithiLabel: String,
    val section: AccountSection,
    val type: TxnType,
    val amountPaise: Long,
    val payStatus: PayStatus = PayStatus.PAID,
    val counterparty: String? = null,
    val farmId: Long? = null,
    val cycleId: Long? = null,
    val familyId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
    val voiceNote: String? = null,
    val billPhotoPath: String? = null,
    val mann: Double? = null,
    val extraKg: Double? = null,
    val ratePerMannPaise: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Root-spec "isPending": true for દેવાના / લેવાના બાકી rows. */
val TransactionEntity.isPending: Boolean get() = payStatus != PayStatus.PAID
