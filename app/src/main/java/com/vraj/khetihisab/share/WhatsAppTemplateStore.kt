package com.vraj.khetihisab.share

import android.content.Context
import android.content.SharedPreferences

/**
 * User-edited WhatsApp templates (Settings › "WhatsApp મેસેજ નમૂના", doc 12). Placeholders are fixed;
 * a blank value means "use the default". The Settings screen (compose-screens) reads/writes this.
 */
class WhatsAppTemplateStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    fun loanTemplate(): String =
        prefs.getString(KEY_LOAN, null)?.takeIf { it.isNotBlank() } ?: WhatsAppTemplates.DEFAULT_LOAN_REMINDER

    fun familyTemplate(): String =
        prefs.getString(KEY_FAMILY, null)?.takeIf { it.isNotBlank() } ?: WhatsAppTemplates.DEFAULT_FAMILY_STATEMENT

    fun setLoanTemplate(text: String?) = prefs.edit().putString(KEY_LOAN, text).apply()
    fun setFamilyTemplate(text: String?) = prefs.edit().putString(KEY_FAMILY, text).apply()

    companion object {
        const val PREFS = "vk_wa_templates"
        const val KEY_LOAN = "loan_reminder"
        const val KEY_FAMILY = "family_statement"
    }
}
