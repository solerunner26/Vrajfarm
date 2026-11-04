package com.vraj.khetihisab.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.vraj.khetihisab.ui.components.VkFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class VkThemesTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (max(la, lb) / min(la, lb)).toDouble()
    }

    @Test
    fun tokens_matchDoc01Table() {
        val g = VkThemes.palette(VkThemeId.GREEN)
        assertEquals(Color(0xFF2E7D5B), g.g1)
        assertEquals(Color(0xFF1F5C45), g.p)
        assertEquals(Color(0xFF7A520A), g.t)
        val k = VkThemes.palette(VkThemeId.KESAR)
        assertEquals(Color(0xFF9A6408), k.g1) // darkened per doc 17 #13
        assertEquals(Color(0xFF8A3B12), k.t)
        val n = VkThemes.palette(VkThemeId.NIGHT)
        assertTrue(n.isDark)
        assertEquals(Color(0xFF0F1512), n.bg)
        assertEquals(Color(0xFFFFB4AB), n.err)
        assertEquals(Color(0xFFE8F3EC), n.onG)
    }

    @Test
    fun contrast_passesAA_inAllFiveThemes() {
        VkThemeId.entries.forEach { id ->
            val p = VkThemes.palette(id)
            val cs = VkThemes.colorScheme(p)
            assertTrue("$id body ${contrast(cs.onSurface, cs.surface)}", contrast(cs.onSurface, cs.surface) >= 4.5)
            assertTrue("$id variant", contrast(cs.onSurfaceVariant, cs.surfaceContainerLowest) >= 4.5)
            assertTrue("$id header on g1 ${contrast(p.onG, p.g1)}", contrast(p.onG, p.g1) >= 4.5)
            assertTrue("$id panchang ${contrast(cs.tertiary, cs.tertiaryContainer)}", contrast(cs.tertiary, cs.tertiaryContainer) >= 4.5)
            assertTrue("$id onTc", contrast(cs.onTertiaryContainer, cs.tertiaryContainer) >= 4.5)
            assertTrue("$id primary text", contrast(cs.primary, cs.surfaceContainerLowest) >= 4.5)
            assertTrue("$id onPc", contrast(cs.onPrimaryContainer, cs.primaryContainer) >= 4.5)
            assertTrue("$id error", contrast(cs.error, cs.surfaceContainerLowest) >= 4.5)
        }
    }

    @Test
    fun night_hasNoWhiteSurfaces() {
        val cs = VkThemes.colorScheme(VkThemes.palette(VkThemeId.NIGHT))
        listOf(cs.surface, cs.surfaceContainerLowest, cs.surfaceContainerLow, cs.surfaceContainer, cs.surfaceContainerHigh)
            .forEach { assertFalse(it == Color.White) }
    }

    @Test
    fun themeId_storageRoundTrip() {
        VkThemeId.entries.forEach { assertEquals(it, VkThemeId.fromKey(it.storageKey)) }
        assertEquals(VkThemeId.MATI, VkThemeId.fromKey(null))
        assertEquals(VkThemeId.MATI, VkThemeId.fromKey("unknown"))
    }

    @Test
    fun iconSet_has25Icons_andPalettesPerTheme() {
        assertEquals(25, VkIcon.entries.size)
        assertEquals(5, VkThemeId.entries.map { VkIcons.of(it) }.toSet().size)
    }

    @Test
    fun rupeeFormatting_indianGrouping() {
        assertEquals("₹0", VkFormat.rupees(0))
        assertEquals("₹999", VkFormat.rupees(99_900))
        assertEquals("₹1,200", VkFormat.rupees(120_000))
        assertEquals("₹12,34,567.50", VkFormat.rupees(123_456_750))
        assertEquals("₹1,00,000", VkFormat.rupees(-10_000_000))
    }
}
