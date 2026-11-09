package com.szczurek9.reminder.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.szczurek9.reminder.ReminderApp
import com.szczurek9.reminder.notifications.ReminderIntents
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Id przypomnienia do otwarcia (z powiadomienia lub z paska Media Bar); -1 = brak. */
    private var openId by mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openId = extractId(intent)
        val app = application as ReminderApp
        if (savedInstanceState == null) {
            // Odtworzenie alarmów m.in. po wymuszonym zatrzymaniu aplikacji (system kasuje wtedy alarmy).
            app.appScope.launch { app.engine.rescheduleAndFireMissed() }
        }
        setContent {
            SzczurTheme { AppRoot(openId = openId, onOpenConsumed = { openId = -1L }) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractId(intent).takeIf { it >= 0 }?.let { openId = it }
    }

    private fun extractId(intent: Intent?): Long =
        if (intent?.action == ReminderIntents.ACTION_OPEN) intent.getLongExtra(ReminderIntents.EXTRA_ID, -1L) else -1L
}
