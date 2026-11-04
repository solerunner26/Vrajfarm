package com.vraj.khetihisab.ui.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vraj.khetihisab.VrajApp
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.security.LockManager
import com.vraj.khetihisab.security.PinStore
import com.vraj.khetihisab.settings.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Home backup pill (doc 05 §3): OK within 26 h, બાકી when never run, નિષ્ફળ after a failure. */
enum class BackupUiStatus { OK, PENDING, FAILED }

/** Backup card on Settings (doc 15). Times are epoch millis; null = never. */
data class BackupUiInfo(
    val status: BackupUiStatus = BackupUiStatus.PENDING,
    val lastSuccessAt: Long? = null,
    val nextRunLabel: String? = null,
)

/**
 * Everything the compose-screens ViewModels need. Built from the app's [com.vraj.khetihisab.di.AppContainer]
 * by default; the lead integrator may [ScreensDepsProvider.install] a fully wired instance (shared
 * [DataRepositories] with the widget DataChangeListener, real backup status, "backup now").
 */
class ScreensDeps(
    val appContext: Context,
    val repos: DataRepositories,
    val themeRepository: ThemeRepository,
    private val pinStoreFactory: () -> PinStore,
    val lockManager: LockManager,
    val backupInfo: Flow<BackupUiInfo> = flowOf(BackupUiInfo()),
    val onBackupNow: (() -> Unit)? = null,
) {
    val pinStore: PinStore by lazy { pinStoreFactory() }

    /** UI-only preferences (Panchang dialog dismissals, show-guidance, evening time, વદ બીજ). */
    val uiPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        const val UI_PREFS_NAME = "vk_screens_prefs"
    }
}

object ScreensDepsProvider {
    @Volatile private var installed: ScreensDeps? = null

    /** Called by the lead from AppContainer / VrajApp to share its repositories and backup status. */
    fun install(deps: ScreensDeps) {
        installed = deps
    }

    fun get(context: Context): ScreensDeps = installed ?: synchronized(this) {
        installed ?: buildDefault(context.applicationContext).also { installed = it }
    }

    private fun buildDefault(app: Context): ScreensDeps {
        val container = (app as VrajApp).container
        return ScreensDeps(
            appContext = app,
            // AppDatabase.get is the process singleton, so Flows stay in sync with other modules.
            repos = DataRepositories(AppDatabase.get(app)),
            themeRepository = container.themeRepository,
            pinStoreFactory = { container.pinStore },
            lockManager = container.lockManager,
        )
    }
}

/** ViewModel bound to the current NavBackStackEntry, created with [ScreensDeps] + its SavedStateHandle. */
@Composable
inline fun <reified VM : ViewModel> screenViewModel(
    key: String? = null,
    crossinline create: (ScreensDeps, SavedStateHandle) -> VM,
): VM {
    val ctx = LocalContext.current
    return viewModel(key = key) { create(ScreensDepsProvider.get(ctx), createSavedStateHandle()) }
}

@Composable
fun rememberScreensDeps(): ScreensDeps = ScreensDepsProvider.get(LocalContext.current)
