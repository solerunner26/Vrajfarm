package com.vraj.khetihisab.notify

import android.content.Context
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.settings.vkPrefsDataStore
import com.vraj.khetihisab.widget.WidgetRefresher

/**
 * Service locator for code that runs without an Activity (receivers, Glance widgets, actions,
 * the Reminders ViewModel factory). The lead calls [install] from `Application.onCreate` with
 * AppContainer's repositories so the whole app shares one set. If it is not installed, a fallback
 * set is built on the shared [AppDatabase] singleton with [WidgetRefresher] as DataChangeListener.
 */
object NotifyGraph {
    @Volatile private var repos: DataRepositories? = null

    fun install(repositories: DataRepositories) {
        repos = repositories
    }

    fun repos(context: Context): DataRepositories = repos ?: synchronized(this) {
        repos ?: DataRepositories(
            AppDatabase.get(context.applicationContext),
            listener = WidgetRefresher.get(context.applicationContext),
        ).also { repos = it }
    }

    fun settings(context: Context): PanchangAlertSettingsStore =
        PanchangAlertSettingsStore(context.applicationContext.vkPrefsDataStore)

    fun alarmPort(context: Context): AlarmPort = AndroidAlarmPort(context.applicationContext)

    fun reminderScheduler(context: Context): ReminderScheduler = ReminderScheduler(alarmPort(context))

    fun panchangScheduler(context: Context): PanchangAlarmScheduler = PanchangAlarmScheduler(alarmPort(context))
}
