package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Moderner Lade-Indikator entsprechend Material 3 Expressive.
 * Verwendet standardmäßig den neuen CircularWavyProgressIndicator.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    CircularWavyProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        trackColor = trackColor
    )
}

/**
 * Standardmäßiger zirkulärer Lade-Indikator (Material 3).
 */
@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        trackColor = trackColor
    )
}

/**
 * Linearer wellenförmiger Ladebalken (z. B. am oberen Rand einer Seite während des Ladens).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLinearLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    LinearWavyProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        color = color,
        trackColor = trackColor
    )
}

/**
 * Standardmäßiger linearer Ladebalken (Material 3).
 */
@Composable
fun AppLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        color = color,
        trackColor = trackColor
    )
}

/**
 * Zentrierter Lade-Container für Seiten oder Abschnitte.
 */
@Composable
fun AppLoadingView(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        AppLoadingIndicator()
    }
}

/**
 * Vollflächiger oder containerfüllender Lade-Overlay-Screen ohne Verzögerung oder Fade-Effekt.
 */
@Composable
fun AppLoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator()
            }
        }
    }
}
