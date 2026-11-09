package com.szczurek9.reminder.data

import com.szczurek9.reminder.domain.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Jedyne miejsce dostępu do danych przypomnień; niezależne od Media Bar i od sposobu prezentacji. */
interface ReminderRepository {
    fun observeActive(): Flow<List<Reminder>>
    fun observeDone(): Flow<List<Reminder>>
    fun observe(id: Long): Flow<Reminder?>
    suspend fun get(id: Long): Reminder?
    suspend fun getPending(): List<Reminder>
    suspend fun insert(reminder: Reminder): Long
    suspend fun update(reminder: Reminder)
    suspend fun delete(id: Long)
    suspend fun claimFire(id: Long, notifyAt: Long, now: Long): Boolean
}

class RoomReminderRepository(private val dao: ReminderDao) : ReminderRepository {
    override fun observeActive(): Flow<List<Reminder>> = dao.observeActive().map { l -> l.map { it.toDomain() } }
    override fun observeDone(): Flow<List<Reminder>> = dao.observeDone().map { l -> l.map { it.toDomain() } }
    override fun observe(id: Long): Flow<Reminder?> = dao.observe(id).map { it?.toDomain() }
    override suspend fun get(id: Long): Reminder? = dao.get(id)?.toDomain()
    override suspend fun getPending(): List<Reminder> = dao.getPending().map { it.toDomain() }
    override suspend fun insert(reminder: Reminder): Long = dao.insert(reminder.copy(id = 0L).toEntity())
    override suspend fun update(reminder: Reminder) = dao.update(reminder.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun claimFire(id: Long, notifyAt: Long, now: Long): Boolean =
        dao.claimFire(id, notifyAt, now) > 0
}
