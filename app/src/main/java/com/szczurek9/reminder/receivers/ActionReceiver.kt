package com.szczurek9.reminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.szczurek9.reminder.ReminderApp
import com.szczurek9.reminder.notifications.ReminderIntents
import kotlinx.coroutines.launch

/** "Wykonane" i "Przypomnij później" – z powiadomienia systemowego oraz z paska Media Bar (przez PendingIntent). */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderIntents.EXTRA_ID, -1L)
        if (id < 0) return
        val app = context.applicationContext as ReminderApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    ReminderIntents.ACTION_DONE -> app.engine.markDone(id)
                    ReminderIntents.ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(ReminderIntents.EXTRA_MINUTES, 0)
                        if (minutes > 0) app.engine.snooze(id, minutes)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
