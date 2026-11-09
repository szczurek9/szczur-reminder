package com.szczurek9.reminder.settings

data class AppSettings(
    /** Główny przełącznik powiadomień aplikacji. */
    val notificationsEnabled: Boolean = true,
    /** Czas prezentacji na pasku Media Bar (3–10 s). Nie dotyczy zwykłych powiadomień Androida. */
    val barDisplaySeconds: Int = DEFAULT_BAR_SECONDS,
    /** Domyślne wyprzedzenie w nowym przypomnieniu (minuty; 0 = o wybranej godzinie). */
    val defaultLeadMinutes: Int = 0,
    /** Interwały "Przypomnij później" (minuty), posortowane rosnąco, co najmniej jeden. */
    val snoozeMinutes: List<Int> = DEFAULT_SNOOZE,
    /** Użytkownik pozwala na korzystanie z Media Bar (gdy dostępny). */
    val integrationEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_BAR_SECONDS = 5
        val BAR_SECONDS_RANGE = 3..10
        val DEFAULT_SNOOZE = listOf(5, 15, 60)
        val SNOOZE_CHOICES = listOf(5, 10, 15, 30, 60, 120, 1440)
    }
}
