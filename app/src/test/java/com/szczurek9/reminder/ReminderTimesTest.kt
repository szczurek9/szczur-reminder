package com.szczurek9.reminder

import com.szczurek9.reminder.domain.ReminderTimes
import com.szczurek9.reminder.domain.ReminderValidator
import com.szczurek9.reminder.domain.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimesTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private val date = LocalDate.of(2026, 12, 10)

    private fun millis(h: Int, m: Int, d: LocalDate = date) = ZonedDateTime.of(d, LocalTime.of(h, m), zone).toInstant().toEpochMilli()

    @Test fun `o wybranej godzinie`() = assertEquals(millis(15, 0), ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 0, zone))
    @Test fun `30 minut wczesniej o 15 daje 14 30`() = assertEquals(millis(14, 30), ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 30, zone))
    @Test fun `5 15 60 minut`() {
        assertEquals(millis(14, 55), ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 5, zone))
        assertEquals(millis(14, 45), ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 15, zone))
        assertEquals(millis(14, 0), ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 60, zone))
    }
    @Test fun `1 dzien wczesniej`() =
        assertEquals(millis(15, 0, date.minusDays(1)), ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 1440, zone))

    @Test fun `zmiana strefy przesuwa moment bezwzgledny`() {
        val a = ReminderTimes.eventMillis(date, LocalTime.of(15, 0), ZoneId.of("Europe/Warsaw"))
        val b = ReminderTimes.eventMillis(date, LocalTime.of(15, 0), ZoneId.of("Europe/London"))
        assertEquals(3_600_000L, b - a)
    }

    @Test fun `walidacja`() {
        val now = millis(12, 0)
        assertEquals(ValidationError.TITLE_EMPTY, ReminderValidator.validate("  ", "", date, LocalTime.of(15, 0), 0, now, zone))
        assertEquals(ValidationError.TITLE_TOO_LONG, ReminderValidator.validate("x".repeat(81), "", date, LocalTime.of(15, 0), 0, now, zone))
        assertEquals(ValidationError.EVENT_IN_PAST, ReminderValidator.validate("a", "", date, LocalTime.of(11, 0), 0, now, zone))
        assertEquals(ValidationError.NOTIFY_IN_PAST, ReminderValidator.validate("a", "", date, LocalTime.of(12, 20), 30, now, zone))
        assertNull(ReminderValidator.validate("a", "", date, LocalTime.of(15, 0), 30, now, zone))
    }
}
