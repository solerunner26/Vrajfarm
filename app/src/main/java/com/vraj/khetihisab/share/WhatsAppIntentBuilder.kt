package com.vraj.khetihisab.share

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Pure (JVM-testable) half of [WhatsAppLauncher]: decides what to open. No Android types here.
 *
 * Order (doc 12): normalise the number → com.whatsapp → com.whatsapp.w4b → Android share sheet.
 */
object WhatsAppIntentBuilder {

    const val PKG_WHATSAPP = "com.whatsapp"
    const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    /** Preference order. Both must be listed in the manifest `<queries>` (API 30+ package visibility). */
    val PACKAGES: List<String> = listOf(PKG_WHATSAPP, PKG_WHATSAPP_BUSINESS)

    /** What the launcher should do. */
    sealed interface Plan {
        /** ACTION_VIEW on [url], restricted to [pkg]. */
        data class OpenChat(val url: String, val pkg: String, val number: String) : Plan

        /** ACTION_SEND chooser with the same text. [noNumber] = the phone was missing or invalid. */
        data class ShareSheet(val text: String, val noNumber: Boolean) : Plan
    }

    /**
     * Percent-encodes [text] for a URL query value, UTF-8, spaces as `%20` (never `+`), newlines as
     * `%0A`. Equivalent to `Uri.encode` for everything WhatsApp needs (Gujarati, & ? # newlines).
     */
    fun encode(text: String): String =
        URLEncoder.encode(text, StandardCharsets.UTF_8.name()).replace("+", "%20")

    /** `https://wa.me/<number>?text=<encoded>`; an empty text opens the chat only. */
    fun waMeUrl(number: String, text: String): String =
        if (text.isEmpty()) "https://wa.me/$number" else "https://wa.me/$number?text=" + encode(text)

    /** First package in [PACKAGES] that is installed **and enabled**, or null. */
    fun pickPackage(isInstalledAndEnabled: (String) -> Boolean): String? =
        PACKAGES.firstOrNull { runCatching { isInstalledAndEnabled(it) }.getOrDefault(false) }

    fun plan(phone: String?, text: String, isInstalledAndEnabled: (String) -> Boolean): Plan {
        val number = PhoneNormalizer.toE164NoPlus(phone) ?: return Plan.ShareSheet(text, noNumber = true)
        val pkg = pickPackage(isInstalledAndEnabled) ?: return Plan.ShareSheet(text, noNumber = false)
        return Plan.OpenChat(waMeUrl(number, text), pkg, number)
    }
}
