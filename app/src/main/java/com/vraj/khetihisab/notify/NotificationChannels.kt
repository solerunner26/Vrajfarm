package com.vraj.khetihisab.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.vraj.khetihisab.R

/** doc 13 notification framework. Call [createAll] from `Application.onCreate` (idempotent). */
object NotificationChannels {
    const val REMINDERS = "reminders"
    const val ASSETS = "assets"
    const val SHOPPING = "shopping"
    const val PANCHANG = "panchang"
    const val BACKUP = "backup"

    /** Channel id, Gujarati name resource, importance. */
    data class Spec(val id: String, val nameRes: Int, val importance: Int)

    val specs: List<Spec> = listOf(
        Spec(REMINDERS, R.string.ch_reminders, NotificationManager.IMPORTANCE_HIGH),
        Spec(ASSETS, R.string.ch_assets, NotificationManager.IMPORTANCE_DEFAULT),
        Spec(SHOPPING, R.string.ch_shopping, NotificationManager.IMPORTANCE_LOW),
        Spec(PANCHANG, R.string.ch_panchang, NotificationManager.IMPORTANCE_DEFAULT),
        Spec(BACKUP, R.string.ch_backup, NotificationManager.IMPORTANCE_LOW),
    )

    fun createAll(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannels(specs.map { NotificationChannel(it.id, context.getString(it.nameRes), it.importance) })
    }
}
