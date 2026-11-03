package com.vraj.khetihisab.data.seed

import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType

/**
 * Default "માન્યતા પ્રમાણે" (as per belief) guidance from doc 10 §E.
 * Lines are newline-separated so the engine can split / de-duplicate them; the user can edit them,
 * and "મૂળ સલાહ પાછી લાવો" restores these values via [defaultPref].
 */
object PanchangDefaults {
    /** Footer shown under every guidance block. */
    const val BELIEF_NOTE = "માન્યતા પ્રમાણે · સેટિંગ્સમાં બદલી શકો"

    /** Festival-specific override (doc 10 §E): અખાત્રીજ do-lines. */
    const val AKHATRIJ_NAME = "અખાત્રીજ"
    val AKHATRIJ_DO_LINES = listOf("ખેતીના નવા વર્ષની શરૂઆત", "હળ પૂજન", "નવું કામ શુભ")

    private val doLines: Map<PanchangAlertType, List<String>> = mapOf(
        PanchangAlertType.AMAS to listOf("પિતૃ તર્પણ અને દાન", "ખેતર અને સાધનોની સફાઈ-દેખરેખ"),
        PanchangAlertType.POONAM to listOf("વાવણી કે નવું કામ શરૂ કરવું", "ખરીદી", "પૂજા-પાઠ"),
        PanchangAlertType.AGIYARAS to listOf("ઉપવાસ", "ભજન-કીર્તન", "દાન"),
        PanchangAlertType.BIJ to listOf("ચંદ્ર દર્શન", "નવા કામની શુભ શરૂઆત"),
        PanchangAlertType.FESTIVAL to listOf("પરિવાર સાથે ઉજવણી", "પૂજા"),
    )

    private val avoidLines: Map<PanchangAlertType, List<String>> = mapOf(
        PanchangAlertType.AMAS to listOf(
            "નવી વાવણી શરૂ કરવી",
            "નવું સાધન કે જમીન ખરીદવી",
            "નવું કામ શરૂ કરવું",
        ),
        PanchangAlertType.POONAM to emptyList(),
        PanchangAlertType.AGIYARAS to listOf(
            "કેટલાક લોકો કાપણી / લણણી ટાળે છે",
            "અનાજ (ચોખા) ન ખાવું",
        ),
        PanchangAlertType.BIJ to emptyList(),
        PanchangAlertType.FESTIVAL to emptyList(),
    )

    fun doText(type: PanchangAlertType): String = doLines.getValue(type).joinToString("\n")

    fun avoidText(type: PanchangAlertType): String = avoidLines.getValue(type).joinToString("\n")

    /** Default row: enabled, evening-before notification on, day dialog on. */
    fun defaultPref(type: PanchangAlertType, now: Long): PanchangAlertPrefEntity = PanchangAlertPrefEntity(
        type = type,
        enabled = true,
        notifyEveningBefore = true,
        showDialogOnDay = true,
        doText = doText(type),
        avoidText = avoidText(type),
        updatedAt = now,
    )

    fun all(now: Long): List<PanchangAlertPrefEntity> = PanchangAlertType.entries.map { defaultPref(it, now) }

    /** Split stored guidance into display lines (trimmed, blank and duplicate lines dropped). */
    fun lines(text: String): List<String> =
        text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
