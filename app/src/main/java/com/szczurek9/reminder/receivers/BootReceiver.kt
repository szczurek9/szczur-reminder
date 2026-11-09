package com.szczurek9.reminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.szczurek9.reminder.ReminderApp
import kotlinx.coroutines.launch

/**
 * Odtwarza alarmy po restarcie, aktualizacji aplikacji, zmianie czasu systemowego i strefy czasowej.
 * Alarmy AlarmManagera giną przy restarcie i przy aktualizacji pakietu, a zmiana strefy przesuwa "ścienny" czas wydarzenia.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> Unit
            else -> return
        }
        val app = context.applicationContext as ReminderApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.engine.rescheduleAndFireMissed()
            } finally {
                pending.finish()
            }
        }
    }
}
