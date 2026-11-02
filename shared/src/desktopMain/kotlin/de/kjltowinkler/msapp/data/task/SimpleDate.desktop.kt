package de.kjltowinkler.msapp.data.task

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

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

actual fun parseDateTimeToEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    val localDateTime = LocalDateTime.of(year, month, day, hour, minute, 0)
    return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
