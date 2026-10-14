package de.kjltowinkler.msapp.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val colorScheme = when {
        !isInspection && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                if (darkTheme) FigmaDarkColorScheme else FigmaLightColorScheme
            }
        }
        darkTheme -> FigmaDarkColorScheme
        else -> FigmaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getAppTypography(),
        content = content
    )
}
