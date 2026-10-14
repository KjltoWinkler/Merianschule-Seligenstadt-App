package de.kjltowinkler.msapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Liefert das aktuelle Datum formatiert im Stil "Heute, 13. Oktober" (bzw. Wochentag/Datum).
 */
expect fun getCurrentFormattedDate(): String