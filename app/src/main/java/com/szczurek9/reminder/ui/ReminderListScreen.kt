package com.szczurek9.reminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.ReminderStatus
import com.szczurek9.reminder.domain.formatEventLabel
import kotlinx.coroutines.delay
import java.time.ZoneId

private enum class TileState { UPCOMING, OVERDUE, DONE }

@Composable
fun ReminderListScreen(vm: RemindersViewModel, onOpen: (Long) -> Unit, onEdit: (Long) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val active by vm.active.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }

    // Odświeżanie co 30 s, żeby przypomnienie samo przeszło w stan "przeterminowane".
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val zone = ZoneId.systemDefault()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Przypomnienia",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Nadchodzące${active?.let { " (${it.size})" } ?: ""}") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Wykonane${done?.let { " (${it.size})" } ?: ""}") })
        }

        val list = if (tab == 0) active else done
        when {
            list == null -> Box(Modifier.fillMaxSize())
            list.isEmpty() -> EmptyState(historyTab = tab == 1)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { r ->
                    val state = when {
                        r.status == ReminderStatus.DONE -> TileState.DONE
                        r.isOverdue(now, zone) -> TileState.OVERDUE
                        else -> TileState.UPCOMING
                    }
                    ReminderTile(
                        reminder = r,
                        state = state,
                        onClick = { onOpen(r.id) },
                        onDone = { vm.markDone(r.id) },
                        onEdit = { onEdit(r.id) },
                        onDelete = { pendingDelete = r },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    pendingDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Usunąć przypomnienie?") },
            text = { Text("„${r.title}” zostanie usunięte bezpowrotnie.") },
            confirmButton = { TextButton(onClick = { vm.delete(r.id); pendingDelete = null }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Anuluj") } },
        )
    }
}

@Composable
private fun ReminderTile(
    reminder: Reminder,
    state: TileState,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when (state) {
        TileState.UPCOMING -> scheme.secondaryContainer
        TileState.OVERDUE -> scheme.errorContainer
        TileState.DONE -> scheme.surfaceVariant
    }
    val content = when (state) {
        TileState.UPCOMING -> scheme.onSecondaryContainer
        TileState.OVERDUE -> scheme.onErrorContainer
        TileState.DONE -> scheme.onSurfaceVariant
    }
    val statusLabel = when (state) {
        TileState.UPCOMING -> "Nadchodzące"
        TileState.OVERDUE -> "Przeterminowane"
        TileState.DONE -> "Wykonane"
    }
    val preview = remember(reminder.description) { markdownToPlain(reminder.description).replace('\n', ' ').trim() }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (state == TileState.DONE) TextDecoration.LineThrough else null,
                modifier = Modifier.padding(end = 10.dp),
            )
            if (preview.isNotEmpty()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(0.85f).padding(top = 4.dp, end = 10.dp),
                )
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatEventLabel(reminder.eventDate, reminder.eventTime), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.alpha(0.8f))
                }
                if (state != TileState.DONE) {
                    IconButton(onClick = onDone) { Icon(Icons.Filled.Check, contentDescription = "Oznacz jako wykonane") }
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edytuj") }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Usuń") }
            }
        }
    }
}

@Composable
private fun EmptyState(historyTab: Boolean) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(88.dp).alpha(0.35f),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                if (historyTab) "Brak wykonanych przypomnień" else "Brak przypomnień",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (historyTab) "Tu pojawią się przypomnienia oznaczone jako wykonane."
                else "Dotknij przycisku + na dole, aby dodać pierwsze.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
