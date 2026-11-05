package com.vraj.khetihisab.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.backup.BackupGraph
import com.vraj.khetihisab.backup.BackupShare
import com.vraj.khetihisab.backup.BackupState
import com.vraj.khetihisab.backup.BackupStatus
import com.vraj.khetihisab.backup.JsonBackup
import com.vraj.khetihisab.backup.RestoreInbox
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Backup (owner request 2026-11-04): one .json of everything →
 * ① Google Drive, automatic every night after choosing a Drive file once,
 * ② WhatsApp (opens the chat picker), ③ e-mail (Gmail opens with the file attached).
 * Restore: pick the .json from Drive / WhatsApp / Downloads / Gmail, or "Open with" it from WhatsApp.
 */
@Composable
fun BackupSettingsRoute(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val deps = remember { BackupGraph.deps(ctx) }
    val status by remember { runCatching { deps.status.status }.getOrElse { flowOf(BackupStatus()) } }
        .collectAsStateWithLifecycle(BackupStatus())
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    var driveSet by remember { mutableStateOf(deps.prefs.driveConfigured) }
    var driveLabel by remember { mutableStateOf(deps.prefs.driveLabel) }
    var email by remember { mutableStateOf(deps.prefs.email) }
    // Gmail one-tap sending (encrypted settings; read off the main thread below).
    var mailReady by remember { mutableStateOf(false) }
    var mailUser by remember { mutableStateOf(deps.prefs.email) }
    var mailPass by remember { mutableStateOf("") }
    var mailNightly by remember { mutableStateOf(false) }
    var editMail by remember { mutableStateOf(false) }
    var mailError by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { deps.mail } }.getOrNull()?.let { m ->
            mailReady = m.configured
            if (m.gmail.isNotBlank()) mailUser = m.gmail
            mailNightly = m.nightly
        }
    }
    val msgMailSent = stringResource(R.string.v3_mail_sent)
    val msgMailAuth = stringResource(R.string.v3_mail_err_auth)
    val msgMailNet = stringResource(R.string.v3_mail_err_net)
    val msgMailFail = stringResource(R.string.v3_mail_err_fail)
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreInfo by remember { mutableStateOf<JsonBackup.Summary?>(null) }
    var restoreBad by remember { mutableStateOf(false) }

    val msgDriveOk = stringResource(R.string.v2_bk_drive_saved)
    val msgDriveFail = stringResource(R.string.v2_bk_err_drive)
    val msgFail = stringResource(R.string.bk_err_local)
    val msgNoApp = stringResource(R.string.v2_bk_no_app)

    // A file opened with "Open with" from WhatsApp / Gmail.
    val inbox by RestoreInbox.uri.collectAsStateWithLifecycle()
    LaunchedEffect(inbox) {
        inbox?.let { restoreUri = it; RestoreInbox.uri.value = null }
    }
    LaunchedEffect(restoreUri) {
        val u = restoreUri ?: return@LaunchedEffect
        restoreInfo = withContext(Dispatchers.IO) {
            runCatching { ctx.contentResolver.openInputStream(u)?.use { JsonBackup.inspect(it) } }.getOrNull()
        }
        restoreBad = restoreInfo == null
    }

    /** Fresh file, then [then] with it. */
    fun withFreshFile(then: suspend (File) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            val f = runCatching { withContext(Dispatchers.IO) { deps.makeBackupFile() } }.getOrNull()
            if (f == null) message = msgFail else then(f)
            busy = false
        }
    }

    val pickDrive = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JsonBackup.MIME)) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        deps.prefs.driveUri = uri
        deps.prefs.driveLabel = uri.authority?.let(::providerLabel)
        driveSet = true
        driveLabel = deps.prefs.driveLabel
        // First copy right away, so the user sees it work.
        withFreshFile { f ->
            val ok = withContext(Dispatchers.IO) { BackupShare.writeToDrive(ctx, uri, f) }
            if (ok) deps.status.recordSuccess(f.length(), "drive")
            message = if (ok) msgDriveOk else msgDriveFail
        }
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_backup_title), onBack = onBack)
        StBodyColumn {
            // ---- status ----
            val state = status.stateAt(System.currentTimeMillis())
            VkCard(Modifier.fillMaxWidth().testTag("backup_status")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VkIconImage(VkIcon.BACKUP, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.v2_bk_last), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            status.lastSuccessAt?.let { lastText(ctx, it) } ?: stringResource(R.string.st_backup_never),
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val (bg, fg) = when (state) {
                        BackupState.OK -> cs.primaryContainer to cs.onPrimaryContainer
                        BackupState.FAILED -> cs.errorContainer to cs.onErrorContainer
                        BackupState.STALE, BackupState.NEVER -> cs.surfaceContainer to cs.onSurface
                    }
                    VkBadge(stringResource(state.pillRes), bg, fg)
                }
                if (state == BackupState.FAILED) status.lastFailure?.let { StError(stringResource(it.messageRes)) }
                if (busy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                message?.let { StNote(it) }
            }

            // ---- 1. Google Drive (automatic) ----
            VkSectionTitle(stringResource(R.string.v2_bk_drive_title))
            VkCard(Modifier.fillMaxWidth()) {
                StNote(stringResource(if (driveSet) R.string.v2_bk_drive_on else R.string.v2_bk_drive_how))
                if (driveSet) {
                    driveLabel?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) }
                    VkPrimaryButton(stringResource(R.string.v2_bk_drive_now), {
                        val uri = deps.prefs.driveUri ?: return@VkPrimaryButton
                        withFreshFile { f ->
                            val ok = withContext(Dispatchers.IO) { BackupShare.writeToDrive(ctx, uri, f) }
                            if (ok) deps.status.recordSuccess(f.length(), "drive") else deps.status.recordFailure(com.vraj.khetihisab.backup.BackupFailure.DRIVE)
                            message = if (ok) msgDriveOk else msgDriveFail
                        }
                    }, Modifier.fillMaxWidth().testTag("backup_drive_now"), enabled = !busy)
                    VkOutlinedButton(stringResource(R.string.v2_bk_drive_change), { pickDrive.launch(JsonBackup.fileName(deps.clock())) }, Modifier.fillMaxWidth())
                } else {
                    VkPrimaryButton(stringResource(R.string.v2_bk_drive_pick), { pickDrive.launch("VrajKheti-backup.json") }, Modifier.fillMaxWidth().testTag("backup_drive_pick"))
                }
            }

            // ---- 2. WhatsApp ----
            VkSectionTitle(stringResource(R.string.v2_bk_wa_title))
            VkCard(Modifier.fillMaxWidth()) {
                StNote(stringResource(R.string.v2_bk_wa_how))
                VkTonalButton(stringResource(R.string.v2_bk_wa_send), {
                    withFreshFile { f ->
                        if (BackupShare.whatsApp(ctx, f)) deps.status.recordSuccess(f.length(), "whatsapp") else message = msgNoApp
                    }
                }, Modifier.fillMaxWidth().testTag("backup_whatsapp"), enabled = !busy)
            }

            // ---- 3. E-mail ----
            VkSectionTitle(stringResource(R.string.v2_bk_mail_title))
            VkCard(Modifier.fillMaxWidth().testTag("backup_mail_card")) {
                if (mailReady && !editMail) {
                    // One tap: built + sent in the background, no Gmail screen (owner request 2026-11-05).
                    StNote(stringResource(R.string.v3_mail_ready, mailUser))
                    VkPrimaryButton(stringResource(R.string.v3_mail_send_now), {
                        withFreshFile { f ->
                            val r = withContext(Dispatchers.IO) { deps.email(f) }
                            message = when (r) {
                                com.vraj.khetihisab.backup.GmailSender.Result.Sent -> { deps.status.recordSuccess(f.length(), "email"); msgMailSent }
                                com.vraj.khetihisab.backup.GmailSender.Result.AuthFailed -> msgMailAuth
                                com.vraj.khetihisab.backup.GmailSender.Result.NoNetwork -> msgMailNet
                                is com.vraj.khetihisab.backup.GmailSender.Result.Failed -> msgMailFail
                            }
                        }
                    }, Modifier.fillMaxWidth().testTag("backup_email"), enabled = !busy)
                    StSwitchRow(
                        title = stringResource(R.string.v3_mail_nightly),
                        meta = stringResource(R.string.v3_mail_nightly_sub),
                        checked = mailNightly,
                        onCheckedChange = { mailNightly = it; deps.mail.nightly = it },
                        onLabel = stringResource(R.string.st_on),
                        offLabel = stringResource(R.string.st_off),
                    )
                    VkTextButton(stringResource(R.string.v3_mail_change), { editMail = true }, Modifier.fillMaxWidth())
                } else {
                    StNote(stringResource(R.string.v3_mail_how))
                    StTextField(
                        mailUser,
                        { mailUser = it.trim(); mailError = null },
                        stringResource(R.string.v3_mail_gmail),
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = "name@gmail.com",
                        isError = mailError == 1,
                    )
                    StTextField(
                        mailPass,
                        { mailPass = it; mailError = null },
                        stringResource(R.string.v3_mail_app_password),
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                        password = true,
                        placeholder = "abcd efgh ijkl mnop",
                        isError = mailError == 2,
                    )
                    mailError?.let { StError(stringResource(if (it == 1) R.string.v3_mail_err_gmail else R.string.v3_mail_err_pass)) }
                    VkPrimaryButton(stringResource(R.string.v3_mail_save), {
                        val keepOld = mailPass.isBlank() && deps.mail.hasPassword
                        when {
                            !com.vraj.khetihisab.backup.MailSettings.isGmail(mailUser) -> mailError = 1
                            !keepOld && !com.vraj.khetihisab.backup.MailSettings.isAppPassword(mailPass) -> mailError = 2
                            else -> {
                                deps.mail.save(mailUser, if (keepOld) null else mailPass)
                                mailPass = ""
                                mailReady = true
                                editMail = false
                            }
                        }
                    }, Modifier.fillMaxWidth().testTag("mail_save"))
                    // Old way still works without setup: opens Gmail with the file attached.
                    VkTextButton(stringResource(R.string.v3_mail_draft), {
                        withFreshFile { f ->
                            if (BackupShare.email(ctx, f, mailUser.ifBlank { email })) deps.status.recordSuccess(f.length(), "email") else message = msgNoApp
                        }
                    }, Modifier.fillMaxWidth(), enabled = !busy)
                }
            }

            // ---- restore ----
            VkSectionTitle(stringResource(R.string.v2_bk_restore_title))
            VkCard(Modifier.fillMaxWidth(), tone = VkTone.Primary) {
                StNote(stringResource(R.string.v2_bk_restore_how))
                VkOutlinedButton(stringResource(R.string.v2_bk_restore_pick), { pickRestore.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) }, Modifier.fillMaxWidth().testTag("backup_restore"))
            }
            StNote(stringResource(R.string.v2_bk_privacy))
        }
    }

    if (restoreUri != null && (restoreInfo != null || restoreBad)) {
        val info = restoreInfo
        if (info == null) {
            StDialog(
                title = stringResource(R.string.v2_bk_restore_bad_title),
                confirmLabel = stringResource(R.string.st_close),
                onConfirm = { restoreUri = null; restoreBad = false },
                dismissLabel = stringResource(R.string.st_cancel),
                onDismiss = { restoreUri = null; restoreBad = false },
            ) { Text(stringResource(R.string.v2_bk_restore_bad_body)) }
        } else {
            StDialog(
                title = stringResource(R.string.v2_bk_restore_q),
                confirmLabel = stringResource(R.string.v2_bk_restore_yes),
                onConfirm = {
                    val u = restoreUri ?: return@StDialog
                    restoreInfo = null
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { doRestore(ctx, u) }
                        busy = false
                        restoreUri = null
                        if (r is JsonBackup.RestoreResult.Ok) {
                            message = ctx.getString(R.string.v2_bk_restored)
                            kotlinx.coroutines.delay(900)
                            RestoreInbox.restartApp(ctx)
                        } else {
                            message = ctx.getString(
                                if (r is JsonBackup.RestoreResult.TooNew) R.string.v2_bk_restore_too_new else R.string.v2_bk_restore_bad_body,
                            )
                        }
                    }
                },
                dismissLabel = stringResource(R.string.st_cancel),
                onDismiss = { restoreUri = null; restoreInfo = null },
            ) {
                Text(stringResource(R.string.v2_bk_restore_info, lastText(ctx, info.createdAt), info.entries.toInt(), info.photoCount))
                StError(stringResource(R.string.v2_bk_restore_warn))
            }
        }
    }
}

private fun doRestore(ctx: Context, uri: Uri): JsonBackup.RestoreResult {
    val db = AppDatabase.get(ctx).openHelper.writableDatabase
    return ctx.contentResolver.openInputStream(uri)?.use { JsonBackup.restore(db, AppDatabase.VERSION, ctx.filesDir, it) }
        ?: JsonBackup.RestoreResult.Broken("open")
}

private fun lastText(ctx: Context, millis: Long): String {
    val today = LocalDate.now(SettingsFormat.IST)
    return SettingsFormat.backupWhen(millis, today, ctx.getString(R.string.st_today), ctx.getString(R.string.st_tomorrow))
}

private fun providerLabel(authority: String): String = when {
    authority.contains("google.android.apps.docs") -> "Google Drive"
    authority.contains("downloads") || authority.contains("externalstorage") -> "ફોનમાં"
    else -> authority
}
