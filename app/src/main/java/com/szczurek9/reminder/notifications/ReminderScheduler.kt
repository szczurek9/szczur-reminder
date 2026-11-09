package com.szczurek9.reminder.notifications

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import com.szczurek9.reminder.domain.AlarmScheduler

/**
 * Planowanie przez AlarmManager. Jeden PendingIntent na przypomnienie (data = id), więc ponowne
 * zaplanowanie nadpisuje poprzedni alarm zamiast go dublować.
 */
class ReminderScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = alarmManager.canScheduleExactAlarms()

    override fun schedule(reminderId: Long, triggerAtMillis: Long) {
        val pi = ReminderIntents.alarm(context, reminderId, triggerAtMillis)
        try {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                // Uprawnienie USE_EXACT_ALARM jest przyznawane automatycznie; to tylko awaryjnie (alarm może się spóźnić).
                Log.w(TAG, "Brak dokładnych alarmów – używam niedokładnego dla #$reminderId")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException przy planowaniu #$reminderId", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    override fun cancel(reminderId: Long) {
        val pi = ReminderIntents.alarm(context, reminderId, 0L)
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private companion object {
        const val TAG = "ReminderScheduler"
    }
}
