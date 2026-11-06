package de.kjltowinkler.msapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

@Composable
expect fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
)

/**
 * Container-Farbe speziell für Content-Cards innerhalb von Screens
 * (z. B. Aufgaben-Liste, Vertretungsplan-Einträge, Stundenplan-Karten).
 *
 * Im Dark Mode wird eine nuancierte Zwischenstufe (ca. Tone 6.5–7) zwischen
 * surfaceContainerLowest (Tone 4) und surfaceContainerLow (Tone 10) gewählt:
 * spürbar dunkler als normale Container, aber mit echter dynamischer Tönung und nicht pechschwarz.
 * Navbar, Tabs, BottomSheets und Dialoge behalten ihre Standard-Container-Farben.
 * Im Light Mode wird die reguläre surfaceContainer-Farbe verwendet.
 */
@Composable
fun screenCardContainerColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surfaceContainerLow,
            0.7f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
}

/**
 * Spezielles Theme für die Desktop- und Webversion:
 * Verwendet garantiert die exakten Figma-Farben OHNE Material You (Dynamic Colors).
 */
@Composable
fun DesktopTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DesktopDarkColorScheme,
        typography = getAppTypography(),
        content = content
    )
}

