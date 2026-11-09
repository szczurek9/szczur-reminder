package com.szczurek9.reminder.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class ReminderStatus { UPCOMING, DONE }

/** Model domenowy. Data i godzina wydarzenia są czasem lokalnym ("ściennym"), więc przeżywają zmianę strefy. */
data class Reminder(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val eventDate: LocalDate,
    val eventTime: LocalTime,
    val leadMinutes: Int,
    /** Obliczony moment powiadomienia (epoch ms); po odłożeniu = [snoozeUntil]. */
    val notifyAt: Long,
    /** 0 = brak odłożenia; w przeciwnym razie moment, na który odłożono przypomnienie. */
    val snoozeUntil: Long = 0L,
    /** Czy powiadomienie dla bieżącego [notifyAt] już zostało wywołane. */
    val notified: Boolean = false,
    val status: ReminderStatus = ReminderStatus.UPCOMING,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun eventMillis(zone: ZoneId): Long = ReminderTimes.eventMillis(eventDate, eventTime, zone)

    fun isOverdue(now: Long, zone: ZoneId): Boolean =
        status == ReminderStatus.UPCOMING && eventMillis(zone) < now
}

/** Presety "kiedy przed wydarzeniem pokazać powiadomienie". */
enum class LeadTime(val minutes: Int, val label: String) {
    AT_TIME(0, "O wybranej godzinie"),
    MIN_5(5, "5 minut wcześniej"),
    MIN_15(15, "15 minut wcześniej"),
    MIN_30(30, "30 minut wcześniej"),
    MIN_60(60, "60 minut wcześniej"),
    DAY_1(1440, "1 dzień wcześniej");

    companion object {
        fun fromMinutes(m: Int): LeadTime = entries.firstOrNull { it.minutes == m } ?: AT_TIME
    }
}
