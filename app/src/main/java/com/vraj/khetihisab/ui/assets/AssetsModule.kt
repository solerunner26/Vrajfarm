package com.vraj.khetihisab.ui.assets

import android.content.Context
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.AssetRepository
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.data.repo.MasterRepository
import com.vraj.khetihisab.data.repo.ProfileRepository
import com.vraj.khetihisab.data.repo.ReminderRepository
import com.vraj.khetihisab.data.repo.ShoppingRepository

/** Repositories used by shopping / assets / lending screens and the two workers. */
class AssetsDependencies(
    val shopping: ShoppingRepository,
    val assets: AssetRepository,
    val reminders: ReminderRepository,
    val profiles: ProfileRepository,
    val master: MasterRepository,
) {
    constructor(repos: DataRepositories) : this(repos.shopping, repos.assets, repos.reminders, repos.profiles, repos.master)
}

/**
 * Service locator for this module (screens are created inside the lead's NavHost and workers by
 * WorkManager, so neither can take constructor arguments from AppContainer directly).
 *
 * The lead should call `AssetsModule.install { AssetsDependencies(container.repos) }` once in
 * `VrajApp.onCreate`, so the widget [com.vraj.khetihisab.data.repo.DataChangeListener] is shared.
 * If nothing is installed, a fallback over the `AppDatabase.get(context)` singleton is used.
 */
object AssetsModule {
    @Volatile private var provider: (() -> AssetsDependencies)? = null
    @Volatile private var cached: AssetsDependencies? = null

    fun install(provider: () -> AssetsDependencies) {
        this.provider = provider
        cached = null
    }

    fun get(context: Context): AssetsDependencies {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val deps = provider?.invoke() ?: AssetsDependencies(DataRepositories(AppDatabase.get(context.applicationContext)))
            cached = deps
            return deps
        }
    }
}
