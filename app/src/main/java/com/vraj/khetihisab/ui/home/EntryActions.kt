package com.vraj.khetihisab.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkConfirmDeleteDialog
import com.vraj.khetihisab.ui.components.VkDangerButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import kotlinx.coroutines.launch

/**
 * One sheet for every ledger row in the app (Home, Samanya, Personal, Bhagiya, Farm, Calendar):
 * સુધારો (opens Entry pre-filled) or કાઢી નાખો (asks first, soft delete).
 */
@Composable
fun EntryActionsSheet(
    row: LedgerRowUi,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit = {},
    /** ભાગિયા ઉપાડ rows: send this one advance on WhatsApp (pre-filled, never auto-sent). */
    onWhatsApp: (() -> Unit)? = null,
) {
    val deps = rememberScreensDeps()
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()
    val deletedMsg = stringResource(R.string.v5_deleted)
    val undoLabel = stringResource(R.string.v5_undo)
    var confirm by remember { mutableStateOf(false) }
    HomeBottomSheet(row.title, onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("entry_actions"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${row.meta} · ${ScreensFormat.signedRupees(row.amountPaise)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onWhatsApp != null) {
                VkPrimaryButton(stringResource(R.string.scr_wa_send_receipt), { onDismiss(); onWhatsApp() }, Modifier.fillMaxWidth())
            }
            VkTonalButton(stringResource(R.string.v2_edit), { onDismiss(); onEdit() }, Modifier.fillMaxWidth())
            VkDangerButton(stringResource(R.string.scr_delete), { confirm = true }, Modifier.fillMaxWidth())
        }
    }
    if (confirm) {
        VkConfirmDeleteDialog(
            what = "${row.title} · ${ScreensFormat.signedRupees(row.amountPaise)}",
            onConfirm = {
                confirm = false
                val id = row.id
                // Activity-lifetime scope (VkRoot): this sheet leaves composition right after the tap.
                snackbar.scope.launch {
                    deps.repos.ledger.softDelete(id)
                    snackbar.show(deletedMsg, actionLabel = undoLabel) {
                        snackbar.scope.launch { deps.repos.ledger.restore(id) }
                    }
                }
                onDeleted()
                onDismiss()
            },
            onDismiss = { confirm = false },
        )
    }
}
