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
 * Single local source of truth — 14 entities, version 2.
 * v2 (2026-11-05): farm.jointManagers (farm run by both ભાગિયા) + milk_entry (દૂધ રજીસ્ટર).
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
        com.vraj.khetihisab.data.entity.MilkEntryEntity::class,
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
    abstract fun milkDao(): com.vraj.khetihisab.data.dao.MilkDao

    companion object {
        const val NAME = "vraj_kheti.db"
        const val VERSION = 2

        /** v1 → v2: additive only (a column with a default + a new table), no data is touched. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `farm` ADD COLUMN `jointManagers` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(MILK_TABLE_SQL)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_milk_entry_date` ON `milk_entry` (`date`)")
            }
        }

        /** Must equal Room's createSql in schemas/…/2.json (checked by MigrationTest). */
        const val MILK_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `milk_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`date` INTEGER NOT NULL, `session` TEXT NOT NULL, `animal` TEXT NOT NULL, `litres` REAL NOT NULL, " +
            "`fatPercent` REAL, `ratePaise` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"

        /** Explicit migrations only. */
        val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf(MIGRATION_1_2)

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
