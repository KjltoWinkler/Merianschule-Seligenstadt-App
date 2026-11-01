package de.kjltowinkler.msapp.data.task

import java.util.Calendar

actual fun getTodayDate(): SimpleDate {
    val cal = Calendar.getInstance()
    return SimpleDate(
        year = cal.get(Calendar.YEAR),
        monthNumber = cal.get(Calendar.MONTH) + 1,
        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    )
}

actual fun getCurrentTime(): SimpleTime {
    val cal = Calendar.getInstance()
    return SimpleTime(
        hour = cal.get(Calendar.HOUR_OF_DAY),
        minute = cal.get(Calendar.MINUTE)
    )
}

actual fun getCurrentEpochMillis(): Long = System.currentTimeMillis()
