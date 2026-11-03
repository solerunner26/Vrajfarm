package com.vraj.khetihisab

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.vraj.khetihisab.di.AppContainer

class VrajApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Auto-lock after 30 s in the background (doc 04 §2).
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.lockManager.observer)
    }
}
