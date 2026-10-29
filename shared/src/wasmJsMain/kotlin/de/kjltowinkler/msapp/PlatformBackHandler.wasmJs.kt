package de.kjltowinkler.msapp

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Web Back Navigation wird vom Browser / NavController gesteuert
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<PredictiveBackProgress>) -> Unit
) {
    // Auf Web gibt es keine native Predictive Back Gesten-Animation
}
