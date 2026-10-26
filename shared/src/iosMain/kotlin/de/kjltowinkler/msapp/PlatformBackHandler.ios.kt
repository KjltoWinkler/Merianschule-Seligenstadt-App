package de.kjltowinkler.msapp

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Auf iOS gibt es kein systemweites Zurück-Tasten-Event
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<PredictiveBackProgress>) -> Unit
) {
    // Auf iOS gibt es kein systemweites Zurück-Tasten-Event
}
