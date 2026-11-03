package com.vraj.khetihisab.di

import android.content.Context
import com.vraj.khetihisab.security.LockManager
import com.vraj.khetihisab.security.PinStore
import com.vraj.khetihisab.settings.ThemeRepository
import com.vraj.khetihisab.settings.vkPrefsDataStore

/**
 * Manual DI (no Hilt). One instance lives in [com.vraj.khetihisab.VrajApp.container].
 * Feature agents add their repositories here (database, WhatsAppLauncher, schedulers, …).
 */
class AppContainer(private val appContext: Context) {
    val themeRepository: ThemeRepository by lazy { ThemeRepository(appContext.vkPrefsDataStore) }
    val pinStore: PinStore by lazy { PinStore.create(appContext) }
    val lockManager: LockManager = LockManager()
}
