package com.vraj.khetihisab.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * doc 14 acceptance: the credit "Design and Developed by Jaldip Vala" appears ONLY on the Help screen.
 * File scan over src/main/java (unit tests run with the app module as working directory).
 */
class CreditOnlyInHelpTest {

    private val srcRoot = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }

    private fun kotlinFiles() = srcRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun creditKeysAreReferencedOnlyFromHelpScreen() {
        val users = kotlinFiles().filter { f -> f.readText().contains("R.string.st_credit_") }.map { it.name }
        assertEquals(listOf("HelpScreen.kt"), users)
    }

    @Test
    fun noKotlinFileHardCodesTheCredit() {
        val offenders = kotlinFiles().filter { f ->
            val t = f.readText()
            t.contains("Jaldip") || t.contains("જલદીપ")
        }.map { it.path }
        assertTrue("credit literal found in $offenders", offenders.isEmpty())
    }

    @Test
    fun helpShowsCreditLast() {
        val help = kotlinFiles().single { it.name == "HelpScreen.kt" }.readText()
        val credit = help.indexOf("R.string.st_credit_name")
        val owner = help.indexOf("R.string.st_owner_en")
        assertTrue(credit > owner)
    }
}
