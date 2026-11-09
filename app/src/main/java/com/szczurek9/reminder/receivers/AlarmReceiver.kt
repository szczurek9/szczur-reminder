package com.szczurek9.reminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.szczurek9.reminder.ReminderApp
import com.szczurek9.reminder.notifications.ReminderIntents
import kotlinx.coroutines.launch

/** Odbiera alarm AlarmManager i oddaje go silnikowi. Nie zakłada, że proces aplikacji żył wcześniej. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderIntents.EXTRA_ID, -1L)
        val notifyAt = intent.getLongExtra(ReminderIntents.EXTRA_NOTIFY_AT, -1L)
        if (id < 0 || notifyAt < 0) return
        val app = context.applicationContext as ReminderApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.engine.onAlarm(id, notifyAt)
            } finally {
                pending.finish()
            }
        }
    }
}
