package com.vraj.khetihisab.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.backup.BackupGraph
import com.vraj.khetihisab.backup.BackupScheduler
import com.vraj.khetihisab.backup.BackupStatus
import com.vraj.khetihisab.backup.BackupTrigger
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.repo.PanchangPrefRepository
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.notify.PanchangAlertSettings
import com.vraj.khetihisab.notify.PanchangAlertSettingsStore
import com.vraj.khetihisab.notify.PanchangNotifier
import com.vraj.khetihisab.settings.ThemePrefs
import com.vraj.khetihisab.settings.ThemeRepository
import com.vraj.khetihisab.ui.home.ScreensDeps
import com.vraj.khetihisab.ui.theme.VkThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the main Settings screen shows (artboard 15). */
data class SettingsUiState(
    val theme: ThemePrefs = ThemePrefs(VkThemeId.DEFAULT, false),
    val prefs: Map<PanchangAlertType, PanchangAlertPrefEntity> = emptyMap(),
    val alert: PanchangAlertSettings = PanchangAlertSettings(),
    val backup: BackupStatus = BackupStatus(),
    val backupConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    /** "હોમ પર રકમ હંમેશાં બતાવો". */
    val showHomeNumbers: Boolean = false,
) {
    fun enabled(type: PanchangAlertType): Boolean = prefs[type]?.enabled ?: true
}

/**
 * Main Settings: theme, Panchang alert switches (Room prefs + [PanchangAlertSettingsStore], the only
 * store for evening time / guidance / વદ બીજ), backup card and the biometric toggle.
 * Every Panchang change re-arms the evening alarm via [reschedule].
 */
class SettingsViewModel(
    private val themes: ThemeRepository,
    private val panchangPrefs: PanchangPrefRepository,
    private val alertStore: PanchangAlertSettingsStore,
    backupStatus: Flow<BackupStatus>,
    private val backupConfigured: () -> Boolean,
    private val backupNowAction: () -> Unit,
    private val biometricGet: () -> Boolean,
    private val biometricSet: (Boolean) -> Unit,
    private val reschedule: suspend () -> Unit,
) : ViewModel() {

    private val bio = MutableStateFlow(false)
    private val configured = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        combine(themes.prefs, panchangPrefs.prefsMap(), alertStore.settings) { t, p, a -> Triple(t, p, a) },
        backupStatus,
        bio,
        configured,
        themes.showHomeNumbers,
    ) { (t, p, a), b, bioOn, conf, nums ->
        SettingsUiState(theme = t, prefs = p, alert = a, backup = b, backupConfigured = conf, biometricEnabled = bioOn, showHomeNumbers = nums)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshSecure()
    }

    /** EncryptedSharedPreferences reads are slow on first touch: keep them off the main thread. */
    fun refreshSecure() {
        viewModelScope.launch {
            val (b, c) = withContext(Dispatchers.IO) {
                runCatching { biometricGet() }.getOrDefault(false) to runCatching { backupConfigured() }.getOrDefault(false)
            }
            bio.value = b
            configured.value = c
        }
    }

    fun setTheme(id: VkThemeId) = viewModelScope.launch { themes.setTheme(id) }

    fun setFollowSystem(on: Boolean) = viewModelScope.launch { themes.setFollowSystemDark(on) }

    fun setShowHomeNumbers(on: Boolean) = viewModelScope.launch { themes.setShowHomeNumbers(on) }

    fun setTypeEnabled(type: PanchangAlertType, on: Boolean) = viewModelScope.launch {
        panchangPrefs.setEnabled(type, on)
        runCatching { reschedule() }
    }

    fun setVadBij(on: Boolean) = viewModelScope.launch {
        alertStore.setIncludeVadBij(on)
        runCatching { reschedule() }
    }

    fun setShowGuidance(on: Boolean) = viewModelScope.launch { alertStore.setShowGuidance(on) }

    fun setEveningTime(hour: Int, minute: Int) = viewModelScope.launch {
        alertStore.setEveningTime(hour, minute)
        runCatching { reschedule() }
    }

    fun backupNow() = backupNowAction()

    fun setBiometric(on: Boolean) {
        bio.value = on
        viewModelScope.launch(Dispatchers.IO) { runCatching { biometricSet(on) } }
    }

    companion object {
        fun create(deps: ScreensDeps): SettingsViewModel {
            val ctx: Context = deps.appContext
            return SettingsViewModel(
                themes = deps.themeRepository,
                panchangPrefs = deps.repos.panchang,
                alertStore = NotifyGraph.settings(ctx),
                backupStatus = runCatching { BackupGraph.deps(ctx).status.status }.getOrElse { flowOf(BackupStatus()) },
                backupConfigured = { BackupGraph.deps(ctx).prefs.driveConfigured },
                backupNowAction = deps.onBackupNow ?: { BackupScheduler(ctx).enqueueBackup(BackupTrigger.MANUAL) },
                biometricGet = { deps.pinStore.biometricEnabled },
                biometricSet = { deps.pinStore.biometricEnabled = it },
                reschedule = { PanchangNotifier.reschedule(ctx) },
            )
        }
    }
}
