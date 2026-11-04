package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.R
import com.vraj.khetihisab.security.BiometricHelper
import com.vraj.khetihisab.security.PinStore
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkNumericKeypad
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.theme.VkIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PinStep { CURRENT, NEW, CONFIRM, DONE }

enum class PinMessage { WRONG, MISMATCH, LOCKED }

data class PinChangeState(
    val step: PinStep = PinStep.CURRENT,
    val entered: String = "",
    val newPin: String = "",
    val message: PinMessage? = null,
    val lockedMillis: Long = 0L,
    val busy: Boolean = false,
)

/** Result of checking the current PIN (maps [PinStore.VerifyResult]). */
enum class PinCheck { OK, WRONG, LOCKED }

/**
 * "PIN બદલો" (doc 04): current → new → confirm. Wrong current PINs count towards the lockout in
 * [PinStore.verify] exactly like the lock screen. Pure state machine; [verify] / [store] are injected.
 */
class PinChangeViewModel(
    private val verify: suspend (String) -> Pair<PinCheck, Long>,
    private val store: suspend (String) -> Unit,
    private val bioGet: () -> Boolean,
    private val bioSet: (Boolean) -> Unit,
    private val io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _state = MutableStateFlow(PinChangeState())
    val state: StateFlow<PinChangeState> = _state.asStateFlow()

    private val _bio = MutableStateFlow(false)
    val biometric: StateFlow<Boolean> = _bio.asStateFlow()

    init {
        viewModelScope.launch { _bio.value = withContext(io) { runCatching(bioGet).getOrDefault(false) } }
    }

    fun setBiometric(on: Boolean) {
        _bio.value = on
        viewModelScope.launch(io) { runCatching { bioSet(on) } }
    }

    fun onDigit(c: Char) {
        val s = _state.value
        if (s.busy || s.step == PinStep.DONE || s.entered.length >= PinStore.PIN_LENGTH || !c.isDigit()) return
        val entered = s.entered + c
        _state.value = s.copy(entered = entered, message = if (entered.length == 1) null else s.message)
        if (entered.length == PinStore.PIN_LENGTH) complete(entered)
    }

    fun onBackspace() = _state.update { if (it.busy) it else it.copy(entered = it.entered.dropLast(1)) }

    fun restart() {
        _state.value = PinChangeState()
    }

    private fun complete(pin: String) {
        val s = _state.value
        when (s.step) {
            PinStep.CURRENT -> {
                _state.value = s.copy(busy = true)
                viewModelScope.launch {
                    val (r, ms) = verify(pin)
                    _state.value = when (r) {
                        PinCheck.OK -> PinChangeState(step = PinStep.NEW)
                        PinCheck.WRONG -> PinChangeState(step = PinStep.CURRENT, message = PinMessage.WRONG)
                        PinCheck.LOCKED -> PinChangeState(step = PinStep.CURRENT, message = PinMessage.LOCKED, lockedMillis = ms)
                    }
                }
            }
            PinStep.NEW -> _state.value = PinChangeState(step = PinStep.CONFIRM, newPin = pin)
            PinStep.CONFIRM -> {
                if (pin != s.newPin) {
                    _state.value = PinChangeState(step = PinStep.NEW, message = PinMessage.MISMATCH)
                } else {
                    _state.value = s.copy(busy = true)
                    viewModelScope.launch {
                        store(pin)
                        _state.value = PinChangeState(step = PinStep.DONE)
                    }
                }
            }
            PinStep.DONE -> Unit
        }
    }

    companion object {
        fun create(pinStore: () -> PinStore) = PinChangeViewModel(
            verify = { pin ->
                withContext(Dispatchers.Default) {
                    when (val r = pinStore().verify(pin)) {
                        PinStore.VerifyResult.Success, PinStore.VerifyResult.NoPin -> PinCheck.OK to 0L
                        is PinStore.VerifyResult.Wrong -> PinCheck.WRONG to 0L
                        is PinStore.VerifyResult.LockedOut -> PinCheck.LOCKED to r.remainingMillis
                    }
                }
            },
            store = { pin -> withContext(Dispatchers.Default) { pinStore().setPin(pin) } },
            bioGet = { pinStore().biometricEnabled },
            bioSet = { pinStore().biometricEnabled = it },
        )
    }
}

@Composable
fun SecuritySettingsRoute(onBack: () -> Unit) {
    val vm: PinChangeViewModel = screenViewModel { deps, _ -> PinChangeViewModel.create { deps.pinStore } }
    SecuritySettingsScreen(vm, onBack)
}

@Composable
fun SecuritySettingsScreen(vm: PinChangeViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val bio by vm.biometric.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bioAvailable = remember { BiometricHelper.isAvailable(context) }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_set_pin), onBack = onBack)
        StBodyColumn {
            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                StSwitchRow(
                    title = stringResource(R.string.st_bio_toggle),
                    meta = if (bioAvailable) null else stringResource(R.string.st_bio_unavailable),
                    icon = VkIcon.LOCK,
                    checked = bio && bioAvailable,
                    enabled = bioAvailable,
                    onCheckedChange = vm::setBiometric,
                    onLabel = stringResource(R.string.st_on),
                    offLabel = stringResource(R.string.st_off),
                )
            }
            VkSectionTitle(stringResource(R.string.st_pin_change))
            VkCard(Modifier.fillMaxWidth()) {
                val prompt = when (s.step) {
                    PinStep.CURRENT -> R.string.st_pin_current
                    PinStep.NEW -> R.string.st_pin_new
                    PinStep.CONFIRM -> R.string.st_pin_confirm
                    PinStep.DONE -> R.string.st_pin_changed
                }
                Text(stringResource(prompt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                if (s.step != PinStep.DONE) {
                    PinDots(s.entered.length)
                    // Reserved line so the keypad never jumps (doc 01 §8.6).
                    val msg = when (s.message) {
                        PinMessage.WRONG -> stringResource(R.string.st_pin_wrong)
                        PinMessage.MISMATCH -> stringResource(R.string.st_pin_mismatch)
                        PinMessage.LOCKED -> stringResource(R.string.st_pin_locked, formatMmSs(s.lockedMillis))
                        null -> " "
                    }
                    if (s.message != null) StError(msg) else StNote(msg)
                    VkNumericKeypad(onKey = vm::onDigit, onBackspace = vm::onBackspace, allowDecimal = false)
                } else {
                    StNote(stringResource(R.string.st_pin_changed_note))
                }
            }
        }
    }
}

internal fun formatMmSs(ms: Long): String {
    val total = (ms + 999) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun PinDots(count: Int) {
    val cs = MaterialTheme.colorScheme
    val desc = stringResource(R.string.st_pin_digits, count)
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        repeat(PinStore.PIN_LENGTH) { i ->
            val filled = i < count
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (filled) cs.primary else cs.surfaceContainerLowest)
                    .border(2.dp, cs.primary, CircleShape),
            )
        }
    }
}
