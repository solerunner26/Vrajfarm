package com.vraj.khetihisab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vraj.khetihisab.data.dao.AssetDao
import com.vraj.khetihisab.data.dao.MasterDao
import com.vraj.khetihisab.data.dao.PanchangDao
import com.vraj.khetihisab.data.dao.ProfileDao
import com.vraj.khetihisab.data.dao.ReminderDao
import com.vraj.khetihisab.data.dao.ShoppingDao
import com.vraj.khetihisab.data.dao.TxnDao
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.FamilyEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.seed.SeedCallback

/**
 * Single local source of truth — 13 entities, version 1.
 *
 * Migration policy (doc 03): every schema change bumps [VERSION], adds an explicit
 * Migration(n, n+1) to [MIGRATIONS] and a MigrationTestHelper test.
 * fallbackToDestructiveMigration() is FORBIDDEN — this is the owner's ledger.
 */
@Database(
    entities = [
        FarmEntity::class,
        FamilyEntity::class,
        CropMasterEntity::class,
        CategoryMasterEntity::class,
        FarmCropCycleEntity::class,
        TransactionEntity::class,
        ShoppingItemEntity::class,
        AssetEntity::class,
        ReminderEntity::class,
        PersonProfileEntity::class,
        AssetLoanEntity::class,
        PanchangAlertPrefEntity::class,
        CustomFestivalEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(DateConverters::class, EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txnDao(): TxnDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun assetDao(): AssetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun masterDao(): MasterDao
    abstract fun profileDao(): ProfileDao
    abstract fun panchangDao(): PanchangDao

    companion object {
        const val NAME = "vraj_kheti.db"
        const val VERSION = 1

        /** Explicit migrations only. Empty for v1. */
        val MIGRATIONS: Array<androidx.room.migration.Migration> = emptyArray()

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addCallback(SeedCallback())
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
