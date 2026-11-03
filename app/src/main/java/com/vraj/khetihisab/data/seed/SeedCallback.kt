package com.vraj.khetihisab.data.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds the default master data in one transaction. Runs only in [onCreate], i.e. exactly once,
 * when the database file is first created. Writes raw rows because DAOs are not usable inside onCreate.
 */
class SeedCallback(
    private val clock: () -> Long = System::currentTimeMillis,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seed(db, clock())
    }

    companion object {
        fun seed(db: SupportSQLiteDatabase, now: Long) {
            db.beginTransaction()
            try {
                SeedData.farms.forEach { f ->
                    db.insertOrThrow(
                        "farm",
                        ContentValues().apply {
                            put("name", f.name)
                            put("areaInVigha", 0.0)
                            put("managerRole", f.managerRole.name)
                            put("sortOrder", f.sortOrder)
                            put("createdAt", now)
                            put("updatedAt", now)
                        },
                    )
                }
                SeedData.families.forEach { f ->
                    db.insertOrThrow(
                        "family",
                        ContentValues().apply {
                            put("code", f.code)
                            put("displayName", f.displayName)
                            putNull("whatsappPhone")
                            put("createdAt", now)
                            put("updatedAt", now)
                        },
                    )
                }
                SeedData.profiles.forEach { p ->
                    db.insertOrThrow(
                        "person_profile",
                        ContentValues().apply {
                            put("role", p.role.name)
                            put("name", p.name)
                            putNull("phone")
                            if (p.address != null) put("address", p.address) else putNull("address")
                            putNull("photoPath")
                            put("updatedAt", now)
                        },
                    )
                }
                SeedData.crops.forEach { c ->
                    db.insertOrThrow(
                        "crop_master",
                        ContentValues().apply {
                            put("name", c)
                            put("isActive", 1)
                        },
                    )
                }
                SeedData.categories.forEach { c ->
                    db.insertOrThrow(
                        "category_master",
                        ContentValues().apply {
                            put("name", c.name)
                            put("section", c.section.name)
                            put("type", c.type.name)
                            put("isPhysicalTool", if (c.isPhysicalTool) 1 else 0)
                            put("isActive", 1)
                        },
                    )
                }
                PanchangDefaults.all(now).forEach { p ->
                    db.insertOrThrow(
                        "panchang_alert_pref",
                        ContentValues().apply {
                            put("type", p.type.name)
                            put("enabled", if (p.enabled) 1 else 0)
                            put("notifyEveningBefore", if (p.notifyEveningBefore) 1 else 0)
                            put("showDialogOnDay", if (p.showDialogOnDay) 1 else 0)
                            put("doText", p.doText)
                            put("avoidText", p.avoidText)
                            put("updatedAt", p.updatedAt)
                        },
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        private fun SupportSQLiteDatabase.insertOrThrow(table: String, values: ContentValues) {
            val id = insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
            check(id != -1L) { "Seed insert into $table failed" }
        }
    }
}
