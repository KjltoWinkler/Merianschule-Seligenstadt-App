package de.kjltowinkler.msapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow

/**
 * Gesten-Fortschrittsdaten für eine vorausschauende Zurück-Geste (Predictive Back Gesture).
 * @param progress Fortschritt der Geste (0.0f bis 1.0f)
 * @param touchX X-Koordinate des Berührungspunkts
 * @param touchY Y-Koordinate des Berührungspunkts
 * @param swipeEdge Kante, von der gewischt wurde (0 = links, 1 = rechts)
 */
data class PredictiveBackProgress(
    val progress: Float = 0f,
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val swipeEdge: Int = 0
)

/**
 * Erlaubt das Abfangen von Zurück-Gesten auf Plattformen mit System-Zurück-Unterstützung (Android/Web).
 * Nutzt auf Android intern die moderne Predictive-Back-API (PredictiveBackHandler),
 * sodass vorausschauende Wischgesten nahtlos unterstützt werden.
 * Ist [enabled] false (z.B. auf dem Root-Screen), wird die Zurück-Geste nicht abgefangen,
 * sodass das System native Systemanimationen (Back-to-Home, Cross-Activity, Cross-Task) ausführen kann.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * Erlaubt das Abfangen von Zurück-Gesten mit direktem Zugriff auf den Gesten-Fortschritt
 * (0.0f bis 1.0f) für flüssige Echtzeit-Animationen (z. B. Skalieren, Verschieben, Eckenabrundung).
 */
@Composable
expect fun PlatformPredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (progress: Flow<PredictiveBackProgress>) -> Unit
)

/**
 * Synchronisiert den NavController auf Plattformen wie Web mit der Browser-History und Zurück-Gesten.
 */
@Composable
expect fun PlatformNavigationBackHandler(navController: NavHostController)

/**
 * Registriert Touch-Wischgesten von den Bildschirmrändern (links/rechts) für Zurück-Navigation.
 */
expect fun Modifier.platformEdgeBackGesture(): Modifier

