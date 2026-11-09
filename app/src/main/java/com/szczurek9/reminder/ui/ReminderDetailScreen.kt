package com.szczurek9.reminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.szczurek9.reminder.domain.LeadTime
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.ReminderStatus
import com.szczurek9.reminder.domain.formatEventLabel
import com.szczurek9.reminder.domain.formatMinutes
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

@Composable
fun ReminderDetailScreen(id: Long, vm: RemindersViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val reminder by remember(id) { vm.observe(id) }.collectAsStateWithLifecycle(initialValue = null)
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var snoozeMenu by remember { mutableStateOf(false) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć") }
            Text("Szczegóły przypomnienia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }

        val r: Reminder? = reminder
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Nie znaleziono przypomnienia (mogło zostać usunięte).", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        val zone = ZoneId.systemDefault()
        val overdue = r.isOverdue(now, zone)
        val statusLabel = when {
            r.status == ReminderStatus.DONE -> "Wykonane"
            overdue -> "Przeterminowane"
            else -> "Nadchodzące"
        }
        val notifyZdt = Instant.ofEpochMilli(r.notifyAt).atZone(zone)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(r.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            Card(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(formatEventLabel(r.eventDate, r.eventTime), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Status: $statusLabel", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Powiadomienie: ${LeadTime.fromMinutes(r.leadMinutes).label.lowercase()}" +
                            if (r.snoozeUntil > 0) " (odłożone na ${formatEventLabel(notifyZdt.toLocalDate(), notifyZdt.toLocalTime().withSecond(0).withNano(0))})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (r.description.isNotBlank()) {
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    MarkdownText(r.description, MaterialTheme.colorScheme.onSurface, Modifier.padding(18.dp))
                }
            }

            if (r.status == ReminderStatus.UPCOMING) {
                Button(onClick = { vm.markDone(r.id); onBack() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("  Oznacz jako wykonane")
                }
                Box {
                    OutlinedButton(onClick = { snoozeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Przypomnij później") }
                    DropdownMenu(expanded = snoozeMenu, onDismissRequest = { snoozeMenu = false }) {
                        settings.snoozeMinutes.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("Za ${formatMinutes(m)}") },
                                onClick = { snoozeMenu = false; vm.snooze(r.id, m) },
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onEdit(r.id) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("  Edytuj")
                }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("  Usuń")
                }
            }
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Usunąć przypomnienie?") },
                text = { Text("„${r.title}” zostanie usunięte bezpowrotnie.") },
                confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(r.id); onBack() }) { Text("Usuń") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anuluj") } },
            )
        }
    }
}
