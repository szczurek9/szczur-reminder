package com.szczurek9.reminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.szczurek9.reminder.domain.LeadTime
import com.szczurek9.reminder.domain.ReminderValidator
import com.szczurek9.reminder.domain.SaveResult
import com.szczurek9.reminder.domain.ValidationError
import com.szczurek9.reminder.domain.formatDate
import com.szczurek9.reminder.domain.formatTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorScreen(id: Long?, vm: RemindersViewModel, onClose: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var dateEpoch by rememberSaveable { mutableLongStateOf(defaultStart().toLocalDate().toEpochDay()) }
    var minuteOfDay by rememberSaveable { mutableIntStateOf(defaultStart().toLocalTime().toSecondOfDay() / 60) }
    var leadOverride by rememberSaveable { mutableStateOf<Int?>(null) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<ValidationError?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null && !loaded) {
            val r = vm.load(id)
            if (r == null) {
                onClose()
            } else {
                title = r.title
                description = r.description
                dateEpoch = r.eventDate.toEpochDay()
                minuteOfDay = r.eventTime.toSecondOfDay() / 60
                leadOverride = r.leadMinutes
                loaded = true
            }
        }
    }

    val date = LocalDate.ofEpochDay(dateEpoch)
    val time = LocalTime.ofSecondOfDay(minuteOfDay * 60L)
    val lead = leadOverride ?: settings.defaultLeadMinutes
    val zone = ZoneId.systemDefault()
    val validation = ReminderValidator.validate(title, description, date, time, lead, System.currentTimeMillis(), zone)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anuluj") }
            Text(
                if (id == null) "Nowe przypomnienie" else "Edytuj przypomnienie",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= ReminderValidator.MAX_TITLE) { title = it; serverError = null } },
                label = { Text("Tytuł") },
                singleLine = true,
                isError = attempted && validation == ValidationError.TITLE_EMPTY,
                supportingText = {
                    if (attempted && validation == ValidationError.TITLE_EMPTY) Text(ValidationError.TITLE_EMPTY.message)
                    else Text("${title.length}/${ReminderValidator.MAX_TITLE}")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Opis", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    FilterChip(selected = !preview, onClick = { preview = false }, label = { Text("Edycja") })
                    FilterChip(selected = preview, onClick = { preview = true }, label = { Text("Podgląd") })
                }
                if (preview) {
                    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                        if (description.isBlank()) {
                            Text("Brak opisu", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            MarkdownText(description, MaterialTheme.colorScheme.onSurface, Modifier.padding(16.dp))
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= ReminderValidator.MAX_DESCRIPTION) description = it },
                        placeholder = { Text("Opcjonalny opis. Markdown: **pogrubienie**, *kursywa*, ~~przekreślenie~~, # nagłówek, - lista, 1. lista numerowana") },
                        minLines = 5,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Termin", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) { Text(formatDate(date)) }
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) { Text(formatTime(time)) }
                }
                if (validation == ValidationError.EVENT_IN_PAST) {
                    Text(ValidationError.EVENT_IN_PAST.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Column {
                Text("Kiedy pokazać powiadomienie", style = MaterialTheme.typography.titleSmall)
                LeadTime.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { leadOverride = option.minutes },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = lead == option.minutes, onClick = { leadOverride = option.minutes })
                        Text(option.label)
                    }
                }
                if (validation == ValidationError.NOTIFY_IN_PAST) {
                    Text(ValidationError.NOTIFY_IN_PAST.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            serverError?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }

            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Anuluj") }
                Button(
                    onClick = {
                        attempted = true
                        if (validation != null) return@Button
                        vm.save(id, title, description, date, time, lead) { result ->
                            when (result) {
                                is SaveResult.Saved, SaveResult.NotFound -> onClose()
                                is SaveResult.Invalid -> serverError = result.error
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Zapisz przypomnienie") }
            }
        }
    }

    if (showDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateEpoch = java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Anuluj") } },
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = { minuteOfDay = state.hour * 60 + state.minute; showTime = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Anuluj") } },
            text = { TimePicker(state = state) },
        )
    }
}

/** Domyślnie: następna pełna godzina, co najmniej 30 min od teraz. */
private fun defaultStart(): java.time.LocalDateTime {
    val t = java.time.LocalDateTime.now().plusMinutes(30)
    return t.withMinute(0).withSecond(0).withNano(0).plusHours(1)
}
