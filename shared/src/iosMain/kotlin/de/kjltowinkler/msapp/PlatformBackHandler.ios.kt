package de.kjltowinkler.msapp

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Auf iOS gibt es kein systemweites Zurück-Tasten-Event
}
