package com.vraj.khetihisab.di

import android.content.Context
import com.vraj.khetihisab.backup.BackupScheduler
import com.vraj.khetihisab.backup.BackupStatusRepository
import com.vraj.khetihisab.backup.BackupTrigger
import com.vraj.khetihisab.backup.BackupGraph
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.security.LockManager
import com.vraj.khetihisab.security.PinStore
import com.vraj.khetihisab.settings.ThemeRepository
import com.vraj.khetihisab.settings.vkPrefsDataStore
import com.vraj.khetihisab.share.WhatsAppTemplateStore
import com.vraj.khetihisab.widget.WidgetRefresher

/**
 * Manual DI (no Hilt). One instance lives in [com.vraj.khetihisab.VrajApp.container].
 * The single [repos] set is shared with every module's service locator (NotifyGraph, AssetsModule,
 * ScreensDepsProvider) so widget refreshes fire on every write.
 */
class AppContainer(private val appContext: Context) {
    val themeRepository: ThemeRepository by lazy { ThemeRepository(appContext.vkPrefsDataStore) }
    val pinStore: PinStore by lazy { PinStore.create(appContext) }
    val lockManager: LockManager = LockManager()

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val repos: DataRepositories by lazy {
        DataRepositories(database, listener = WidgetRefresher.get(appContext))
    }
    val whatsAppTemplates: WhatsAppTemplateStore by lazy { WhatsAppTemplateStore(appContext) }

    val backupScheduler: BackupScheduler by lazy { BackupScheduler(appContext) }
    val backupStatus: BackupStatusRepository get() = BackupGraph.deps(appContext).status

    fun backupNow() = backupScheduler.enqueueBackup(BackupTrigger.MANUAL)
}
