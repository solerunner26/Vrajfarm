package com.vraj.khetihisab.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ReminderSource
import com.vraj.khetihisab.notify.Deadline
import com.vraj.khetihisab.notify.ReminderTiming
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class ReminderTab { ACTIVE, ARCHIVE }

/** Card tag (canvas 09): HIGH = જરૂરી, NORMAL = સામાન્ય, PANCHANG = પંચાંગ (gold). */
enum class ReminderTag { HIGH, NORMAL, PANCHANG }

/** Small origin chip for source-linked reminders (doc 13). PANCHANG is shown by its tag. */
enum class ReminderOrigin { LOAN }

data class ReminderCard(
    val id: Long,
    val title: String,
    val note: String?,
    val imagePath: String?,
    val tag: ReminderTag,
    val origin: ReminderOrigin?,
    val deadline: Deadline,
    val doneAt: Long?,
    val archived: Boolean,
)

/** Add/edit sheet state. [base] is the row being edited (null = new). */
data class ReminderDraft(
    val base: ReminderEntity? = null,
    val title: String = "",
    val note: String = "",
    val priority: Priority = Priority.NORMAL,
    val date: LocalDate,
    val time: LocalTime,
    val photoPath: String? = null,
    val titleError: Boolean = false,
) {
    val isNew: Boolean get() = base == null
}

data class RemindersUiState(
    val tab: ReminderTab = ReminderTab.ACTIVE,
    val active: List<ReminderCard> = emptyList(),
    val archived: List<ReminderCard> = emptyList(),
    val editor: ReminderDraft? = null,
    val exactAllowed: Boolean = true,
    val loaded: Boolean = false,
) {
    val visible: List<ReminderCard> get() = if (tab == ReminderTab.ACTIVE) active else archived
}

sealed interface RemindersEvent {
    /** Show "આર્કાઇવમાં મૂક્યું · {title}" with "પાછું લાવો". */
    data class Done(val id: Long, val title: String) : RemindersEvent
    data object Saved : RemindersEvent
    data object Deleted : RemindersEvent
    data object Reopened : RemindersEvent

    /** First reminder ever saved: ask for POST_NOTIFICATIONS (API 33+) contextually (doc 13). */
    data object AskNotificationPermission : RemindersEvent
}

/** Pure draft ⇄ entity rules, including the due-time maths (IST). */
object ReminderDraftLogic {
    val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

    /** New reminder: tomorrow 09:00 IST, NORMAL. */
    fun newDraft(now: Long): ReminderDraft =
        ReminderDraft(date = ReminderTiming.istDate(now).plusDays(1), time = DEFAULT_TIME)

    fun fromEntity(r: ReminderEntity): ReminderDraft {
        val dt = ReminderTiming.istDateTime(r.dueAt)
        return ReminderDraft(
            base = r,
            title = r.title,
            note = r.note.orEmpty(),
            priority = r.priority,
            date = dt.toLocalDate(),
            time = dt.toLocalTime().withSecond(0).withNano(0),
            photoPath = r.imagePath,
        )
    }

    /** Due instant of the draft: [ReminderDraft.date] + [ReminderDraft.time] in Asia/Kolkata. */
    fun dueAt(d: ReminderDraft): Long = ReminderTiming.istMillis(d.date.atTime(d.time))

    /** Saved without an alarm; the sheet shows "આ સમય વીતી ગયો છે · સૂચના નહીં આવે". */
    fun isPast(d: ReminderDraft, now: Long): Boolean = dueAt(d) <= now

    fun isValid(d: ReminderDraft): Boolean = d.title.isNotBlank()

