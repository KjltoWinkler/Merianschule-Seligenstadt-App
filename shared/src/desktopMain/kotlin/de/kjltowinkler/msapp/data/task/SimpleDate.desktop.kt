package de.kjltowinkler.msapp.data.task

import java.time.LocalDate

actual fun getTodayDate(): SimpleDate {
    val now = LocalDate.now()
    return SimpleDate(
        year = now.year,
        monthNumber = now.monthValue,
        dayOfMonth = now.dayOfMonth
    )
}

actual fun getCurrentEpochMillis(): Long = System.currentTimeMillis()
