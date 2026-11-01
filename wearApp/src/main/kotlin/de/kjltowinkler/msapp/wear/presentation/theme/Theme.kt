package de.kjltowinkler.msapp.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme
import de.kjltowinkler.msapp.theme.getGoogleSansFlexFontFamily

// Fallback M3 Expressive Farbpalette für Wear OS (Schwarz-Hintergrund für AMOLED)
val WearPrimary = Color(0xFFD0BCFF)
val WearOnPrimary = Color(0xFF381E72)
val WearPrimaryContainer = Color(0xFF4F378B)
val WearOnPrimaryContainer = Color(0xFFEADDFF)

val WearSecondary = Color(0xFFCCC2DC)
val WearOnSecondary = Color(0xFF332D41)
val WearSecondaryContainer = Color(0xFF4A4458)
val WearOnSecondaryContainer = Color(0xFFE8DEF8)

val WearBackground = Color(0xFF000000)
val WearOnBackground = Color(0xFFE6E1E5)
val WearSurfaceContainer = Color(0xFF1D1B20)
val WearOnSurface = Color(0xFFE6E1E5)

val WearError = Color(0xFFF2B8B5)
val WearOnError = Color(0xFF601410)
val WearErrorContainer = Color(0xFF8C1D18)
val WearOnErrorContainer = Color(0xFFF9DEDC)

val WearColorScheme = ColorScheme(
    primary = WearPrimary,
    onPrimary = WearOnPrimary,
    primaryContainer = WearPrimaryContainer,
    onPrimaryContainer = WearOnPrimaryContainer,
    secondary = WearSecondary,
    onSecondary = WearOnSecondary,
    secondaryContainer = WearSecondaryContainer,
    onSecondaryContainer = WearOnSecondaryContainer,
    background = WearBackground,
    onBackground = WearOnBackground,
    surfaceContainer = WearSurfaceContainer,
    onSurface = WearOnSurface,
    error = WearError,
    onError = WearOnError,
    errorContainer = WearErrorContainer,
    onErrorContainer = WearOnErrorContainer
)

/**
 * Erstellt die Wear OS Typografie mit Google Sans Flex (in Medium für Onboarding-Titel & Fließtexte, exakt wie im Figma Design).
 */
@Composable
fun getWearTypography(): Typography {
    val googleSans = getGoogleSansFlexFontFamily()

    return Typography(
        defaultFontFamily = googleSans,
        displayLarge = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 40.sp),
        displayMedium = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 34.sp),
        displaySmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 30.sp),
        titleLarge = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 14.sp),
        labelLarge = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 15.sp),
        labelMedium = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 11.sp),
        bodyLarge = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 12.sp),
        bodyExtraSmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 10.sp),
        numeralExtraLarge = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 52.sp),
        numeralLarge = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 44.sp),
        numeralMedium = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 38.sp),
        numeralSmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 30.sp),
        numeralExtraSmall = TextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None, fontSize = 24.sp),
        arcLarge = CurvedTextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSize = 15.sp),
        arcMedium = CurvedTextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        arcSmall = CurvedTextStyle(fontFamily = googleSans, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    )
}

@Composable
fun WearAppTheme(
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicScheme = if (useDynamicColors) dynamicColorScheme(context) else null
    val colorScheme = dynamicScheme ?: WearColorScheme
    val typography = getWearTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