    fun toEntity(d: ReminderDraft, now: Long): ReminderEntity {
        val title = d.title.trim()
        val note = d.note.trim().ifEmpty { null }
        val due = dueAt(d)
        return d.base?.copy(
            title = title,
            note = note,
            priority = d.priority,
            dueAt = due,
            imagePath = d.photoPath,
            updatedAt = now,
        ) ?: ReminderEntity(
            title = title,
            note = note,
            imagePath = d.photoPath,
            priority = d.priority,
            dueAt = due,
            sourceType = ReminderSource.MANUAL,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun card(r: ReminderEntity, now: Long): ReminderCard = ReminderCard(
        id = r.id,
        title = r.title,
        note = r.note?.takeIf { it.isNotBlank() },
        imagePath = r.imagePath,
        tag = when {
            r.sourceType == ReminderSource.PANCHANG -> ReminderTag.PANCHANG
            r.priority == Priority.HIGH -> ReminderTag.HIGH
            else -> ReminderTag.NORMAL
        },
        origin = if (r.sourceType == ReminderSource.LOAN) ReminderOrigin.LOAN else null,
        deadline = Deadline.of(r.dueAt, now),
        doneAt = r.doneAt,
        archived = r.isDone,
    )
}

/** Reminders screen (doc 13, canvas 09): active/archive, DONE + undo, add/edit sheet. */
class RemindersViewModel(
    private val store: ReminderStore,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Deadline labels ("આજે 5:00 PM" → "મુદત વીતી") refresh this often; ≤ 0 = never (tests). */
    private val tickMillis: Long = 60_000L,
    /** True while POST_NOTIFICATIONS (API 33+) is not granted; checked after the first save. */
    private val notificationPermissionNeeded: () -> Boolean = { false },
) : ViewModel() {

    private val tab = MutableStateFlow(ReminderTab.ACTIVE)
    private val editor = MutableStateFlow<ReminderDraft?>(null)
    private val exactAllowed = MutableStateFlow(store.canScheduleExact())
    private val events = Channel<RemindersEvent>(Channel.BUFFERED)
    val eventFlow: Flow<RemindersEvent> = events.receiveAsFlow()

    /** Photos captured in the open sheet and not yet saved (deleted on dismiss). */
    private val unsavedPhotos = mutableSetOf<String>()

    private val ticker: Flow<Long> = flow {
        emit(clock())
        if (tickMillis > 0) {
            while (true) {
                delay(tickMillis)
                emit(clock())
            }
        }
    }

    private val lists = combine(store.active(), store.archived(), ticker) { a, z, now ->
        a.map { ReminderDraftLogic.card(it, now) } to z.map { ReminderDraftLogic.card(it, now) }
    }

    val state: StateFlow<RemindersUiState> = combine(tab, lists, editor, exactAllowed) { t, (a, z), e, x ->
        RemindersUiState(tab = t, active = a, archived = z, editor = e, exactAllowed = x, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState(exactAllowed = exactAllowed.value))

    fun selectTab(t: ReminderTab) {
        tab.value = t
    }

    /** Re-read after returning from the exact-alarm settings page. */
    fun refreshExactAllowed() {
        exactAllowed.value = store.canScheduleExact()
    }

    // ---------- DONE / undo / reopen ----------
    fun markDone(id: Long, title: String) {
        viewModelScope.launch {
            store.markDone(id)
            events.send(RemindersEvent.Done(id, title))
        }
    }

    /** Snackbar "પાછું લાવો". */
    fun undoDone(id: Long) {
        viewModelScope.launch { store.reopen(id) }
    }

    /** Archive card "ફરી ચાલુ કરો". */
    fun reopen(id: Long) {
        viewModelScope.launch {
            store.reopen(id)
            events.send(RemindersEvent.Reopened)
        }
    }

    // ---------- editor sheet ----------
    fun openNew() {
        unsavedPhotos.clear()
        editor.value = ReminderDraftLogic.newDraft(clock())
    }

    fun openEdit(id: Long) {
        viewModelScope.launch {
            val r = store.byId(id) ?: return@launch
            if (r.isDeleted || r.isDone) return@launch
            unsavedPhotos.clear()
            editor.value = ReminderDraftLogic.fromEntity(r)
        }
    }

    fun onTitle(v: String) = editor.update { it?.copy(title = v, titleError = false) }
    fun onNote(v: String) = editor.update { it?.copy(note = v) }
    fun onPriority(p: Priority) = editor.update { it?.copy(priority = p) }
    fun onDate(d: LocalDate) = editor.update { it?.copy(date = d) }
    fun onTime(t: LocalTime) = editor.update { it?.copy(time = t.withSecond(0).withNano(0)) }

    /** Voice result: appended to the title (doc 13 "title with a voice mic"). */
    fun onVoiceText(text: String) = editor.update { d ->
        d?.copy(title = listOf(d.title.trim(), text.trim()).filter { it.isNotEmpty() }.joinToString(" "), titleError = false)
    }

    fun onPhoto(path: String) {
        val d = editor.value ?: run {
            store.deletePhoto(path)
            return
        }
        d.photoPath?.let { if (it in unsavedPhotos) dropUnsaved(it) }
        unsavedPhotos += path
        editor.value = d.copy(photoPath = path)
    }

    fun removePhoto() {
        val d = editor.value ?: return
        d.photoPath?.let { if (it in unsavedPhotos) dropUnsaved(it) }
        editor.value = d.copy(photoPath = null)
    }

    fun dismissEditor() {
        unsavedPhotos.toList().forEach(::dropUnsaved)
        editor.value = null
    }

    fun isPast(d: ReminderDraft): Boolean = ReminderDraftLogic.isPast(d, clock())

    fun save() {
        val d = editor.value ?: return
        if (!ReminderDraftLogic.isValid(d)) {
            editor.value = d.copy(titleError = true)
            return
        }
        val entity = ReminderDraftLogic.toEntity(d, clock())
        val replaced = d.base?.imagePath?.takeIf { it != d.photoPath }
        editor.value = null
        unsavedPhotos.clear()
        viewModelScope.launch {
            store.save(entity)
            replaced?.let(store::deletePhoto)
            if (d.isNew && notificationPermissionNeeded()) events.send(RemindersEvent.AskNotificationPermission)
            events.send(RemindersEvent.Saved)
        }
    }

    fun delete() {
        val d = editor.value ?: return
        val base = d.base ?: return dismissEditor()
        unsavedPhotos.toList().forEach(::dropUnsaved)
        editor.value = null
        viewModelScope.launch {
            store.delete(base.id)
            events.send(RemindersEvent.Deleted)
        }
    }

    private fun dropUnsaved(path: String) {
        unsavedPhotos -= path
        store.deletePhoto(path)
    }

    companion object {
        fun factory(store: ReminderStore, notificationPermissionNeeded: () -> Boolean): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RemindersViewModel(store, notificationPermissionNeeded = notificationPermissionNeeded) as T
            }
    }
}
