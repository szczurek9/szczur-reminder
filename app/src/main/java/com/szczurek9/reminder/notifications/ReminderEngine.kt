package com.szczurek9.reminder.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.szczurek9.reminder.data.ReminderRepository
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.ReminderManager
import com.szczurek9.reminder.integration.MediaBarClient
import com.szczurek9.reminder.integration.MediaBarPresenter
import com.szczurek9.reminder.integration.ReminderPresenter
import com.szczurek9.reminder.settings.SettingsRepository

/**
 * Spina logikę przypomnień ([ReminderManager]) z prezentacją. Wybór mechanizmu odbywa się dopiero w chwili alarmu:
 * Media Bar dostępny -> pasek + ciche (bez baneru) powiadomienie w centrum; inaczej -> zwykłe powiadomienie z banerem.
 * Dzięki temu przełączanie trybów nie dotyka bazy ani alarmów.
 */
class ReminderEngine(
    private val context: Context,
    val manager: ReminderManager,
    private val repository: ReminderRepository,
    private val settings: SettingsRepository,
    private val mediaBarClient: MediaBarClient,
) {
    private val barPresenter: ReminderPresenter = MediaBarPresenter(context, mediaBarClient)
    private val alertPresenter: ReminderPresenter = SystemNotificationPresenter(context, NotificationChannels.ALERT)
    private val quietPresenter: ReminderPresenter = SystemNotificationPresenter(context, NotificationChannels.BAR)

    /** Wywołane z AlarmReceiver. Nieaktualne lub powtórzone alarmy są ignorowane (claimFire). */
    suspend fun onAlarm(id: Long, notifyAt: Long) {
        val r = repository.get(id) ?: return
        if (!repository.claimFire(id, notifyAt, System.currentTimeMillis())) return
        dispatch(r)
    }

    /** Przypomnienie, którego moment minął, gdy telefon był wyłączony / aplikacja zatrzymana. */
    suspend fun fireMissed(r: Reminder) {
        if (!repository.claimFire(r.id, r.notifyAt, System.currentTimeMillis())) return
        dispatch(r)
    }

    /** Odtwarza alarmy i pokazuje zaległe. Bezpieczne do wielokrotnego wywołania (BootReceiver, start aplikacji). */
    suspend fun rescheduleAndFireMissed() {
        manager.rescheduleAll().forEach { fireMissed(it) }
    }

    suspend fun markDone(id: Long) {
        manager.markDone(id)
        clearPresentation(id)
    }

    suspend fun snooze(id: Long, minutes: Int) {
        manager.snooze(id, minutes)
        clearPresentation(id)
    }

    suspend fun delete(id: Long) {
        manager.delete(id)
        clearPresentation(id)
    }

    /** Zdejmuje powiadomienie systemowe i (best effort) chowa pasek Media Bar. */
    suspend fun clearPresentation(id: Long) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
        runCatching { mediaBarClient.hide(id) }
    }

    private suspend fun dispatch(r: Reminder) {
        val s = settings.current()
        if (!s.notificationsEnabled) return
        val shownOnBar = s.integrationEnabled && runCatching { barPresenter.present(r, s) }.getOrDefault(false)
        (if (shownOnBar) quietPresenter else alertPresenter).present(r, s)
    }
}
