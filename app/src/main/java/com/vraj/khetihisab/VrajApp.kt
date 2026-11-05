package com.vraj.khetihisab

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.vraj.khetihisab.backup.BackupStatus
import com.vraj.khetihisab.backup.BackupState
import com.vraj.khetihisab.di.AppContainer
import com.vraj.khetihisab.notify.AlarmRescheduler
import com.vraj.khetihisab.notify.NotificationChannels
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.ui.assets.AssetsDependencies
import com.vraj.khetihisab.ui.assets.AssetsModule
import com.vraj.khetihisab.ui.home.BackupUiInfo
import com.vraj.khetihisab.ui.home.BackupUiStatus
import com.vraj.khetihisab.ui.home.ScreensDeps
import com.vraj.khetihisab.ui.home.ScreensDepsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class VrajApp : Application() {

    lateinit var container: AppContainer
        private set

    /** Process-lifetime scope for idempotent start-up work (alarm re-arming). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val c = AppContainer(this)
        container = c
        // Auto-lock after 30 s in the background (doc 04 §2).
        ProcessLifecycleOwner.get().lifecycle.addObserver(c.lockManager.observer)

        // One shared repository set for every module (Wave 2 integration contract, HANDOVER.md).
        NotifyGraph.install(c.repos)
        AssetsModule.install { AssetsDependencies(c.repos) }
        ScreensDepsProvider.install(
            ScreensDeps(
                appContext = this,
                repos = c.repos,
                themeRepository = c.themeRepository,
                pinStoreFactory = { c.pinStore },
                lockManager = c.lockManager,
                backupInfo = c.backupStatus.status.map(::toUiInfo),
                onBackupNow = c::backupNow,
            ),
        )

        NotificationChannels.createAll(this)
        appScope.launch {
            runCatching {
                // ભાગિયા = farm manager: keep the ledger's family rows in step with the profiles.
                c.repos.profiles.syncBhagiya()
                AlarmRescheduler.rescheduleEverything(this@VrajApp)
                c.backupScheduler.ensureScheduled()
                com.vraj.khetihisab.update.UpdateCheckWorker.ensureScheduled(this@VrajApp)
            }.onFailure { Log.w(TAG, "start-up scheduling failed", it) }
        }
    }

    private fun toUiInfo(s: BackupStatus) = BackupUiInfo(
        status = when (s.stateAt(System.currentTimeMillis())) {
            BackupState.OK -> BackupUiStatus.OK
            BackupState.FAILED -> BackupUiStatus.FAILED
            BackupState.STALE, BackupState.NEVER -> BackupUiStatus.PENDING
        },
        lastSuccessAt = s.lastSuccessAt,
        nextRunLabel = null,
    )

    private companion object {
        const val TAG = "VrajApp"
    }
}
