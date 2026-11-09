package com.szczurek9.reminder.notifications

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.szczurek9.reminder.receivers.ActionReceiver
import com.szczurek9.reminder.receivers.AlarmReceiver
import com.szczurek9.reminder.ui.MainActivity

/** Wszystkie PendingIntent-y aplikacji. Tożsamość = akcja + data (Uri), więc każdy jest unikalny dla przypomnienia. */
object ReminderIntents {
    const val ACTION_ALARM = "com.szczurek9.reminder.ALARM"
    const val ACTION_DONE = "com.szczurek9.reminder.DONE"
    const val ACTION_SNOOZE = "com.szczurek9.reminder.SNOOZE"
    const val ACTION_OPEN = "com.szczurek9.reminder.OPEN"

    const val EXTRA_ID = "reminder_id"
    const val EXTRA_NOTIFY_AT = "notify_at"
    const val EXTRA_MINUTES = "minutes"

    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun uri(id: Long, kind: String): Uri = Uri.parse("szczurreminder://reminder/$id/$kind")

    fun alarm(context: Context, id: Long, notifyAt: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_ALARM)
            .setData(uri(id, "alarm"))
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_NOTIFY_AT, notifyAt)
        return PendingIntent.getBroadcast(context, 0, intent, FLAGS)
    }

    fun done(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ActionReceiver::class.java)
            .setAction(ACTION_DONE)
            .setData(uri(id, "done"))
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(context, 0, intent, FLAGS)
    }

    fun snooze(context: Context, id: Long, minutes: Int): PendingIntent {
        val intent = Intent(context, ActionReceiver::class.java)
            .setAction(ACTION_SNOOZE)
            .setData(uri(id, "snooze/$minutes"))
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_MINUTES, minutes)
        return PendingIntent.getBroadcast(context, 0, intent, FLAGS)
    }

    /** Otwiera szczegóły przypomnienia. Zezwala na start z tła także wtedy, gdy PendingIntent wysyła Media Bar. */
    fun open(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN)
            .setData(uri(id, "open"))
            .putExtra(EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val options = ActivityOptions.makeBasic()
            .setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        return PendingIntent.getActivity(context, 0, intent, FLAGS, options.toBundle())
    }
}
