package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.R
import com.vraj.khetihisab.backup.BackupFailure
import com.vraj.khetihisab.backup.BackupGraph
import com.vraj.khetihisab.backup.BackupSettings
import com.vraj.khetihisab.backup.BackupUploader
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Result of "કનેક્શન તપાસો" (`GET ?ping=1`, doc 15 edge cases). */
sealed interface PingResult {
    data object Running : PingResult
    data object Ok : PingResult
    data class Failed(val failure: BackupFailure) : PingResult
}

data class BackupSettingsUi(
    val url: String = "",
    /** Never holds the stored key: only what the owner types now (blank = keep the stored one). */
    val keyInput: String = "",
    val hasKey: Boolean = false,
    val urlError: BackupSettings.EndpointProblem? = null,
    val keyError: BackupSettings.KeyProblem? = null,
    val saved: Boolean = false,
    val ping: PingResult? = null,
    val loaded: Boolean = false,
)

/**
 * Endpoint URL + API key entry (doc 15 "Settings"). The key is stored only in
 * EncryptedSharedPreferences via [BackupSettings]; it is never shown, logged or kept in UI state after save.
 */
class BackupSettingsViewModel(
    private val settingsProvider: () -> BackupSettings,
    private val uploader: BackupUploader,
) : ViewModel() {
    private val _ui = MutableStateFlow(BackupSettingsUi())
    val ui: StateFlow<BackupSettingsUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val (url, has) = withContext(Dispatchers.IO) {
                val s = settingsProvider()
                s.endpointUrl.takeIf { it != BackupSettings.DEFAULT_URL }.orEmpty() to s.hasApiKey()
            }
            _ui.update { it.copy(url = url, hasKey = has, loaded = true) }
        }
    }

    fun onUrl(v: String) = _ui.update { it.copy(url = v, urlError = null, saved = false, ping = null) }
    fun onKey(v: String) = _ui.update { it.copy(keyInput = v, keyError = null, saved = false, ping = null) }

    fun save() {
        val u = _ui.value
        viewModelScope.launch {
            val (urlErr, keyErr, has) = withContext(Dispatchers.IO) {
                val s = settingsProvider()
                val ue = BackupSettings.validateEndpoint(u.url.trim())
                val ke = if (u.keyInput.isBlank()) null else BackupSettings.validateKey(u.keyInput.trim())
                if (ue == null && ke == null) {
                    s.setEndpoint(u.url)
                    if (u.keyInput.isNotBlank()) s.setApiKey(u.keyInput)
                }
                Triple(ue, ke, s.hasApiKey())
            }
            _ui.update {
                it.copy(
                    urlError = urlErr,
                    keyError = keyErr,
                    hasKey = has,
                    saved = urlErr == null && keyErr == null,
                    keyInput = if (urlErr == null && keyErr == null) "" else it.keyInput,
                )
            }
            if (urlErr == null && keyErr == null && has) ping()
        }
    }

    fun ping() {
        _ui.update { it.copy(ping = PingResult.Running) }
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                val s = settingsProvider()
                val url = s.endpointHttpUrl() ?: return@withContext PingResult.Failed(BackupFailure.WRONG_URL)
                val key = s.apiKey() ?: return@withContext PingResult.Failed(BackupFailure.NOT_CONFIGURED)
                when (val o = uploader.ping(url, key, s.installId)) {
                    is BackupUploader.Outcome.NetworkError -> PingResult.Failed(BackupFailure.NETWORK)
                    is BackupUploader.Outcome.Http -> o.response.failure?.let { PingResult.Failed(it) } ?: PingResult.Ok
                }
            }
            _ui.update { it.copy(ping = r) }
        }
    }

    companion object {
        fun create(context: android.content.Context) = BackupSettingsViewModel(
            settingsProvider = { BackupGraph.deps(context).settings },
            uploader = BackupGraph.deps(context).uploader,
        )
    }
}

@Composable
fun BackupSettingsRoute(onBack: () -> Unit) {
    val vm: BackupSettingsViewModel = screenViewModel { deps, _ -> BackupSettingsViewModel.create(deps.appContext) }
    BackupSettingsScreen(vm, onBack)
}

@Composable
fun BackupSettingsScreen(vm: BackupSettingsViewModel, onBack: () -> Unit) {
    val u by vm.ui.collectAsStateWithLifecycle()
    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_backup_title), subtitle = stringResource(R.string.st_backup_sub), onBack = onBack)
        StBodyColumn {
            VkSectionTitle(stringResource(R.string.st_backup_configure))
            VkCard(Modifier.fillMaxWidth()) {
                StTextField(
                    value = u.url,
                    onValueChange = vm::onUrl,
                    label = stringResource(R.string.st_bk_endpoint),
                    placeholder = stringResource(R.string.st_bk_endpoint_hint),
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = u.urlError != null,
                    supporting = u.urlError?.let { stringResource(urlErrorRes(it)) },
                )
                StTextField(
                    value = u.keyInput,
                    onValueChange = vm::onKey,
                    label = stringResource(R.string.st_bk_key),
                    placeholder = stringResource(if (u.hasKey) R.string.st_bk_key_set else R.string.st_bk_key_hint),
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                    password = true,
                    isError = u.keyError != null,
                    supporting = u.keyError?.let { stringResource(keyErrorRes(it)) },
                )
                StNote(stringResource(R.string.st_bk_key_note))
                VkPrimaryButton(stringResource(R.string.st_save), vm::save, Modifier.fillMaxWidth(), enabled = u.loaded)
                VkTonalButton(stringResource(R.string.st_bk_test), vm::ping, Modifier.fillMaxWidth(), enabled = u.hasKey && u.ping != PingResult.Running)
                if (u.saved) StNote(stringResource(R.string.st_saved))
                when (val p = u.ping) {
                    PingResult.Running -> StNote(stringResource(R.string.st_bk_testing))
                    PingResult.Ok -> StNote(stringResource(R.string.st_bk_test_ok))
                    is PingResult.Failed -> StError(stringResource(p.failure.messageRes))
                    null -> Unit
                }
            }
        }
    }
}

private fun urlErrorRes(p: BackupSettings.EndpointProblem): Int = when (p) {
    BackupSettings.EndpointProblem.INVALID -> R.string.st_bk_url_invalid
    BackupSettings.EndpointProblem.NOT_HTTPS -> R.string.st_bk_url_https
    BackupSettings.EndpointProblem.PLACEHOLDER -> R.string.st_bk_url_placeholder
}

private fun keyErrorRes(p: BackupSettings.KeyProblem): Int = when (p) {
    BackupSettings.KeyProblem.TOO_SHORT -> R.string.st_bk_key_short
    BackupSettings.KeyProblem.INVALID_CHARS -> R.string.st_bk_key_chars
}
