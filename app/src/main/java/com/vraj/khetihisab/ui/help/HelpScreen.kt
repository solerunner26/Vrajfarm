package com.vraj.khetihisab.ui.help

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.BuildConfig
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.repo.ProfileRepository
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.settings.StBodyColumn
import com.vraj.khetihisab.ui.settings.StNote
import com.vraj.khetihisab.ui.settings.StScrollBody
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import com.vraj.khetihisab.ui.theme.VkLogoImage
import com.vraj.khetihisab.ui.theme.VkWordmarkStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Owner block reads the OWNER profile, so edits on Profiles show here (doc 14 §4). */
class HelpViewModel(profiles: ProfileRepository) : ViewModel() {
    val owner: StateFlow<PersonProfileEntity?> = profiles.owner().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** FAQ item: icon, question and answer string resources (doc 14 table, canvas 16 copy). */
internal data class Faq(val icon: VkIcon, val q: Int, val a: Int)

internal val HelpFaqs = listOf(
    Faq(VkIcon.INCOME, R.string.st_faq_q1, R.string.st_faq_a1),
    Faq(VkIcon.CALCULATOR, R.string.st_faq_q2, R.string.st_faq_a2),
    Faq(VkIcon.MOON, R.string.st_faq_q5, R.string.st_faq_a5),
    Faq(VkIcon.CAMERA, R.string.st_faq_q3, R.string.st_faq_a3),
    Faq(VkIcon.MESSAGE, R.string.st_faq_q4, R.string.st_faq_a4),
    Faq(VkIcon.BACKUP, R.string.st_faq_q6, R.string.st_faq_a6),
    Faq(VkIcon.LOCK, R.string.st_faq_q7, R.string.st_faq_a7),
)

@Composable
fun HelpRoute(onBack: () -> Unit) {
    val vm: HelpViewModel = screenViewModel { deps, _ -> HelpViewModel(deps.repos.profiles) }
    HelpScreen(vm, onBack)
}

/**
 * Help & About (doc 14, artboard 16). The developer credit lives ONLY here
 * (CreditOnlyInHelpTest scans the sources for the st_credit_* keys).
 */
@Composable
fun HelpScreen(vm: HelpViewModel, onBack: () -> Unit) {
    val owner by vm.owner.collectAsStateWithLifecycle()
    val x = LocalVkExtra.current
    val cs = MaterialTheme.colorScheme
    var open by rememberSaveable { mutableIntStateOf(-1) }
    var licenses by rememberSaveable { mutableStateOf(false) }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_help_title), onBack = onBack) {
            // App identity on the gradient band (canvas 16).
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VkLogoImage(size = 96.dp)
                Text(stringResource(R.string.app_wordmark), style = VkWordmarkStyle, color = x.onGradient, maxLines = 1)
                Text(stringResource(R.string.st_app_latin), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp), color = x.onGradient.copy(alpha = 0.88f), maxLines = 1)
                Text(stringResource(R.string.st_version_line, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = x.onGradient.copy(alpha = 0.88f), maxLines = 1)
            }
        }
        StBodyColumn {
            VkSectionTitle(stringResource(R.string.st_faq))
            HelpFaqs.forEachIndexed { i, f ->
                FaqItem(f, expanded = open == i, onToggle = { open = if (open == i) -1 else i })
            }

            // Owner block (values from the OWNER profile; English line is static).
            VkCard(Modifier.fillMaxWidth()) {
                VkBadge(stringResource(R.string.st_role_owner), cs.primaryContainer, cs.onPrimaryContainer)
                Text(owner?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.st_owner_name_default), style = MaterialTheme.typography.headlineSmall)
                Text(owner?.address?.takeIf { it.isNotBlank() } ?: stringResource(R.string.st_owner_address_default), style = MaterialTheme.typography.bodyLarge)
                StNote(stringResource(R.string.st_owner_en))
            }

            VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                VkListRow(
                    title = stringResource(R.string.st_licenses),
                    leadingIcon = VkIcon.HELP,
                    onClick = { licenses = !licenses },
                    trailing = { Text(if (licenses) "−" else "+", style = MaterialTheme.typography.headlineSmall, color = cs.onSurfaceVariant) },
                )
                if (licenses) {
                    StNote(stringResource(R.string.st_licenses_body), Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
                }
            }

            // Credit block: the LAST item, and the only place in the app with the developer credit.
            VkCard(Modifier.fillMaxWidth(), tone = VkTone.Primary) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.st_credit_en), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.st_credit_name), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.st_credit_gu), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun FaqItem(f: Faq, expanded: Boolean, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val state = stringResource(if (expanded) R.string.st_expanded else R.string.st_collapsed)
    VkCard(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { stateDescription = state },
        onClick = onToggle,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VkIconImage(f.icon, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(f.q), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.headlineSmall, color = cs.onSurfaceVariant)
        }
        if (expanded) {
            Text(stringResource(f.a), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }
    }
}
