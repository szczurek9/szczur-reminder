package com.szczurek9.reminder.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.szczurek9.reminder.R
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.formatEventLabel
import com.szczurek9.reminder.domain.formatMinutes
import com.szczurek9.reminder.integration.ReminderPresenter
import com.szczurek9.reminder.settings.AppSettings
import com.szczurek9.reminder.ui.markdownToPlain
import java.time.ZoneId

/**
 * Standardowe powiadomienie Androida (NotificationManager), bez własnego wyglądu.
 * Id powiadomienia = id przypomnienia, więc ponowne wysłanie (np. zmiana trybu) zastępuje wpis, nie dubluje go.
 * Systemowe powiadomienie pokazuje do 3 przycisków: "Wykonane" + dwa pierwsze interwały odłożenia.
 * Dotknięcie powiadomienia otwiera szczegóły przypomnienia.
 */
class SystemNotificationPresenter(
    private val context: Context,
    private val channelId: String,
) : ReminderPresenter {

    override suspend fun present(reminder: Reminder, settings: AppSettings): Boolean {
        NotificationChannels.ensure(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return false
        return try {
            nm.notify(reminder.id.toInt(), build(reminder, settings))
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun build(r: Reminder, s: AppSettings): Notification {
        val whenLabel = formatEventLabel(r.eventDate, r.eventTime)
        val plain = markdownToPlain(r.description)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(r.title)
            .setContentText(plain.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() } ?: whenLabel)
            .setSubText(whenLabel)
            .setWhen(r.eventMillis(ZoneId.systemDefault()))
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(ReminderIntents.open(context, r.id))
            .addAction(0, "Wykonane", ReminderIntents.done(context, r.id))
        if (plain.isNotBlank()) builder.setStyle(NotificationCompat.BigTextStyle().bigText(plain))
        s.snoozeMinutes.take(2).forEach { minutes ->
            builder.addAction(0, "Odłóż ${formatMinutes(minutes)}", ReminderIntents.snooze(context, r.id, minutes))
        }
        return builder.build()
    }
}
