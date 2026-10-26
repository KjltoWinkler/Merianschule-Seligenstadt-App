package de.kjltowinkler.msapp

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Auf Desktop gibt es keinen nativen System-Back-Button/Geste wie bei Android
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<PredictiveBackProgress>) -> Unit
) {
    // Auf Desktop gibt es keinen nativen System-Back-Button/Geste wie bei Android
}
