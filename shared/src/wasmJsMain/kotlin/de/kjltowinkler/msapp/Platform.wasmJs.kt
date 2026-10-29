package de.kjltowinkler.msapp

import de.kjltowinkler.msapp.data.task.getTodayDate

class WasmPlatform : Platform {
    override val name: String = "Web"
}

actual fun getPlatform(): Platform = WasmPlatform()

private val monthsGerman = listOf(
    "", "Januar", "Februar", "März", "April", "Mai", "Juni",
    "Juli", "August", "September", "Oktober", "November", "Dezember"
)

actual fun getCurrentFormattedDate(): String {
    val today = getTodayDate()
    val monthName = monthsGerman.getOrElse(today.monthNumber) { "" }
    return "Heute, ${today.dayOfMonth}. $monthName"
}
