package com.vraj.khetihisab.ui.panchang

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.R
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.notify.PanchangNotifier
import com.vraj.khetihisab.panchang.AlertPurpose
import com.vraj.khetihisab.panchang.PanchangAlert
import com.vraj.khetihisab.ui.components.VkFestivalDialog
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.panchang.PanchangCalendarViewModel.Companion.toOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The once-per-day Panchang dialog (doc 10 scheduling §5): shown when `alertFor(today, DAY_DIALOG)` is
 * not null and `panchang_dialog_dismissed_<epochDay>` is absent. Any action writes that key
 * (PanchangAlertSettingsStore.markDialogDismissed), so the dialog never comes back the same day.
 */
class PanchangAlertViewModel(
    private val loadAlert: suspend (LocalDate) -> PanchangAlert?,
    private val isDismissed: suspend (LocalDate) -> Boolean,
    private val markDismissed: suspend (LocalDate) -> Unit,
    private val createReminder: suspend (LocalDate, String, String) -> ReminderOutcome,
    private val today: () -> LocalDate,
) : ViewModel() {

    private val _alert = MutableStateFlow<PanchangAlert?>(null)
    val alert: StateFlow<PanchangAlert?> = _alert.asStateFlow()

    private val _events = MutableSharedFlow<ReminderOutcome>(extraBufferCapacity = 2)
    val events: SharedFlow<ReminderOutcome> = _events.asSharedFlow()

    init {
        refresh()
    }

    /** Re-evaluates today (call on resume: the day may have changed). */
    fun refresh() {
        viewModelScope.launch {
            val d = today()
            _alert.value = runCatching { if (isDismissed(d)) null else loadAlert(d) }.getOrNull()
        }
    }

    /** "સમજાયું" or tapping outside. */
    fun dismiss() = closeWith { }

    fun makeReminder() = closeWith { a ->
        _events.emit(runCatching { createReminder(a.special.date, a.special.titleGu, a.special.day.tithiGu) }.getOrDefault(ReminderOutcome.FAILED))
    }

    /** "કેલેન્ડર જુઓ": returns the day to open. */
    fun openCalendar(): LocalDate {
        val d = _alert.value?.special?.date ?: today()
        closeWith { }
        return d
    }

    private fun closeWith(action: suspend (PanchangAlert) -> Unit) {
        val a = _alert.value ?: return
        _alert.value = null
        viewModelScope.launch {
            runCatching { markDismissed(a.special.date) }
            action(a)
        }
    }

    companion object {
        fun create(context: android.content.Context): PanchangAlertViewModel {
            val app = context.applicationContext
            val store = NotifyGraph.settings(app)
            val reminders = NotifyGraph.repos(app).reminders
            return PanchangAlertViewModel(
                loadAlert = { d -> withContext(Dispatchers.Default) { PanchangNotifier.alertFor(app, d, AlertPurpose.DAY_DIALOG) } },
                isDismissed = store::isDialogDismissed,
                markDismissed = store::markDialogDismissed,
                createReminder = { d, t, n -> reminders.createPanchangReminder(d, t, n).toOutcome() },
                today = { LocalDate.now(PanchangUi.IST) },
            )
        }
    }
}

/**
 * Drop-in host for Home (doc 05/10): `PanchangAlertHost(onOpenCalendar = { nav.navigate(SettingsRoutes.calendar(it)) })`.
 * Renders nothing unless today's dialog is due.
 */
@Composable
fun PanchangAlertHost(onOpenCalendar: (epochDay: Long) -> Unit) {
    val vm: PanchangAlertViewModel = screenViewModel(key = "panchang_alert_host") { deps, _ -> PanchangAlertViewModel.create(deps.appContext) }
    PanchangAlertHost(vm, onOpenCalendar)
}

@Composable
fun PanchangAlertHost(vm: PanchangAlertViewModel, onOpenCalendar: (epochDay: Long) -> Unit) {
    val alert by vm.alert.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val created = stringResource(R.string.st_reminder_created)
    val exists = stringResource(R.string.st_reminder_exists)
    val failed = stringResource(R.string.st_reminder_failed)
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()
    LaunchedEffect(vm) {
        vm.events.collect { o ->
            when (o) {
                ReminderOutcome.CREATED -> snackbar.show(created, success = true)
                ReminderOutcome.EXISTS -> snackbar.show(exists)
                ReminderOutcome.FAILED -> snackbar.show(failed)
            }
        }
    }
    alert?.let { a ->
        VkFestivalDialog(
            alert = a,
            onDismiss = vm::dismiss,
            onCreateReminder = vm::makeReminder,
            onOpenCalendar = { onOpenCalendar(vm.openCalendar().toEpochDay()) },
        )
    }
}
