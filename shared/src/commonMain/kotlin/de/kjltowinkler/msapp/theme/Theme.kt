package de.kjltowinkler.msapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.graphics.Color
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
 * Im Dark Mode wird der dunklere Material-You-Ton (surfaceContainerLowest, Tone 4) verwendet,
 * während Navbar, Tabs, BottomSheets und Dialoge ihre Standard-Container-Farben behalten.
 * Im Light Mode wird die reguläre surfaceContainer-Farbe verwendet.
 */
@Composable
fun screenCardContainerColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLowest
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

