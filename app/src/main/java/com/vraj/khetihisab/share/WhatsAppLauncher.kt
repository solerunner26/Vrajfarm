package com.vraj.khetihisab.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.vraj.khetihisab.R
import com.vraj.khetihisab.VrajApp
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.data.repo.AssetRepository
import java.time.LocalDate

/**
 * The one place that opens WhatsApp (doc 12). It only **opens** a chat with the message pre-filled;
 * it never sends. The user always taps Send inside WhatsApp.
 *
 * Call it from the UI layer with an Activity context (ViewModels emit an event; the screen calls
 * [open]). Nothing else in the app may build a `wa.me` URL.
 *
 * ```
 * when (WhatsAppLauncher.open(context, phone, text)) { … }
 * ```
 */
object WhatsAppLauncher {

    sealed interface Result {
        /** WhatsApp (or WhatsApp Business) opened on the chat. */
        data class Opened(val pkg: String) : Result

        /** No usable WhatsApp: the share sheet opened with the same text. */
        data object SharedFallback : Result

        /** The number was missing/invalid: a toast "WhatsApp નંબર નથી" + the share sheet. */
        data object NoNumber : Result

        /** Nothing on the device can take the text. */
        data object NoApp : Result

        val opened: Boolean get() = this is Opened || this == SharedFallback || this == NoNumber
    }

    /**
     * Opens WhatsApp on [phone] (any format, see [PhoneNormalizer]) with [message] pre-filled.
     * Falls back to com.whatsapp.w4b, then to the Android share sheet.
     */
    fun open(context: Context, phone: String?, message: String): Result {
        val pm = context.packageManager
        val plan = WhatsAppIntentBuilder.plan(phone, message) { pkg -> isUsable(pm, pkg) }
        return when (plan) {
            is WhatsAppIntentBuilder.Plan.OpenChat -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plan.url)).apply {
                    setPackage(plan.pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                suppressLock(context)
                try {
                    context.startActivity(intent)
                    Result.Opened(plan.pkg)
                } catch (_: ActivityNotFoundException) {
                    share(context, message, noNumber = false)
                } catch (_: SecurityException) {
                    share(context, message, noNumber = false)
                }
            }
            is WhatsAppIntentBuilder.Plan.ShareSheet -> share(context, message, plan.noNumber)
        }
    }

    /** Opens the chat with no text (doc 12 "Manager message"). */
    fun openChat(context: Context, phone: String?): Result = open(context, phone, "")

    /**
     * Loan reminder: builds the template, opens WhatsApp, and when something opened records
     * `lastWhatsAppReminderAt` via [AssetRepository.markReminded]. This records that the reminder was
     * **opened**, not delivered (WhatsApp gives no callback) — the UI says "છેલ્લે યાદ અપાવ્યું".
     */
    suspend fun loanReminder(
        context: Context,
        loan: LoanWithAsset,
        ownerName: String,
        village: String,
        assets: AssetRepository,
        today: LocalDate = LocalDate.now(),
        template: String = WhatsAppTemplateStore(context).loanTemplate(),
    ): Result {
        val text = loanReminderText(loan, ownerName, village, today, template)
        val r = open(context, loan.loan.borrowerPhone, text)
        if (r.opened) assets.markReminded(loan.loan.id)
        return r
    }

    fun loanReminderText(
        loan: LoanWithAsset,
        ownerName: String,
        village: String,
        today: LocalDate = LocalDate.now(),
        template: String = WhatsAppTemplates.DEFAULT_LOAN_REMINDER,
    ): String = WhatsAppTemplates.loanReminder(
        borrower = loan.loan.borrowerName,
        asset = loan.asset.name,
        handedOver = loan.loan.handedOverDate,
        expectedReturn = loan.loan.expectedReturnDate,
        ownerName = ownerName,
        village = village,
        today = today,
        template = template,
    )

    private fun share(context: Context, text: String, noNumber: Boolean): Result {
        if (noNumber) {
            Toast.makeText(context, context.getString(R.string.as_wa_no_number), Toast.LENGTH_SHORT).show()
        }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, context.getString(R.string.as_wa_share))
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        suppressLock(context)
        return try {
            context.startActivity(chooser)
            if (noNumber) Result.NoNumber else Result.SharedFallback
        } catch (_: ActivityNotFoundException) {
            Result.NoApp
        }
    }

    /** Installed and enabled (a disabled WhatsApp would open wa.me in a browser — doc 12 edge case). */
    private fun isUsable(pm: PackageManager, pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Returning from WhatsApp must not re-prompt the PIN (doc 04 / doc 12 §4). */
    private fun suppressLock(context: Context) {
        runCatching { (context.applicationContext as? VrajApp)?.container?.lockManager?.suppressNextLock = true }
    }
}

/** Alias matching the doc 12 name. */
typealias LaunchResult = WhatsAppLauncher.Result
