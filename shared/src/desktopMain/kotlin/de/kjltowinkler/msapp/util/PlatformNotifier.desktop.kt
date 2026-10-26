package de.kjltowinkler.msapp.util

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

actual object PlatformNotifier {
    actual fun sendNotification(title: String, summary: String, details: List<String>) {
        try {
            if (SystemTray.isSupported()) {
                val tray = SystemTray.getSystemTray()
                val image = Toolkit.getDefaultToolkit().createImage("")
                val trayIcon = TrayIcon(image, "Merianschule App")
                trayIcon.isImageAutoSize = true
                tray.add(trayIcon)
                val fullMessage = if (details.isNotEmpty()) {
                    "$summary\n" + details.joinToString("\n")
                } else {
                    summary
                }
                trayIcon.displayMessage(title, fullMessage, TrayIcon.MessageType.INFO)
            } else {
                println("[Desktop-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
            }
        } catch (_: Throwable) {
            println("[Desktop-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
        }
    }
}
