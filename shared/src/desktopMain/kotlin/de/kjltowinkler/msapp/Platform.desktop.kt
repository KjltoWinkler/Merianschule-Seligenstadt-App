package de.kjltowinkler.msapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopPlatform : Platform {
    override val name: String = "Desktop (${System.getProperty("os.name")})"
}

actual fun getPlatform(): Platform = DesktopPlatform()

actual fun getCurrentFormattedDate(): String {
    val formatter = SimpleDateFormat("d. MMMM", Locale.GERMAN)
    return "Heute, ${formatter.format(Date())}"
}
