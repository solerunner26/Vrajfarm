package com.vraj.khetihisab.panchang

import java.time.LocalDate
import java.time.ZonedDateTime

enum class Paksha(val gu: String) { SUD(PanchangNames.PAKSHA_SUD), VAD(PanchangNames.PAKSHA_VAD) }

/** Output of [GujaratiPanchangHelper.day] (doc 02 §5.1). All times are in Asia/Kolkata. */
data class PanchangDay(
    val date: LocalDate,
    val weekdayGu: String,
    val vikramSamvat: Int,
    /** 0 = ચૈત્ર … 11 = ફાગણ. */
    val monthIndex: Int,
    val isAdhik: Boolean,
    /** e.g. "આસો" or "અધિક શ્રાવણ". */
    val monthGu: String,
    val paksha: Paksha,
    /** 1..15 within the paksha. */
    val tithiInPaksha: Int,
    /** 1..30 (1–15 sud, 16–30 vad; 30 = અમાસ). */
    val tithiIndex: Int,
    /** e.g. "આસો વદ નોમ". */
    val tithiGu: String,
    val tithiStartsAt: ZonedDateTime,
    val tithiEndsAt: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val sunset: ZonedDateTime,
    /** 0.0 (amas) .. 1.0 (poonam), at sunrise. */
    val moonIllumination: Float,
    val waxing: Boolean,
    /** Tithi starts or ends within ±10 min of sunrise: verify against a printed panchang. */
    val nearBoundary: Boolean,
) {
    /** "વિ.સં. 2082" header chip. */
    val vsLabel: String get() = "${PanchangNames.VS_PREFIX} $vikramSamvat"
    val tithiNameGu: String get() = PanchangNames.tithiName(paksha, tithiInPaksha)
    val isPoonam: Boolean get() = tithiIndex == 15
    val isAmas: Boolean get() = tithiIndex == 30
}

// ---- Alert models (doc 10). PanchangAlertEngine (Wave 2) produces these. ----

enum class PanchangAlertType { FESTIVAL, AGIYARAS, AMAS, POONAM, BIJ }

data class SpecialDay(
    val date: LocalDate,
    val day: PanchangDay,
    val types: Set<PanchangAlertType>,
    val festivals: List<String>,
    val ekadashiName: String?,
) {
    val titleGu: String
        get() = festivals.firstOrNull()
            ?: ekadashiName?.let { "$it અગિયારસ" }
            ?: when {
                PanchangAlertType.POONAM in types -> "${day.monthGu} પૂનમ"
                PanchangAlertType.AMAS in types -> "${day.monthGu} અમાસ"
                else -> "બીજ · ચંદ્ર દર્શન"
            }
}

data class PanchangAlert(
    val special: SpecialDay,
    val doLines: List<String>,
    val avoidLines: List<String>,
    val showGuidance: Boolean,
)
