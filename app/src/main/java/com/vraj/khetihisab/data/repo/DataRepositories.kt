package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.AppDatabase

/** One place to build every repository (used by di/AppContainer). */
class DataRepositories(
    db: AppDatabase,
    clock: () -> Long = System::currentTimeMillis,
    listener: DataChangeListener = DataChangeListener.NONE,
) {
    val ledger = LedgerRepository(db.txnDao(), clock, listener)
    val master = MasterRepository(db.masterDao(), clock, listener)
    val shopping = ShoppingRepository(db, clock, listener)
    val assets = AssetRepository(db, clock, listener)
    val reminders = ReminderRepository(db.reminderDao(), clock, listener)
    val profiles = ProfileRepository(db.profileDao(), clock, listener, db.masterDao())
    val panchang = PanchangPrefRepository(db.panchangDao(), clock)
}
