package com.szczurek9.reminder.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.reminderStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.reminderStore

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setNotificationsEnabled(v: Boolean) = store.edit { it[NOTIFICATIONS] = v }
    suspend fun setBarSeconds(v: Int) = store.edit { it[BAR_SECONDS] = v.coerceIn(AppSettings.BAR_SECONDS_RANGE) }
    suspend fun setDefaultLead(v: Int) = store.edit { it[DEFAULT_LEAD] = v }
    suspend fun setIntegrationEnabled(v: Boolean) = store.edit { it[INTEGRATION] = v }

    /** Zapisuje interwały; pusta lista jest odrzucana (zawsze zostaje co najmniej jeden). */
    suspend fun setSnoozeMinutes(values: List<Int>) {
        val clean = values.filter { it in AppSettings.SNOOZE_CHOICES }.distinct().sorted()
        if (clean.isEmpty()) return
        store.edit { it[SNOOZE] = clean.joinToString(",") }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        val snooze = this[SNOOZE]
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in AppSettings.SNOOZE_CHOICES }
            ?.distinct()?.sorted()
            ?.takeIf { it.isNotEmpty() }
            ?: d.snoozeMinutes
        return AppSettings(
            notificationsEnabled = this[NOTIFICATIONS] ?: d.notificationsEnabled,
            barDisplaySeconds = (this[BAR_SECONDS] ?: d.barDisplaySeconds).coerceIn(AppSettings.BAR_SECONDS_RANGE),
            defaultLeadMinutes = this[DEFAULT_LEAD] ?: d.defaultLeadMinutes,
            snoozeMinutes = snooze,
            integrationEnabled = this[INTEGRATION] ?: d.integrationEnabled,
        )
    }

    private companion object {
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val BAR_SECONDS = intPreferencesKey("bar_display_seconds")
        val DEFAULT_LEAD = intPreferencesKey("default_lead_minutes")
        val SNOOZE = stringPreferencesKey("snooze_minutes")
        val INTEGRATION = booleanPreferencesKey("integration_enabled")
    }
}
