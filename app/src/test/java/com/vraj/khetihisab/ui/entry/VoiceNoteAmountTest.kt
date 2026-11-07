package com.vraj.khetihisab.ui.entry

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test

/** 2026-11-07 audit: the "₹… વાપરો?" suggestion must not merge the farm number into the amount. */
class VoiceNoteAmountTest {
    private val farms = (1L..8L).map { it to "ખેતર $it" }
    private val cats = listOf(VoiceCategory(11, "ખાતર", AccountSection.FARM, TxnType.EXPENSE))

    @Test fun farmNumberIsNotPartOfTheAmount() {
        val v = VoiceEntryParser.parse("ખાતર બે હજાર ખેતર ત્રણ ઉધાર એગ્રો સેન્ટર", farms, emptyList(), cats)
        assertEquals(2_000L, v.rupees)
        assertEquals(3L, v.farmId)
    }

    @Test fun plainAmountStillParses() {
        assertEquals(1_500L, VoiceEntryParser.parse("દવા 1500", farms, emptyList(), cats).rupees)
    }
}
