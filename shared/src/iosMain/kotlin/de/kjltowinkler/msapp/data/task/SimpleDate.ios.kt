package de.kjltowinkler.msapp.data.task

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
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

actual fun getCurrentEpochMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
