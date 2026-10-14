package de.kjltowinkler.msapp

import androidx.compose.runtime.Composable

/**
 * Erlaubt das Abfangen von Zurück-Gesten auf Plattformen mit System-Zurück-Unterstützung (Android).
 * Ist [enabled] false (z.B. auf dem Root-Screen), wird die Zurück-Geste nicht abgefangen,
 * sodass das System native Systemanimationen (Back-to-Home, Cross-Activity, Cross-Task) ausführen kann.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
