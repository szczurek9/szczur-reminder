package com.szczurek9.reminder.integration

import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.settings.AppSettings

/**
 * Sposób prezentacji przypomnienia. Nie wpływa na bazę ani na planowanie alarmów.
 * Implementacje: [MediaBarPresenter] (pasek Media Bar) i SystemNotificationPresenter (zwykłe powiadomienia Androida).
 */
interface ReminderPresenter {
    /** true = przypomnienie zostało faktycznie zaprezentowane. */
    suspend fun present(reminder: Reminder, settings: AppSettings): Boolean
}
