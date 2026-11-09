package com.szczurek9.reminder.integration

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.formatEventLabel
import com.szczurek9.reminder.domain.formatMinutes
import com.szczurek9.reminder.notifications.ReminderIntents
import com.szczurek9.reminder.settings.AppSettings

/**
 * Pokazuje przypomnienie na pasku Szczur Media Bar. Akcje wracają przez PendingIntent-y tej aplikacji
 * (niewyeksportowane receivery) – Media Bar tylko je uruchamia, więc nikt nie może podrobić akcji "Wykonane".
 */
class MediaBarPresenter(
    private val context: Context,
    private val client: MediaBarClient,
) : ReminderPresenter {

    override suspend fun present(reminder: Reminder, settings: AppSettings): Boolean {
        val result = client.show(buildPayload(reminder, settings)) ?: return false
        return result.shown
    }

    private fun buildPayload(r: Reminder, s: AppSettings): Bundle {
        val snoozes = s.snoozeMinutes
        return Bundle().apply {
            putLong(MediaBarProtocol.K_ID, r.id)
            putString(MediaBarProtocol.K_TITLE, r.title)
            putString(MediaBarProtocol.K_DESCRIPTION, r.description)
            putString(MediaBarProtocol.K_WHEN, formatEventLabel(r.eventDate, r.eventTime))
            putInt(MediaBarProtocol.K_DISPLAY_SEC, s.barDisplaySeconds)
            putParcelable(MediaBarProtocol.K_DONE_PI, ReminderIntents.done(context, r.id))
            putParcelable(MediaBarProtocol.K_OPEN_PI, ReminderIntents.open(context, r.id))
            putParcelableArrayList(
                MediaBarProtocol.K_SNOOZE_PIS,
                ArrayList<PendingIntent>(snoozes.map { ReminderIntents.snooze(context, r.id, it) }),
            )
            putStringArrayList(MediaBarProtocol.K_SNOOZE_LABELS, ArrayList(snoozes.map { formatMinutes(it) }))
        }
    }
}
