package com.szczurek9.reminder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.szczurek9.reminder.domain.LeadTime
import com.szczurek9.reminder.domain.formatMinutes
import com.szczurek9.reminder.integration.MediaBarStatus
import com.szczurek9.reminder.settings.AppSettings

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mediaBar by vm.mediaBar.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }

    // Uprawnienia i status integracji mogły się zmienić poza aplikacją.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(tick, settings.integrationEnabled) { vm.refreshMediaBar() }

    val notifGranted = remember(tick) { isNotificationPermissionGranted(ctx) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val exactOk = remember(tick) { vm.canScheduleExact() }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Ustawienia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))

        Section("Powiadomienia") {
            SwitchRow("Powiadomienia włączone", null, settings.notificationsEnabled) { vm.setNotificationsEnabled(it) }

            var seconds by remember(settings.barDisplaySeconds) { mutableIntStateOf(settings.barDisplaySeconds) }
            Text("Czas wyświetlania na pasku Media Bar: $seconds s", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = seconds.toFloat(),
                onValueChange = { seconds = it.toInt() },
                onValueChangeFinished = { vm.setBarSeconds(seconds) },
                valueRange = 3f..10f,
                steps = 6,
            )
            Hint("Dotyczy wyłącznie paska Media Bar. Zwykłe powiadomienie Androida jest zarządzane przez system. Po tym czasie pasek znika, ale przypomnienie nie jest oznaczane jako wykonane i zostaje w centrum powiadomień.")

            Text("Domyślny moment powiadomienia", style = MaterialTheme.typography.bodyLarge)
            LeadTime.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setDefaultLead(option) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.defaultLeadMinutes == option.minutes, onClick = { vm.setDefaultLead(option) })
                    Text(option.label)
                }
            }
        }

        Section("Przypomnij później") {
            Hint("Zaznaczone interwały pojawią się jako przyciski na pasku Media Bar. W powiadomieniu Androida system pokazuje maksymalnie 3 przyciski, więc oprócz „Wykonane” są dwa pierwsze (najkrótsze) interwały.")
            AppSettings.SNOOZE_CHOICES.forEach { minutes ->
                val checked = minutes in settings.snoozeMinutes
                Row(
                    Modifier.fillMaxWidth().clickable { toggleSnooze(vm, settings.snoozeMinutes, minutes) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { toggleSnooze(vm, settings.snoozeMinutes, minutes) })
                    Text(formatMinutes(minutes))
                }
            }
        }

        Section("Integracja z Media Bar") {
            SwitchRow(
                "Używaj szczur media bar",
                "Gdy wyłączone albo Media Bar jest niedostępny, przypomnienia idą jako zwykłe powiadomienia Androida.",
                settings.integrationEnabled,
            ) { vm.setIntegrationEnabled(it) }
            Text(integrationText(settings.integrationEnabled, mediaBar), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { vm.refreshMediaBar() }) { Text("Sprawdź ponownie") }
        }

        Section("Uprawnienia") {
            PermissionRow("Powiadomienia", notifGranted, "Wymagane do pokazywania przypomnień (Android 13+).") {
                if (!notifGranted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!notifGranted) {
                OutlinedButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text("Otwórz ustawienia powiadomień") }
            }
            PermissionRow("Dokładne alarmy", exactOk, "Pozwalają pokazać przypomnienie punktualnie, także w trybie oszczędzania energii. Przyznawane automatycznie.") {}
            Hint("Uruchamianie po restarcie (RECEIVE_BOOT_COMPLETED) i wibracja nie wymagają zgody użytkownika.")
        }

        Section("O aplikacji") {
            Text("szczur reminder", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("Wersja ${appVersion(ctx)} (protokół integracji v1)", style = MaterialTheme.typography.bodyMedium)
            Hint("Współpracuje z aplikacją szczur media bar 1.1.0 lub nowszym. Dane przypomnień są przechowywane lokalnie, niezależnie od Media Bar.")
        }
    }
}

private fun toggleSnooze(vm: SettingsViewModel, current: List<Int>, minutes: Int) {
    val next = if (minutes in current) current - minutes else current + minutes
    if (next.isNotEmpty()) vm.setSnoozeMinutes(next) // zawsze zostaje co najmniej jeden interwał
}

private fun isNotificationPermissionGranted(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun appVersion(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
} catch (e: Exception) {
    "?"
}

private fun reasonText(reason: String?): String = when (reason) {
    "disabled_in_media_bar" -> "przypomnienia są wyłączone w ustawieniach Media Bar"
    "no_overlay_permission" -> "Media Bar nie ma uprawnienia „Wyświetlanie nad innymi aplikacjami”"
    "locked" -> "ekran jest zablokowany (pasek nie działa na blokadzie)"
    "screen_off" -> "ekran jest wyłączony"
    "listener_not_connected" -> "Media Bar nie ma aktywnego dostępu do powiadomień"
    null -> "nieznany powód"
    else -> reason
}

private fun integrationText(enabled: Boolean, status: MediaBarStatus?): String {
    if (!enabled) return "Status: wyłączona w ustawieniach – używam powiadomień Androida."
    return when (status) {
        null -> "Status: sprawdzanie…"
        MediaBarStatus.NotInstalled -> "Status: Szczur Media Bar nie jest zainstalowany – używam powiadomień Androida."
        is MediaBarStatus.Unreachable -> "Status: niedostępny. ${status.detail}. Używam powiadomień Androida."
        is MediaBarStatus.Incompatible ->
            "Status: niezgodna wersja protokołu (Media Bar: v${status.mediaBarProtocol}, Reminder: v1). Zaktualizuj obie aplikacje. Używam powiadomień Androida."
        is MediaBarStatus.Available ->
            if (status.canShow) "Status: połączono z Media Bar (versionCode ${status.versionCode}) – przypomnienia pojawią się na pasku."
            else "Status: połączono, ale pasek chwilowo niedostępny: ${reasonText(status.reason)}. W tym czasie działają powiadomienia Androida."
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Hint(subtitle)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Hint(subtitle)
        }
        Text(
            if (granted) "Przyznane" else "Brak",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
