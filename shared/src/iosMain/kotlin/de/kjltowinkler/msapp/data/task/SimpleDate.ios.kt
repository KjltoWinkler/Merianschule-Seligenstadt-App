package de.kjltowinkler.msapp.data.task

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getTodayDate(): SimpleDate {
    val date = NSDate()
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = date
    )
    return SimpleDate(
        year = components.year.toInt(),
        monthNumber = components.month.toInt(),
        dayOfMonth = components.day.toInt()
    )
}

actual fun getCurrentTime(): SimpleTime {
    val date = NSDate()
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        NSCalendarUnitHour or NSCalendarUnitMinute,
        fromDate = date
    )
    return SimpleTime(
        hour = components.hour.toInt(),
        minute = components.minute.toInt()
    )
}

actual fun getCurrentEpochMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun parseDateTimeToEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    val calendar = NSCalendar.currentCalendar
    val components = platform.Foundation.NSDateComponents().apply {
        this.year = year.toLong()
        this.month = month.toLong()
        this.day = day.toLong()
        this.hour = hour.toLong()
        this.minute = minute.toLong()
        this.second = 0
    }
    val date = calendar.dateFromComponents(components)
    return date?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0L
}

