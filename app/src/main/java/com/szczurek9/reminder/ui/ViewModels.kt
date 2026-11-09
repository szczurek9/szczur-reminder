package com.szczurek9.reminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.szczurek9.reminder.ReminderApp
import com.szczurek9.reminder.domain.LeadTime
import com.szczurek9.reminder.domain.Reminder
import com.szczurek9.reminder.domain.SaveResult
import com.szczurek9.reminder.integration.MediaBarStatus
import com.szczurek9.reminder.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ReminderApp

    /** null = jeszcze się wczytuje. */
    val active: StateFlow<List<Reminder>?> = app.repository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val done: StateFlow<List<Reminder>?> = app.repository.observeDone()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<AppSettings> = app.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun observe(id: Long): Flow<Reminder?> = app.repository.observe(id)
    suspend fun load(id: Long): Reminder? = app.repository.get(id)

    fun markDone(id: Long) {
        viewModelScope.launch { app.engine.markDone(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { app.engine.delete(id) }
    }

    fun snooze(id: Long, minutes: Int) {
        viewModelScope.launch { app.engine.snooze(id, minutes) }
    }

    fun save(
        id: Long?,
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime,
        leadMinutes: Int,
        onResult: (SaveResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = if (id == null) {
                app.manager.create(title, description, date, time, leadMinutes)
            } else {
                app.manager.update(id, title, description, date, time, leadMinutes).also {
                    // Po edycji stare powiadomienie / pasek dotyczy już nieaktualnych danych.
                    if (it is SaveResult.Saved) app.engine.clearPresentation(id)
                }
            }
            onResult(result)
        }
    }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ReminderApp

    val settings: StateFlow<AppSettings> = app.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _mediaBar = MutableStateFlow<MediaBarStatus?>(null)
    val mediaBar: StateFlow<MediaBarStatus?> = _mediaBar

    fun canScheduleExact(): Boolean = app.scheduler.canScheduleExact()

    fun refreshMediaBar() {
        viewModelScope.launch { _mediaBar.value = app.mediaBar.probe() }
    }

    fun setNotificationsEnabled(v: Boolean) { viewModelScope.launch { app.settings.setNotificationsEnabled(v) } }
    fun setBarSeconds(v: Int) { viewModelScope.launch { app.settings.setBarSeconds(v) } }
    fun setDefaultLead(v: LeadTime) { viewModelScope.launch { app.settings.setDefaultLead(v.minutes) } }
    fun setSnoozeMinutes(v: List<Int>) { viewModelScope.launch { app.settings.setSnoozeMinutes(v) } }
    fun setIntegrationEnabled(v: Boolean) {
        viewModelScope.launch {
            app.settings.setIntegrationEnabled(v)
            if (v) _mediaBar.value = app.mediaBar.probe()
        }
    }
}
