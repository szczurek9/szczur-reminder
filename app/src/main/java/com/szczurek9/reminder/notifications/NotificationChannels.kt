package com.szczurek9.reminder.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.provider.Settings
import com.szczurek9.reminder.R

/**
 * Dwa kanały z dźwiękiem i wibracją:
 *  - ALERT (IMPORTANCE_HIGH): baner + dźwięk + wibracja – tryb standalone i awaryjny.
 *  - BAR (IMPORTANCE_DEFAULT): dźwięk + wibracja, ale bez baneru – gdy przypomnienie widać już na pasku Media Bar;
 *    wpis zostaje w centrum powiadomień i na ekranie blokady.
 * Parametry kanału (dźwięk, wibracja) użytkownik może zmienić w ustawieniach systemowych aplikacji.
 */
object NotificationChannels {
    const val ALERT = "reminders_alert_v1"
    const val BAR = "reminders_bar_v1"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pattern = longArrayOf(0, 300, 200, 300)

        fun channel(id: String, nameRes: Int, descRes: Int, importance: Int) =
            NotificationChannel(id, context.getString(nameRes), importance).apply {
                description = context.getString(descRes)
                enableVibration(true)
                vibrationPattern = pattern
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attrs)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

        nm.createNotificationChannel(
            channel(ALERT, R.string.channel_alert_name, R.string.channel_alert_desc, NotificationManager.IMPORTANCE_HIGH),
        )
        nm.createNotificationChannel(
            channel(BAR, R.string.channel_bar_name, R.string.channel_bar_desc, NotificationManager.IMPORTANCE_DEFAULT),
        )
    }
}
