package com.vraj.khetihisab.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoanSearchQueryParserTest {
    private val nov23 = LocalDate.of(2025, 11, 23).toEpochDay()

    @Test fun dashDate() = assertEquals(nov23, LoanSearchQuery.parse("23-11-2025").epochDay)

    @Test fun lenientSeparatorsAndSingleDigits() {
        assertEquals(nov23, LoanSearchQuery.parse("23/11/2025").epochDay)
        assertEquals(nov23, LoanSearchQuery.parse("23.11.2025").epochDay)
        assertEquals(LocalDate.of(2025, 3, 5).toEpochDay(), LoanSearchQuery.parse("5-3-2025").epochDay)
    }

    @Test fun trimmed() {
        val q = LoanSearchQuery.parse("  23-11-2025 ")
        assertEquals("23-11-2025", q.text)
        assertEquals(nov23, q.epochDay)
    }

    @Test fun nameHasNoDate() {
        val q = LoanSearchQuery.parse(" ટ્રેક્ટર ")
        assertEquals("ટ્રેક્ટર", q.text)
        assertNull(q.epochDay)
    }

    @Test fun invalidDateIsNull() {
        assertNull(LoanSearchQuery.parse("31-02-2025").epochDay)
        assertNull(LoanSearchQuery.parse("23-11-25").epochDay)
        assertNull(LoanSearchQuery.parse("2025-11-23").epochDay)
    }

    @Test fun blank() = assertTrue(LoanSearchQuery.parse("   ").isBlank)
}
