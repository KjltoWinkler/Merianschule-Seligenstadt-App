package de.kjltowinkler.msapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

import androidx.compose.material3.MaterialTheme

@Composable
expect fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
)

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

