package de.kjltowinkler.msapp

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getCurrentFormattedDate(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "d. MMMM"
        locale = NSLocale(localeIdentifier = "de_DE")
    }
    return "Heute, ${formatter.stringFromDate(NSDate())}"
}