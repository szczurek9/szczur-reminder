package com.szczurek9.reminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.ReminderStatus
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "reminders", indices = [Index("status"), Index("notifyAt")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    /** ISO yyyy-MM-dd */
    val eventDate: String,
    /** ISO HH:mm */
    val eventTime: String,
    val leadMinutes: Int,
    val notifyAt: Long,
    val snoozeUntil: Long,
    val notified: Boolean,
    /** UPCOMING / DONE */
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    eventDate = LocalDate.parse(eventDate),
    eventTime = LocalTime.parse(eventTime),
    leadMinutes = leadMinutes,
    notifyAt = notifyAt,
    snoozeUntil = snoozeUntil,
    notified = notified,
    status = runCatching { ReminderStatus.valueOf(status) }.getOrDefault(ReminderStatus.UPCOMING),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    description = description,
    eventDate = eventDate.toString(),
    eventTime = eventTime.withSecond(0).withNano(0).toString(),
    leadMinutes = leadMinutes,
    notifyAt = notifyAt,
    snoozeUntil = snoozeUntil,
    notified = notified,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
