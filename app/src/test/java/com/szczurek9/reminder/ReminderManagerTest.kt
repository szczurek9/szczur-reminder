package com.szczurek9.reminder

import com.szczurek9.reminder.domain.ReminderManager
import com.szczurek9.reminder.domain.ReminderStatus
import com.szczurek9.reminder.domain.ReminderTimes
import com.szczurek9.reminder.domain.SaveResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderManagerTest {
    private val zoneRef = arrayOf(ZoneId.of("Europe/Warsaw"))
    private val date = LocalDate.of(2026, 12, 10)
    private var now = ZonedDateTime.of(date, LocalTime.of(12, 0), zoneRef[0]).toInstant().toEpochMilli()
    private val repo = FakeRepository()
    private val scheduler = FakeScheduler()
    private val manager = ReminderManager(repo, scheduler, clock = { now }, zone = { zoneRef[0] })

    private fun at(h: Int, m: Int = 0) = ZonedDateTime.of(date, LocalTime.of(h, m), zoneRef[0]).toInstant().toEpochMilli()

    private fun create(h: Int = 15, lead: Int = 30): Long = runBlocking {
        (manager.create("Tytuł", "**opis**", date, LocalTime.of(h, 0), lead) as SaveResult.Saved).id
    }

    @Test fun `tworzenie planuje alarm 14 30`() {
        val id = create()
        assertEquals(at(14, 30), scheduler.scheduled[id])
        val r = runBlocking { repo.get(id)!! }
        assertEquals("**opis**", r.description) // zapisujemy surowy Markdown
        assertEquals(ReminderStatus.UPCOMING, r.status)
    }

    @Test fun `niepoprawne dane nic nie zapisuja`() {
        val res = runBlocking { manager.create("", "", date, LocalTime.of(15, 0), 0) }
        assertTrue(res is SaveResult.Invalid)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test fun `edycja przenosi alarm i zeruje odlozenie`() {
        val id = create()
        runBlocking { manager.snooze(id, 10) }
        runBlocking { manager.update(id, "Nowy", "", date, LocalTime.of(18, 0), 60) }
        assertEquals(at(17, 0), scheduler.scheduled[id])
        val r = runBlocking { repo.get(id)!! }
        assertEquals(0L, r.snoozeUntil)
        assertFalse(r.notified)
        assertEquals("Nowy", r.title)
    }

    @Test fun `usuniecie kasuje alarm i rekord`() {
        val id = create()
        runBlocking { manager.delete(id) }
        assertNull(scheduler.scheduled[id])
        assertNull(runBlocking { repo.get(id) })
    }

    @Test fun `wykonane anuluje alarm`() {
        val id = create()
        runBlocking { manager.markDone(id) }
        assertNull(scheduler.scheduled[id])
        assertEquals(ReminderStatus.DONE, runBlocking { repo.get(id)!!.status })
    }

    @Test fun `odloz o 15 minut`() {
        val id = create()
        now = at(14, 30)
        runBlocking { repo.claimFire(id, at(14, 30), now) }
        val until = runBlocking { manager.snooze(id, 15) }
        assertEquals(at(14, 45), until)
        assertEquals(at(14, 45), scheduler.scheduled[id])
        assertFalse(runBlocking { repo.get(id)!!.notified })
    }

    @Test fun `claimFire jest jednorazowy - brak duplikatow`() {
        val id = create()
        assertTrue(runBlocking { repo.claimFire(id, at(14, 30), now) })
        assertFalse(runBlocking { repo.claimFire(id, at(14, 30), now) })
    }

    @Test fun `nieaktualny alarm po edycji jest ignorowany`() {
        val id = create()
        runBlocking { manager.update(id, "T", "", date, LocalTime.of(18, 0), 0) }
        assertFalse(runBlocking { repo.claimFire(id, at(14, 30), now) })
    }

    @Test fun `restart - przyszle planowane, zalegle zwrocone`() {
        val future = create(h = 15)
        val past = create(h = 13, lead = 0)
        scheduler.scheduled.clear()
        now = at(13, 30)
        val missed = runBlocking { manager.rescheduleAll() }
        assertEquals(listOf(past), missed.map { it.id })
        assertEquals(at(14, 30), scheduler.scheduled[future])
        assertNull(scheduler.scheduled[past])
    }

    @Test fun `zmiana strefy przelicza moment powiadomienia`() {
        val id = create(h = 15, lead = 30)
        zoneRef[0] = ZoneId.of("Europe/London")
        runBlocking { manager.rescheduleAll() }
        val expected = ReminderTimes.notifyMillis(date, LocalTime.of(15, 0), 30, ZoneId.of("Europe/London"))
        assertEquals(expected, scheduler.scheduled[id])
        assertEquals(expected, runBlocking { repo.get(id)!!.notifyAt })
    }

    @Test fun `odlozone nie jest przeliczane przy zmianie strefy`() {
        val id = create()
        now = at(14, 30)
        val until = runBlocking { manager.snooze(id, 30) }!!
        zoneRef[0] = ZoneId.of("Asia/Tokyo")
        runBlocking { manager.rescheduleAll() }
        assertEquals(until, scheduler.scheduled[id])
    }
}
