package com.szczurek9.reminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status = 'UPCOMING' ORDER BY eventDate, eventTime, id")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'DONE' ORDER BY updatedAt DESC")
    fun observeDone(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observe(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    /** Aktywne przypomnienia, których powiadomienie jeszcze nie padło – do (ponownego) zaplanowania alarmów. */
    @Query("SELECT * FROM reminders WHERE status = 'UPCOMING' AND notified = 0")
    suspend fun getPending(): List<ReminderEntity>

    @Insert
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Atomowe "zajęcie" odpalenia: zwraca 1 tylko dla pierwszego wywołania z aktualnym notifyAt.
     * Chroni przed dubletami (powtórzony alarm, alarm + odtwarzanie po restarcie, nieaktualny alarm po edycji).
     */
    @Query(
        "UPDATE reminders SET notified = 1, updatedAt = :now " +
            "WHERE id = :id AND status = 'UPCOMING' AND notified = 0 AND notifyAt = :notifyAt",
    )
    suspend fun claimFire(id: Long, notifyAt: Long, now: Long): Int
}
