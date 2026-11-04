package com.vraj.khetihisab.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.share.WhatsAppLauncher
import com.vraj.khetihisab.share.WhatsAppTemplateStore
import com.vraj.khetihisab.share.WhatsAppTemplates

/**
 * The only WhatsApp touch-point of the compose-screens package. Every call goes through the shared
 * `com.vraj.khetihisab.share.WhatsAppLauncher` (doc 12): it only opens the chat with the text
 * pre-filled, never sends. The lock is suppressed before leaving the app (doc 04 §2).
 */
object ScreensShare {

    fun whatsApp(context: Context, deps: ScreensDeps, phone: String?, text: String): WhatsAppLauncher.Result {
        deps.lockManager.suppressNextLock = true
        return WhatsAppLauncher.open(context, phone, text)
    }

    /** Overdue-loan reminder from Home (doc 05 §2.2). Records "last reminded" when something opened. */
    suspend fun loanReminder(
        context: Context,
        deps: ScreensDeps,
        loan: LoanWithAsset,
        ownerName: String,
        ownerAddress: String?,
    ): WhatsAppLauncher.Result {
        deps.lockManager.suppressNextLock = true
        return WhatsAppLauncher.loanReminder(
            context = context,
            loan = loan,
            ownerName = ownerName,
            village = WhatsAppTemplates.villageFromAddress(ownerAddress),
            assets = deps.repos.assets,
            today = ScreensFormat.today(),
            template = WhatsAppTemplateStore(context).loanTemplate(),
        )
    }

    /** Opens the dialer with [phone] (Profiles "ફોન કરો", doc 14). Never places the call itself. */
    fun dial(context: Context, deps: ScreensDeps, phone: String) {
        deps.lockManager.suppressNextLock = true
        val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' }))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }
}
