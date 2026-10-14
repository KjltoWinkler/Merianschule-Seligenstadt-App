package de.kjltowinkler.msapp

import android.os.Build

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getCurrentFormattedDate(): String {
    val formatter = SimpleDateFormat("d. MMMM", Locale.GERMAN)
    return "Heute, ${formatter.format(Date())}"
}