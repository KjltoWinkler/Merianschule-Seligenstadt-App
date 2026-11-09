package de.kjltowinkler.msapp.util

actual object PlatformHaptics {
    actual fun performTaskCompletedHaptic() {
        // iOS Taptic Engine Fallback
    }
}
