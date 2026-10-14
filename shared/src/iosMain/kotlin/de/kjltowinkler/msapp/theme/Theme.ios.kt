package de.kjltowinkler.msapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FigmaDarkColorScheme else FigmaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getAppTypography(),
        content = content
    )
}
