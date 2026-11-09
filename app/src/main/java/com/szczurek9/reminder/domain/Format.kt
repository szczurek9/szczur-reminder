package com.szczurek9.reminder.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PL = Locale.forLanguageTag("pl-PL")
private val DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", PL)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", PL)

fun formatDate(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Dziś"
    today.plusDays(1) -> "Jutro"
    today.minusDays(1) -> "Wczoraj"
    else -> date.format(DATE_FMT)
}

fun formatTime(time: LocalTime): String = time.format(TIME_FMT)

/** Np. "Dziś, 15:00" – etykieta przesyłana też na pasek Media Bar. */
fun formatEventLabel(date: LocalDate, time: LocalTime, today: LocalDate = LocalDate.now()): String =
    "${formatDate(date, today)}, ${formatTime(time)}"

fun formatMinutes(minutes: Int): String = when {
    minutes % 1440 == 0 -> if (minutes / 1440 == 1) "1 dzień" else "${minutes / 1440} dni"
    minutes % 60 == 0 -> if (minutes / 60 == 1) "1 godz." else "${minutes / 60} godz."
    else -> "$minutes min"
}
