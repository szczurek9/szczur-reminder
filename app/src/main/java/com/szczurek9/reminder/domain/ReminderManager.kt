package com.szczurek9.reminder.domain

import com.szczurek9.reminder.data.ReminderRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Abstrakcja planowania alarmów (implementacja: AlarmManager; w testach – atrapa). */
interface AlarmScheduler {
    fun schedule(reminderId: Long, triggerAtMillis: Long)
    fun cancel(reminderId: Long)
}

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult
    data class Invalid(val error: ValidationError) : SaveResult
    data object NotFound : SaveResult
}

/**
 * Logika przypomnień: baza + alarmy. Nic nie wie o sposobie prezentacji (Media Bar / powiadomienia systemowe).
 */
class ReminderManager(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {

    suspend fun create(
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime,
        leadMinutes: Int,
    ): SaveResult {
        val now = clock()
        val z = zone()
        ReminderValidator.validate(title, description, date, time, leadMinutes, now, z)?.let { return SaveResult.Invalid(it) }
        val notifyAt = ReminderTimes.notifyMillis(date, time, leadMinutes, z)
        val id = repository.insert(
            Reminder(
                title = title.trim(),
                description = description,
                eventDate = date,
                eventTime = time.withSecond(0).withNano(0),
                leadMinutes = leadMinutes,
                notifyAt = notifyAt,
                createdAt = now,
                updatedAt = now,
            ),
        )
        scheduler.schedule(id, notifyAt)
        return SaveResult.Saved(id)
    }

    /** Edycja zawsze zeruje odłożenie i flagę "powiadomiono" – przypomnienie znów jest aktywne. */
    suspend fun update(
        id: Long,
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime,
        leadMinutes: Int,
    ): SaveResult {
        val existing = repository.get(id) ?: return SaveResult.NotFound
        val now = clock()
        val z = zone()
        ReminderValidator.validate(title, description, date, time, leadMinutes, now, z)?.let { return SaveResult.Invalid(it) }
        val notifyAt = ReminderTimes.notifyMillis(date, time, leadMinutes, z)
        repository.update(
            existing.copy(
                title = title.trim(),
                description = description,
                eventDate = date,
                eventTime = time.withSecond(0).withNano(0),
                leadMinutes = leadMinutes,
                notifyAt = notifyAt,
                snoozeUntil = 0L,
                notified = false,
                status = ReminderStatus.UPCOMING,
                updatedAt = now,
            ),
        )
        scheduler.schedule(id, notifyAt)
        return SaveResult.Saved(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        repository.delete(id)
    }

    suspend fun markDone(id: Long) {
        val r = repository.get(id) ?: return
        scheduler.cancel(id)
        repository.update(r.copy(status = ReminderStatus.DONE, snoozeUntil = 0L, updatedAt = clock()))
    }

    /** Odkłada na [minutes] od teraz. Zwraca nowy moment powiadomienia albo null, gdy przypomnienia nie ma / jest wykonane. */
    suspend fun snooze(id: Long, minutes: Int): Long? {
        val r = repository.get(id) ?: return null
        if (r.status != ReminderStatus.UPCOMING) return null
        val now = clock()
        val until = now + minutes * 60_000L
        repository.update(r.copy(notifyAt = until, snoozeUntil = until, notified = false, updatedAt = now))
        scheduler.schedule(id, until)
        return until
    }

    /**
     * Odtwarza alarmy po restarcie, aktualizacji aplikacji oraz zmianie czasu / strefy.
     * Przelicza moment powiadomienia dla przypomnień nieodłożonych (strefa mogła się zmienić).
     * Zwraca przypomnienia, których moment minął, a powiadomienie nie padło (np. telefon był wyłączony) – do pokazania od razu.
     */
    suspend fun rescheduleAll(): List<Reminder> {
        val now = clock()
        val z = zone()
        val missed = mutableListOf<Reminder>()
        for (r in repository.getPending()) {
            var current = r
            if (r.snoozeUntil == 0L) {
                val recomputed = ReminderTimes.notifyMillis(r.eventDate, r.eventTime, r.leadMinutes, z)
                if (recomputed != r.notifyAt) {
                    current = r.copy(notifyAt = recomputed, updatedAt = now)
                    repository.update(current)
                }
            }
            if (current.notifyAt > now) {
                scheduler.schedule(current.id, current.notifyAt) // ten sam requestCode: nadpisuje, nie dubluje
            } else {
                scheduler.cancel(current.id)
                missed += current
            }
        }
        return missed
    }
}
