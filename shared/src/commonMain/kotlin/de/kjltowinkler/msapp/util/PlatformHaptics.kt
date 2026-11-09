package de.kjltowinkler.msapp.util

expect object PlatformHaptics {
    /**
     * Löst ein besonders glückliches, befriedigendes und dynamisches haptisches Feedback
     * beim Erledigen/Abhaken einer Aufgabe aus (auf Pixel & modernen Android-Geräten mit
     * reicher Haptik-Komposition aus kurzem Pop + fröhlichem Folge-Tick, synchron zur Konfetti-Animation).
     */
    fun performTaskCompletedHaptic()
}
