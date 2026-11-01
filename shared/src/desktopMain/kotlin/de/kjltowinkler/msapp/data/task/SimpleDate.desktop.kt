package de.kjltowinkler.msapp.data.task

import java.time.LocalDate
import java.time.LocalTime

actual fun getTodayDate(): SimpleDate {
    val now = LocalDate.now()
    return SimpleDate(
        year = now.year,
        monthNumber = now.monthValue,
        dayOfMonth = now.dayOfMonth
    )
}

actual fun getCurrentTime(): SimpleTime {
    val now = LocalTime.now()
    return SimpleTime(
        hour = now.hour,
        minute = now.minute
    )
}

actual fun getCurrentEpochMillis(): Long = System.currentTimeMillis()
