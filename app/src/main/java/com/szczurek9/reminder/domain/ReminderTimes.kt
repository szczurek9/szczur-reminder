package com.szczurek9.reminder.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderTimes {

    fun eventMillis(date: LocalDate, time: LocalTime, zone: ZoneId): Long =
        ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()

    /** Przykład: wydarzenie 15:00, wyprzedzenie 30 min -> 14:30. Pełne doby liczone kalendarzowo (zmiana czasu). */
    fun notifyMillis(date: LocalDate, time: LocalTime, leadMinutes: Int, zone: ZoneId): Long {
        val event = ZonedDateTime.of(date, time, zone)
        val at = if (leadMinutes > 0 && leadMinutes % 1440 == 0) {
            event.minusDays((leadMinutes / 1440).toLong())
        } else {
            event.minusMinutes(leadMinutes.toLong())
        }
        return at.toInstant().toEpochMilli()
    }
}

enum class ValidationError(val message: String) {
    TITLE_EMPTY("Podaj tytuł przypomnienia"),
    TITLE_TOO_LONG("Tytuł jest za długi (maks. ${ReminderValidator.MAX_TITLE} znaków)"),
    DESCRIPTION_TOO_LONG("Opis jest za długi (maks. ${ReminderValidator.MAX_DESCRIPTION} znaków)"),
    EVENT_IN_PAST("Data i godzina muszą być w przyszłości"),
    NOTIFY_IN_PAST("Moment powiadomienia już minął – wybierz krótsze wyprzedzenie albo późniejszy termin"),
}

object ReminderValidator {
    const val MAX_TITLE = 80
    const val MAX_DESCRIPTION = 4000

    fun validate(
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime,
        leadMinutes: Int,
        now: Long,
        zone: ZoneId,
    ): ValidationError? {
        val t = title.trim()
        if (t.isEmpty()) return ValidationError.TITLE_EMPTY
        if (t.length > MAX_TITLE) return ValidationError.TITLE_TOO_LONG
        if (description.length > MAX_DESCRIPTION) return ValidationError.DESCRIPTION_TOO_LONG
        if (ReminderTimes.eventMillis(date, time, zone) <= now) return ValidationError.EVENT_IN_PAST
        if (ReminderTimes.notifyMillis(date, time, leadMinutes, zone) <= now) return ValidationError.NOTIFY_IN_PAST
        return null
    }
}
