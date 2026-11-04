package com.vraj.khetihisab.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkTextButton
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
) {
    val deps = rememberScreensDeps()
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
            VkTonalButton(stringResource(R.string.v2_edit), { onDismiss(); onEdit() }, Modifier.fillMaxWidth())
            VkOutlinedButton(stringResource(R.string.scr_delete), { confirm = true }, Modifier.fillMaxWidth())
            VkTextButton(stringResource(R.string.scr_cancel), onDismiss, Modifier.fillMaxWidth())
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.scr_delete_entry_title)) },
            text = { Text("${row.title} · ${ScreensFormat.signedRupees(row.amountPaise)}") },
            confirmButton = {
                VkPrimaryButton(stringResource(R.string.scr_delete), {
                    confirm = false
                    // App-lifetime scope: this sheet leaves composition right after the tap.
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        deps.repos.ledger.softDelete(row.id)
                    }
                    onDeleted()
                    onDismiss()
                })
            },
            dismissButton = { VkTextButton(stringResource(R.string.scr_cancel), { confirm = false }) },
        )
    }
}
