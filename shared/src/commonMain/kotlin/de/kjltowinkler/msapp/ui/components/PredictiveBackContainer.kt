package de.kjltowinkler.msapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kjltowinkler.msapp.PlatformBackHandler
import de.kjltowinkler.msapp.PlatformPredictiveBackHandler
import de.kjltowinkler.msapp.PredictiveBackProgress
import kotlinx.coroutines.CancellationException

/**
 * Globaler CompositionLocal-Wert, der steuert, ob die intelligente „Zurück“-Geste
 * (Predictive Back Animationen) app-weit aktiviert ist.
 */
val LocalPredictiveBackEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

/**
 * Universeller Container für intelligente „Zurück“-Touchgesten und Animationen (Predictive Back),
 * basierend auf Googles Android-Richtlinien:
 * https://developer.android.com/guide/navigation/navigation-event/handle-back?hl=de
 *
 * Reagiert in Echtzeit auf die Wischgeste des Nutzers vom Bildschirmrand:
 * - Verkleinert den Inhalt proportional zum Wischfortschritt (Scale)
 * - Verschiebt den Inhalt leicht in Wischrichtung (abhängig von swipeEdge: links oder rechts)
 * - Rundet die Bildschirmecken während der Geste ab (Material 3 Corner Radius)
 * - Federt bei Abbruch der Geste flüssig an den Ausgangspunkt zurück (Spring Animation)
 * - Ruft bei Vollendung [onBack] auf
 *
 * @param modifier Der Modifier für den umschließenden Container
 * @param enabled Ob die Zurück-Geste aktuell abgefangen werden soll
 * @param animationEnabled Ob die visuelle Vorschau-Animation aktiv sein soll (Standard: [LocalPredictiveBackEnabled])
 * @param maxScaleReduction Maximale Skalierungsreduktion (Standard: 0.10f -> Skalierung bis 0.90f)
 * @param maxCornerRadius Maximaler Eckenradius bei vollem Fortschritt (Standard: 28.dp)
 * @param maxShiftDp Maximaler horizontaler Versatz in dp
 * @param onBack Callback bei vollständigem Abschluss der Geste
 * @param content Der Inhalt des Containers mit Zugriff auf den aktuellen [backProgress]
 */
@Composable
fun PredictiveBackContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animationEnabled: Boolean = LocalPredictiveBackEnabled.current,
    maxScaleReduction: Float = 0.10f,
    maxCornerRadius: Dp = 28.dp,
    maxShiftDp: Dp = 32.dp,
    onBack: () -> Unit,
    content: @Composable BoxScope.(backProgress: Float) -> Unit
) {
    if (!animationEnabled) {
        // Fallback: Wenn Gestenanimationen in den Einstellungen deaktiviert sind,
        // wird die Zurück-Aktion ohne visuelle Skalierung direkt ausgelöst.
        PlatformBackHandler(enabled = enabled, onBack = onBack)
        Box(modifier = modifier) {
            content(0f)
        }
        return
    }

    var rawProgress by remember { mutableFloatStateOf(0f) }
    var swipeEdge by remember { mutableIntStateOf(PredictiveBackProgress.EDGE_LEFT) }
    var isGestureInProgress by remember { mutableStateOf(false) }

    PlatformPredictiveBackHandler(enabled = enabled) { progressFlow ->
        isGestureInProgress = true
        try {
            progressFlow.collect { event ->
                rawProgress = event.progress
                swipeEdge = event.swipeEdge
            }
            // Geste erfolgreich bis zum Ende ausgeführt
            isGestureInProgress = false
            rawProgress = 0f
            onBack()
        } catch (_: CancellationException) {
            // Geste wurde vom Nutzer abgebrochen (zurück an den Rand gewischt)
            isGestureInProgress = false
            rawProgress = 0f
        }
    }

    // Während des Wischens folgen wir der Fingerposition unmittelbar (snap).
    // Bei Abbruch federt die Ansicht sanft per Spring zurück.
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (isGestureInProgress) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PredictiveBackProgress"
    )

    Box(
        modifier = modifier.graphicsLayer {
            if (animatedProgress > 0.001f) {
                val scale = 1f - (animatedProgress * maxScaleReduction)
                scaleX = scale
                scaleY = scale

                // Dynamischer Versatz abhängig von der Wischkante (links nach rechts vs. rechts nach links)
                val baseShiftPx = (size.width / 20f).coerceIn(16f * density, maxShiftDp.toPx())
                val shiftDirection = if (swipeEdge == PredictiveBackProgress.EDGE_RIGHT) -1f else 1f
                translationX = animatedProgress * baseShiftPx * shiftDirection

                clip = true
                shape = RoundedCornerShape(maxCornerRadius * animatedProgress)
            }
        }
    ) {
        content(animatedProgress)
    }
}

/**
 * Wendet den Predictive-Back-Animationsstil auf ein beliebiges Composable an,
 * wenn der aktuelle Fortschritt [progress] und die Wischkante [swipeEdge] übergeben werden.
 */
fun Modifier.predictiveBackTransform(
    progress: Float,
    swipeEdge: Int = PredictiveBackProgress.EDGE_LEFT,
    maxScaleReduction: Float = 0.10f,
    maxCornerRadius: Dp = 28.dp,
    maxShiftDp: Dp = 32.dp
): Modifier = this.graphicsLayer {
    if (progress > 0.001f) {
        val scale = 1f - (progress * maxScaleReduction)
        scaleX = scale
        scaleY = scale

        val baseShiftPx = (size.width / 20f).coerceIn(16f * density, maxShiftDp.toPx())
        val shiftDirection = if (swipeEdge == PredictiveBackProgress.EDGE_RIGHT) -1f else 1f
        translationX = progress * baseShiftPx * shiftDirection

        clip = true
        shape = RoundedCornerShape(maxCornerRadius * progress)
    }
}
