package de.kjltowinkler.msapp.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val rawContext = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val rawConfig = LocalConfiguration.current

    // Capping / moderating excessive system font-weight adjustment:
    // When "Bold Text" is toggled in Android Accessibility Settings, Android pushes
    // fontWeightAdjustment = +300. This turns Normal (400) to Bold (700) and Bold (700)
    // to UltraBlack (1000) with synthetic distortion.
    // Moderating it to +100 ensures the font is clearly, beautifully bold without
    // distorting character shapes, exactly like native Google Workspace apps.
    val (context, configuration) = remember(rawContext, rawConfig) {
        if (!isInspection && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && rawConfig.fontWeightAdjustment > 100) {
            val adjustedConfig = android.content.res.Configuration(rawConfig).apply {
                fontWeightAdjustment = 100
            }
            val adjustedCtx = rawContext.createConfigurationContext(adjustedConfig)
            adjustedCtx to adjustedConfig
        } else {
            rawContext to rawConfig
        }
    }

    val colorScheme = when {
        !isInspection && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                if (darkTheme) MaterialYouDarkColorScheme else MaterialYouLightColorScheme
            }
        }
        dynamicColor -> if (darkTheme) MaterialYouDarkColorScheme else MaterialYouLightColorScheme
        darkTheme -> FigmaDarkColorScheme
        else -> FigmaLightColorScheme
    }

    val typography = getAppTypography()

    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides configuration,
        androidx.compose.material3.LocalTextStyle provides typography.bodyMedium
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
