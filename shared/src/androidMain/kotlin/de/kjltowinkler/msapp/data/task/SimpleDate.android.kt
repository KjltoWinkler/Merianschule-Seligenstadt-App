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

actual fun parseDateTimeToEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, day)
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
