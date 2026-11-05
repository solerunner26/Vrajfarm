package com.vraj.khetihisab.ui.entry

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.ui.home.ScreensFormat
import java.time.LocalDate

enum class EntryKind { INCOME, EXPENSE }

enum class ApmcField { MANN, KG, RATE }

/**
 * The 3-tap entry form (doc 06). Pure state, no Android: the ViewModel and the Compose UI test both
 * drive it through [EntryReducer.reduce].
 */
data class EntryForm(
    val kind: EntryKind = EntryKind.EXPENSE,
    val section: AccountSection? = null,
    val farmId: Long? = null,
    val familyId: Long? = null,
    val categoryId: Long? = null,
    /** The chosen category is "પાક વેચાણ" → APMC mode for FARM + INCOME. */
    val categoryIsCropSale: Boolean = false,
    val amountText: String = "",
    /** Manual "મણ ગણક" switch. */
    val apmcSwitch: Boolean = false,
    val activeField: ApmcField = ApmcField.MANN,
    val mannText: String = "",
    val kgText: String = "",
    val rateText: String = "",
    val pending: Boolean = false,
    val counterparty: String = "",
    val voiceNote: String? = null,
    val billPath: String? = null,
    val date: LocalDate = ScreensFormat.today(),
    /** One-shot: "20 કિલો = 1 મણ ઉમેર્યું" snackbar. */
    val kgNormalized: Boolean = false,
    /** Editing an existing row (સુધારો): saving updates it instead of adding a new one. */
    val editId: Long? = null,
) {
    val apmcMode: Boolean
        get() = apmcSwitch || (section == AccountSection.FARM && kind == EntryKind.INCOME && categoryIsCropSale)

    val mann: Double get() = AmountInput.toDouble(mannText)
    val extraKg: Double get() = AmountInput.toDouble(kgText)
    val ratePerMannPaise: Long get() = ScreensFormat.rupeesTextToPaise(rateText)
    val totalMann: Double get() = ApmcCalculator.totalMann(mann, extraKg)

    val amountPaise: Long
        get() = if (apmcMode) ApmcCalculator.totalAmountPaise(mann, extraKg, ratePerMannPaise)
        else ScreensFormat.rupeesTextToPaise(amountText)

    /** FAMILY entries are always ઉપાડ (ADVANCE). */
    val txnType: TxnType
        get() = when {
            section == AccountSection.FAMILY -> TxnType.ADVANCE
            kind == EntryKind.INCOME -> TxnType.INCOME
            else -> TxnType.EXPENSE
        }

    val payStatus: PayStatus
        get() = when {
            !pending -> PayStatus.PAID
            txnType == TxnType.INCOME -> PayStatus.PENDING_RECEIVABLE
            else -> PayStatus.PENDING_PAYABLE
        }

    /** Tap 2 is complete: a farm, a family, or a category for GENERAL / PERSONAL. */
    val targetChosen: Boolean
        get() = when (section) {
            AccountSection.FARM -> farmId != null
            AccountSection.FAMILY -> familyId != null
            AccountSection.ANIMAL, AccountSection.GENERAL, AccountSection.PERSONAL -> categoryId != null
            null -> false
        }

    /** ઉધાર needs a name, so "who owes whom" is never lost (owner feedback: keep udhaar simple). */
    val needsName: Boolean get() = pending && counterparty.isBlank()

    val canSave: Boolean get() = section != null && targetChosen && amountPaise > 0 && !needsName
}

sealed interface EntryEvent {
    data class SetKind(val kind: EntryKind) : EntryEvent
    data class SetSection(val section: AccountSection) : EntryEvent
    data class PickFarm(val farmId: Long) : EntryEvent
    data class PickFamily(val familyId: Long) : EntryEvent
    /** Typed નોંધ (kept as typed; trimmed only when saved). */
    data class SetNoteText(val text: String) : EntryEvent
    /** Fill the whole form from speech ("ખાતર બે હજાર ખેતર ત્રણ ઉધાર રમેશભાઈ"). */
    data class ApplyVoice(val parsed: VoiceEntry) : EntryEvent
    data class PickCategory(val categoryId: Long, val isCropSale: Boolean) : EntryEvent
    data class Key(val ch: Char) : EntryEvent
    data object Backspace : EntryEvent
    data object ClearActive : EntryEvent
    data class SetApmcSwitch(val on: Boolean) : EntryEvent
    data class SetField(val field: ApmcField) : EntryEvent
    data class SetPending(val pending: Boolean) : EntryEvent
    data class SetCounterparty(val text: String) : EntryEvent
    data class SetVoiceNote(val text: String?) : EntryEvent
    data class SetBill(val path: String?) : EntryEvent
    data class SetDate(val date: LocalDate) : EntryEvent
    data class UseAmount(val rupees: Long) : EntryEvent
    data object NoticeShown : EntryEvent
}

