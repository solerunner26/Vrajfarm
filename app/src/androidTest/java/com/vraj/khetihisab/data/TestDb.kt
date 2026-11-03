package com.vraj.khetihisab.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.FamilyEntity
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.seed.SeedCallback
import java.time.LocalDate

/** In-memory database helpers shared by the DAO tests. */
object TestDb {
    const val NOW = 1_700_000_000_000L

    fun create(seed: Boolean = false): AppDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .apply { if (seed) addCallback(SeedCallback { NOW }) }
            .build()

    fun farm(name: String, area: Double = 0.0, sortOrder: Int = 1) =
        FarmEntity(name = name, areaInVigha = area, sortOrder = sortOrder, createdAt = NOW, updatedAt = NOW)

    fun family(code: String) =
        FamilyEntity(code = code, displayName = "પરિવાર $code", whatsappPhone = null, createdAt = NOW, updatedAt = NOW)

    fun txn(
        section: AccountSection,
        type: TxnType,
        rupees: Long,
        date: LocalDate = LocalDate.of(2026, 11, 3),
        farmId: Long? = null,
        familyId: Long? = null,
        payStatus: PayStatus = PayStatus.PAID,
        categoryId: Long? = null,
    ) = TransactionEntity(
        date = date,
        tithiLabel = "આસો વદ નોમ",
        section = section,
        type = type,
        amountPaise = rupees * 100,
        payStatus = payStatus,
        farmId = farmId,
        familyId = familyId,
        categoryId = categoryId,
        createdAt = NOW,
        updatedAt = NOW,
    )
}
