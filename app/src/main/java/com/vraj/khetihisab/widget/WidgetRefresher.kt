package com.vraj.khetihisab.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.vraj.khetihisab.data.repo.DataChangeListener
import com.vraj.khetihisab.notify.NotifyGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The `DataChangeListener` hook of `DataRepositories` (doc 13 "WidgetRefresher.refresh()").
 * Every repository write calls [onDataChanged]; bursts are debounced, then the three widgets are
 * redrawn with `updateAll` and the reminder alarms are re-synced (so reminders created elsewhere,
 * e.g. AssetRepository.lend's overdue reminder, get their alarm too).
 */
class WidgetRefresher private constructor(private val context: Context) : DataChangeListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pending: Job? = null

    override fun onDataChanged() {
        synchronized(this) {
            pending?.cancel()
            pending = scope.launch {
                delay(DEBOUNCE_MS)
                refreshNow()
                syncReminderAlarms()
            }
        }
    }

    /** Alias used by doc 13. */
    fun refresh() = onDataChanged()

    suspend fun refreshNow() {
        runCatching { ShoppingListGlanceWidget().updateAll(context) }.onFailure { Log.w(TAG, "shopping", it) }
        runCatching { ReminderGlanceWidget().updateAll(context) }.onFailure { Log.w(TAG, "reminder", it) }
        runCatching { TodayGlanceWidget().updateAll(context) }.onFailure { Log.w(TAG, "today", it) }
    }

    private suspend fun syncReminderAlarms() {
        runCatching {
            NotifyGraph.reminderScheduler(context).rescheduleAll(NotifyGraph.repos(context).reminders.activeNow())
        }.onFailure { Log.w(TAG, "alarms", it) }
    }

    companion object {
        private const val TAG = "WidgetRefresher"
        const val DEBOUNCE_MS = 300L

        @Volatile private var instance: WidgetRefresher? = null

        fun get(context: Context): WidgetRefresher = instance ?: synchronized(this) {
            instance ?: WidgetRefresher(context.applicationContext).also { instance = it }
        }
    }
}