object EntryReducer {
    fun reduce(s: EntryForm, e: EntryEvent): EntryForm = when (e) {
        is EntryEvent.SetKind ->
            if (e.kind == s.kind) s else s.copy(kind = e.kind, categoryId = null, categoryIsCropSale = false)
        // Switching section resets step 2 (doc 06 edge cases).
        is EntryEvent.SetSection ->
            if (e.section == s.section) s
            else s.copy(section = e.section, farmId = null, familyId = null, categoryId = null, categoryIsCropSale = false)
        is EntryEvent.PickFarm -> s.copy(farmId = e.farmId)
        is EntryEvent.PickFamily -> s.copy(familyId = e.familyId)
        is EntryEvent.PickCategory ->
            if (s.categoryId == e.categoryId) s.copy(categoryId = null, categoryIsCropSale = false)
            else s.copy(categoryId = e.categoryId, categoryIsCropSale = e.isCropSale)
        is EntryEvent.Key -> typeKey(s, e.ch)
        EntryEvent.Backspace -> editActive(s) { AmountInput.backspace(it) }
        EntryEvent.ClearActive -> editActive(s) { "" }
        is EntryEvent.SetApmcSwitch -> s.copy(apmcSwitch = e.on, activeField = ApmcField.MANN)
        is EntryEvent.SetField -> s.copy(activeField = e.field)
        is EntryEvent.SetPending -> s.copy(pending = e.pending, counterparty = if (e.pending) s.counterparty else "")
        is EntryEvent.SetCounterparty -> s.copy(counterparty = e.text.take(60))
        is EntryEvent.SetVoiceNote -> s.copy(voiceNote = e.text?.trim()?.ifEmpty { null })
        is EntryEvent.SetNoteText -> s.copy(voiceNote = e.text.take(200).ifEmpty { null })
        is EntryEvent.ApplyVoice -> VoiceEntryParser.apply(s, e.parsed)
        is EntryEvent.SetBill -> s.copy(billPath = e.path)
        is EntryEvent.SetDate -> s.copy(date = e.date)
        is EntryEvent.UseAmount -> if (s.apmcMode) s else s.copy(amountText = e.rupees.coerceIn(0, 9_999_999).toString())
        EntryEvent.NoticeShown -> s.copy(kgNormalized = false)
    }

    private fun editActive(s: EntryForm, f: (String) -> String): EntryForm = if (!s.apmcMode) {
        s.copy(amountText = f(s.amountText))
    } else {
        when (s.activeField) {
            ApmcField.MANN -> s.copy(mannText = f(s.mannText))
            ApmcField.KG -> s.copy(kgText = f(s.kgText))
            ApmcField.RATE -> s.copy(rateText = f(s.rateText))
        }
    }

    private fun typeKey(s: EntryForm, ch: Char): EntryForm {
        if (!s.apmcMode) return s.copy(amountText = AmountInput.append(s.amountText, ch, AmountRules.AMOUNT))
        return when (s.activeField) {
            ApmcField.MANN -> s.copy(mannText = AmountInput.append(s.mannText, ch, AmountRules.MANN))
            ApmcField.RATE -> s.copy(rateText = AmountInput.append(s.rateText, ch, AmountRules.RATE))
            ApmcField.KG -> {
                val kgText = AmountInput.append(s.kgText, ch, AmountRules.KG)
                val kg = AmountInput.toDouble(kgText)
                if (kg >= ApmcCalculator.KG_PER_MANN) {
                    val (m, k) = ApmcCalculator.normalize(s.mann, kg)
                    s.copy(mannText = AmountInput.format(m), kgText = AmountInput.format(k), kgNormalized = true)
                } else {
                    s.copy(kgText = kgText)
                }
            }
        }
    }
}
