package com.szczurek9.reminder

import android.app.Application
import com.szczurek9.reminder.data.ReminderDatabase
import com.szczurek9.reminder.data.ReminderRepository
import com.szczurek9.reminder.data.RoomReminderRepository
import com.szczurek9.reminder.domain.ReminderManager
import com.szczurek9.reminder.integration.MediaBarClient
import com.szczurek9.reminder.notifications.NotificationChannels
import com.szczurek9.reminder.notifications.ReminderEngine
import com.szczurek9.reminder.notifications.ReminderScheduler
import com.szczurek9.reminder.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Prosty kontener zależności (bez frameworka DI). */
class ReminderApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ReminderDatabase by lazy { ReminderDatabase.create(this) }
    val repository: ReminderRepository by lazy { RoomReminderRepository(database.reminderDao()) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val scheduler: ReminderScheduler by lazy { ReminderScheduler(this) }
    val manager: ReminderManager by lazy { ReminderManager(repository, scheduler) }
    val mediaBar: MediaBarClient by lazy { MediaBarClient(this) }
    val engine: ReminderEngine by lazy { ReminderEngine(this, manager, repository, settings, mediaBar) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
    }
}
