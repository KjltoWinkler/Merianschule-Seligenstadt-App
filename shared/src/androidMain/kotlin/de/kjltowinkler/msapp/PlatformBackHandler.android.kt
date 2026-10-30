package de.kjltowinkler.msapp
 
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    PredictiveBackHandler(enabled = enabled) { progress ->
        try {
            progress.collect { /* Gesten-Fortschritt beobachten */ }
            onBack()
        } catch (_: CancellationException) {
            // Geste wurde abgebrochen
        }
    }
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<PredictiveBackProgress>) -> Unit
) {
    PredictiveBackHandler(enabled = enabled) { progressFlow ->
        val mappedFlow: Flow<PredictiveBackProgress> = progressFlow.map { event ->
            PredictiveBackProgress(
                progress = event.progress,
                touchX = event.touchX,
                touchY = event.touchY,
                swipeEdge = event.swipeEdge
            )
        }
        onBack(mappedFlow)
    }
}

@Composable
actual fun PlatformNavigationBackHandler(navController: androidx.navigation.NavHostController) {
    // Android NavHost kümmert sich nativ über den OnBackPressedDispatcher um Back-Gesten
}

actual fun androidx.compose.ui.Modifier.platformEdgeBackGesture(): androidx.compose.ui.Modifier = this
