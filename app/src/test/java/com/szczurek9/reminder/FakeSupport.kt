package com.szczurek9.reminder

import com.szczurek9.reminder.data.ReminderRepository
import com.szczurek9.reminder.domain.AlarmScheduler
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.ReminderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRepository : ReminderRepository {
    private val items = MutableStateFlow<Map<Long, Reminder>>(emptyMap())
    private var nextId = 1L

    override fun observeActive(): Flow<List<Reminder>> =
        items.map { m -> m.values.filter { it.status == ReminderStatus.UPCOMING } }
    override fun observeDone(): Flow<List<Reminder>> =
        items.map { m -> m.values.filter { it.status == ReminderStatus.DONE } }
    override fun observe(id: Long): Flow<Reminder?> = items.map { it[id] }
    override suspend fun get(id: Long): Reminder? = items.value[id]
    override suspend fun getPending(): List<Reminder> =
        items.value.values.filter { it.status == ReminderStatus.UPCOMING && !it.notified }
    override suspend fun insert(reminder: Reminder): Long {
        val id = nextId++
        items.value = items.value + (id to reminder.copy(id = id))
        return id
    }
    override suspend fun update(reminder: Reminder) { items.value = items.value + (reminder.id to reminder) }
    override suspend fun delete(id: Long) { items.value = items.value - id }
    override suspend fun claimFire(id: Long, notifyAt: Long, now: Long): Boolean {
        val r = items.value[id] ?: return false
        if (r.status != ReminderStatus.UPCOMING || r.notified || r.notifyAt != notifyAt) return false
        items.value = items.value + (id to r.copy(notified = true, updatedAt = now))
        return true
    }
}

class FakeScheduler : AlarmScheduler {
    val scheduled = mutableMapOf<Long, Long>()
    override fun schedule(reminderId: Long, triggerAtMillis: Long) { scheduled[reminderId] = triggerAtMillis }
    override fun cancel(reminderId: Long) { scheduled.remove(reminderId) }
}
