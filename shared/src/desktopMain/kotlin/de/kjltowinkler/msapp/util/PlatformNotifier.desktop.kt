package de.kjltowinkler.msapp.util

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

actual object PlatformNotifier {
    actual fun sendNotification(title: String, summary: String, details: List<String>, entryId: String?) {
        try {
            if (SystemTray.isSupported()) {
                val tray = SystemTray.getSystemTray()
                val image = Toolkit.getDefaultToolkit().createImage("")
                val trayIcon = TrayIcon(image, "Merianschule App")
                trayIcon.isImageAutoSize = true
                trayIcon.addActionListener {
                    NavigationEvents.emit("substitution", entryId)
                }
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

    actual fun sendTaskNotification(title: String, summary: String, details: List<String>, taskId: String?) {
        try {
            if (SystemTray.isSupported()) {
                val tray = SystemTray.getSystemTray()
                val image = Toolkit.getDefaultToolkit().createImage("")
                val trayIcon = TrayIcon(image, "Merianschule App")
                trayIcon.isImageAutoSize = true
                trayIcon.addActionListener {
                    NavigationEvents.emit("tasks", taskId)
                }
                tray.add(trayIcon)
                val fullMessage = if (details.isNotEmpty()) {
                    "$summary\n" + details.joinToString("\n")
                } else {
                    summary
                }
                trayIcon.displayMessage(title, fullMessage, TrayIcon.MessageType.INFO)
            } else {
                println("[Desktop-Aufgaben-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
            }
        } catch (_: Throwable) {
            println("[Desktop-Aufgaben-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
        }
    }

    actual fun sendUpdateNotification(title: String, summary: String, details: List<String>): Boolean {
        sendNotification(title, summary, details, null)
        return true
    }
}

