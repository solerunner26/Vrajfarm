package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.share.WhatsAppTemplateStore
import com.vraj.khetihisab.share.WhatsAppTemplates
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TemplatesUi(
    val loan: String = "",
    val family: String = "",
    val savedLoan: Boolean = false,
    val savedFamily: Boolean = false,
)

/**
 * WhatsApp template editor (doc 12, Settings › "WhatsApp મેસેજ નમૂના"). Saving the default text or a blank
 * value stores null, so later default changes still apply. Placeholders are fixed.
 */
class TemplatesViewModel(private val store: WhatsAppTemplateStore) : ViewModel() {
    private val _ui = MutableStateFlow(TemplatesUi(store.loanTemplate(), store.familyTemplate()))
    val ui: StateFlow<TemplatesUi> = _ui.asStateFlow()

    fun onLoan(t: String) = _ui.update { it.copy(loan = t, savedLoan = false) }
    fun onFamily(t: String) = _ui.update { it.copy(family = t, savedFamily = false) }

    fun saveLoan() {
        val t = _ui.value.loan
        store.setLoanTemplate(t.takeIf { it.isNotBlank() && it != WhatsAppTemplates.DEFAULT_LOAN_REMINDER })
        _ui.update { it.copy(loan = store.loanTemplate(), savedLoan = true) }
    }

    fun saveFamily() {
        val t = _ui.value.family
        store.setFamilyTemplate(t.takeIf { it.isNotBlank() && it != WhatsAppTemplates.DEFAULT_FAMILY_STATEMENT })
        _ui.update { it.copy(family = store.familyTemplate(), savedFamily = true) }
    }

    fun resetLoan() {
        store.setLoanTemplate(null)
        _ui.update { it.copy(loan = WhatsAppTemplates.DEFAULT_LOAN_REMINDER, savedLoan = true) }
    }

    fun resetFamily() {
        store.setFamilyTemplate(null)
        _ui.update { it.copy(family = WhatsAppTemplates.DEFAULT_FAMILY_STATEMENT, savedFamily = true) }
    }

    companion object {
        val LOAN_PLACEHOLDERS = listOf(
            WhatsAppTemplates.P_BORROWER, WhatsAppTemplates.P_ASSET, WhatsAppTemplates.P_HANDED_OVER,
            WhatsAppTemplates.P_EXPECTED_RETURN, WhatsAppTemplates.P_LATE, WhatsAppTemplates.P_OWNER, WhatsAppTemplates.P_VILLAGE,
        )
        val FAMILY_PLACEHOLDERS = listOf(
            WhatsAppTemplates.P_DISPLAY_NAME, WhatsAppTemplates.P_MONTH, WhatsAppTemplates.P_YEAR,
            WhatsAppTemplates.P_MONTH_TOTAL, WhatsAppTemplates.P_ENTRIES, WhatsAppTemplates.P_YEAR_TOTAL,
            WhatsAppTemplates.P_OWNER, WhatsAppTemplates.P_VILLAGE,
        )
    }
}

@Composable
fun TemplatesSettingsRoute(onBack: () -> Unit) {
    val vm: TemplatesViewModel = screenViewModel { deps, _ -> TemplatesViewModel(WhatsAppTemplateStore(deps.appContext)) }
    val u by vm.ui.collectAsStateWithLifecycle()
    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_set_templates), onBack = onBack)
        StBodyColumn {
            StNote(stringResource(R.string.st_tpl_note))
            VkSectionTitle(stringResource(R.string.st_tpl_loan))
            VkCard(Modifier.fillMaxWidth()) {
                StTextField(u.loan, vm::onLoan, stringResource(R.string.st_tpl_message), singleLine = false, minLines = 6)
                StNote(stringResource(R.string.st_tpl_placeholders, TemplatesViewModel.LOAN_PLACEHOLDERS.joinToString(" ")))
                VkPrimaryButton(stringResource(R.string.st_save), vm::saveLoan, Modifier.fillMaxWidth())
                VkTextButton(stringResource(R.string.st_tpl_reset), vm::resetLoan, Modifier.fillMaxWidth())
                if (u.savedLoan) StNote(stringResource(R.string.st_saved))
            }
            VkSectionTitle(stringResource(R.string.st_tpl_family))
            VkCard(Modifier.fillMaxWidth()) {
                StTextField(u.family, vm::onFamily, stringResource(R.string.st_tpl_message), singleLine = false, minLines = 6)
                StNote(stringResource(R.string.st_tpl_placeholders, TemplatesViewModel.FAMILY_PLACEHOLDERS.joinToString(" ")))
                VkPrimaryButton(stringResource(R.string.st_save), vm::saveFamily, Modifier.fillMaxWidth())
                VkTextButton(stringResource(R.string.st_tpl_reset), vm::resetFamily, Modifier.fillMaxWidth())
                if (u.savedFamily) StNote(stringResource(R.string.st_saved))
            }
        }
    }
}
