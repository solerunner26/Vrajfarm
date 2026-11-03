package com.vraj.khetihisab.panchang

import java.time.DayOfWeek

/** Gujarati name tables (doc 02 §5.4). Pure data, no Android dependencies. */
object PanchangNames {

    /** Amanta lunar months, index 0 = ચૈત્ર … 11 = ફાગણ. */
    val months: List<String> = listOf(
        "ચૈત્ર", "વૈશાખ", "જેઠ", "અષાઢ", "શ્રાવણ", "ભાદરવો",
        "આસો", "કારતક", "માગશર", "પોષ", "મહા", "ફાગણ",
    )

    /** Tithi names within a paksha, index 0 = એકમ … 14 = પૂનમ (sud). */
    val tithis: List<String> = listOf(
        "એકમ", "બીજ", "ત્રીજ", "ચોથ", "પાંચમ", "છઠ", "સાતમ", "આઠમ",
        "નોમ", "દશમ", "અગિયારસ", "બારસ", "તેરસ", "ચૌદશ", "પૂનમ",
    )
    const val AMAS = "અમાસ"
    const val PAKSHA_SUD = "સુદ"
    const val PAKSHA_VAD = "વદ"
    const val ADHIK = "અધિક"
    const val VS_PREFIX = "વિ.સં."

    /** Monday → Sunday. */
    val weekdays: List<String> = listOf(
        "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર", "રવિવાર",
    )

    /** Gregorian month names in Gujarati, index 0 = January. */
    val gregorianMonths: List<String> = listOf(
        "જાન્યુઆરી", "ફેબ્રુઆરી", "માર્ચ", "એપ્રિલ", "મે", "જૂન",
        "જુલાઈ", "ઓગસ્ટ", "સપ્ટેમ્બર", "ઓક્ટોબર", "નવેમ્બર", "ડિસેમ્બર",
    )

    fun weekday(day: DayOfWeek): String = weekdays[day.value - 1]

    fun month(index: Int, adhik: Boolean): String =
        if (adhik) "$ADHIK ${months[index]}" else months[index]

    /** Name of a tithi within its paksha (1..15). Vad 15 is અમાસ. */
    fun tithiName(paksha: Paksha, tithiInPaksha: Int): String {
        require(tithiInPaksha in 1..15) { "tithiInPaksha out of range: $tithiInPaksha" }
        return if (paksha == Paksha.VAD && tithiInPaksha == 15) AMAS else tithis[tithiInPaksha - 1]
    }
}
