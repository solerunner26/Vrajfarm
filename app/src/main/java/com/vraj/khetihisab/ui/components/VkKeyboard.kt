package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList

/**
 * Owner request 2026-11-05: typing defaults to Gujarati. Android does not let an app switch the user's
 * keyboard, but every text field sends this locale hint and Gboard (and Samsung Keyboard) then opens in
 * ગુજરાતી automatically — as long as Gujarati is added once in the keyboard's languages (Help explains).
 */
val VkGujaratiLocales: LocaleList = LocaleList(Locale("gu-IN"))

/** Adds the Gujarati keyboard hint to any [KeyboardOptions] (numbers/e-mail keep their own type). */
fun KeyboardOptions.gujarati(): KeyboardOptions = copy(hintLocales = VkGujaratiLocales)

/** Default options for Gujarati text fields. */
val VkGujaratiKeyboard: KeyboardOptions = KeyboardOptions(hintLocales = VkGujaratiLocales)
