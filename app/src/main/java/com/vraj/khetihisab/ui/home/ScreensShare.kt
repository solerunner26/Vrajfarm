package com.vraj.khetihisab.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vraj.khetihisab.share.WhatsAppLauncher

/**
 * The only WhatsApp / dial touch-point of the compose-screens package. Every call goes through the
 * shared `com.vraj.khetihisab.share.WhatsAppLauncher` (doc 12); nothing here builds a wa.me URL.
 * The lock is suppressed before leaving the app (doc 04 §2).
 */
object ScreensShare {

    fun whatsApp(context: Context, deps: ScreensDeps, phone: String?, text: String) {
        deps.lockManager.suppressNextLock = true
        WhatsAppLauncher.open(context, phone, text)
    }

    fun dial(context: Context, deps: ScreensDeps, phone: String) {
        deps.lockManager.suppressNextLock = true
        val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' }))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }
}
