package de.kjltowinkler.msapp.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor) {
        if (darkTheme) MaterialYouDarkColorScheme else MaterialYouLightColorScheme
    } else {
        if (darkTheme) FigmaDarkColorScheme else FigmaLightColorScheme
    }

    val typography = getAppTypography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides typography.bodyMedium
        ) {
            content()
        }
    }
}
