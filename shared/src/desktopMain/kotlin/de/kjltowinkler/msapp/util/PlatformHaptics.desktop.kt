package de.kjltowinkler.msapp.util

actual object PlatformHaptics {
    actual fun performTaskCompletedHaptic() {
        // Desktop besitzt üblicherweise keine Vibrationsmotoren
    }
}
