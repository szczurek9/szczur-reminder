package com.szczurek9.reminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        fun create(context: Context): ReminderDatabase =
            Room.databaseBuilder(context.applicationContext, ReminderDatabase::class.java, "reminders.db").build()
    }
}
