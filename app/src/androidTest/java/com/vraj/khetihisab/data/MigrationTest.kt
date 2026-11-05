package com.vraj.khetihisab.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Doc 03 policy: every schema bump has an explicit Migration + this test. No data may be lost. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun v1ToV2KeepsLedgerAndAddsJointFarmsAndMilk() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO farm (id, name, areaInVigha, managerRole, sortOrder, createdAt, updatedAt) VALUES (1, 'ખેતર 1', 4.5, 'MANAGER_1', 1, 0, 0)")
            execSQL(
                "INSERT INTO txn (id, date, tithiLabel, section, type, amountPaise, payStatus, farmId, isDeleted, createdAt, updatedAt) " +
                    "VALUES (1, 20000, '', 'FARM', 'INCOME', 4880000, 'PAID', 1, 0, 0, 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 2, true, AppDatabase.MIGRATION_1_2)
        db.query("SELECT managerRole, jointManagers FROM farm WHERE id=1").use { c ->
            c.moveToFirst()
            assertEquals("MANAGER_1", c.getString(0))
            assertEquals(0, c.getInt(1))
        }
        db.query("SELECT amountPaise FROM txn WHERE id=1").use { c ->
            c.moveToFirst()
            assertEquals(4_880_000L, c.getLong(0))
        }
        db.execSQL("INSERT INTO milk_entry (date, session, animal, litres, fatPercent, ratePaise, createdAt, updatedAt) VALUES (20000, 'AM', 'ગંગા', 6.5, 6.8, 5500, 0, 0)")
        db.query("SELECT COUNT(*) FROM milk_entry").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
    }
}
