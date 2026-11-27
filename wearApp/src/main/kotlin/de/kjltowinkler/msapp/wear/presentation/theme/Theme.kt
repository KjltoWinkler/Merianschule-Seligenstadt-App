package de.kjltowinkler.msapp.wear.presentation.theme

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme
import de.kjltowinkler.msapp.wear.R

private fun googleSansRoundedWearFont(weight: FontWeight, axisWeight: Int) = Font(
    resId = R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(axisWeight),
        FontVariation.Setting("ROND", 100f)
    )
)

val GoogleSansRounded = FontFamily(
    googleSansRoundedWearFont(FontWeight.Light, axisWeight = 250),
    googleSansRoundedWearFont(FontWeight.Normal, axisWeight = 340),
    googleSansRoundedWearFont(FontWeight.Medium, axisWeight = 460),
    googleSansRoundedWearFont(FontWeight.SemiBold, axisWeight = 600),
    googleSansRoundedWearFont(FontWeight.Bold, axisWeight = 700)
)

// Merianschule Material You Green Farbschema für Wear OS (AMOLED-optimiert)
val WearMaterialYouGreenScheme = ColorScheme(
    primary = Color(0xFFA8DAB5),
    primaryDim = Color(0xFF8BC49B),
    primaryContainer = Color(0xFF27432D),
    onPrimary = Color(0xFF0D3818),
    onPrimaryContainer = Color(0xFFC7EBC6),
    secondary = Color(0xFFB8CCB8),
    secondaryDim = Color(0xFF9EAF9E),
    secondaryContainer = Color(0xFF354435),
    onSecondary = Color(0xFF233424),
    onSecondaryContainer = Color(0xFFD4E8D4),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE2E9E0),
    surfaceContainerLow = Color(0xFF181F18),
    surfaceContainer = Color(0xFF222B22),
    surfaceContainerHigh = Color(0xFF2C372C),
    onSurface = Color(0xFFE2E9E0),
    onSurfaceVariant = Color(0xFFA2ADA1),
    outline = Color(0xFF6F7A6E),
    outlineVariant = Color(0xFF384338)
)

/**
 * Ermittelt das dynamische Material You Farbschema des Systems (Pixel Watch) oder fällt
 * auf die Markenfarben der Merianschule zurück.
 */
fun getDynamicWearColorScheme(context: Context): ColorScheme {
    // 1. Wear OS 5+ dynamisches Farbschema aus Watchface
    try {
        val wearDynamic = dynamicColorScheme(context)
        if (wearDynamic != null) {
            return wearDynamic
        }
    } catch (_: Throwable) {}

    // 2. Android 12+ (API 31+) Material You System-Monet-Farben
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val res = context.resources
            val theme = context.theme

            val primary = Color(res.getColor(android.R.color.system_accent1_200, theme))
            val onPrimary = Color(res.getColor(android.R.color.system_accent1_800, theme))
            val primaryContainer = Color(res.getColor(android.R.color.system_accent1_700, theme))
            val onPrimaryContainer = Color(res.getColor(android.R.color.system_accent1_100, theme))

            val secondary = Color(res.getColor(android.R.color.system_accent2_200, theme))
            val onSecondary = Color(res.getColor(android.R.color.system_accent2_800, theme))
            val secondaryContainer = Color(res.getColor(android.R.color.system_accent2_700, theme))
            val onSecondaryContainer = Color(res.getColor(android.R.color.system_accent2_100, theme))

            val surfaceContainer = Color(res.getColor(android.R.color.system_neutral2_800, theme))
            val onSurface = Color(res.getColor(android.R.color.system_neutral1_100, theme))
            val onSurfaceVariant = Color(res.getColor(android.R.color.system_neutral2_200, theme))
            val outlineVariant = Color(res.getColor(android.R.color.system_neutral2_700, theme))

            return ColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                background = Color(0xFF000000),
                onBackground = onSurface,
                surfaceContainer = surfaceContainer,
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                outlineVariant = outlineVariant
            )
        } catch (_: Throwable) {}
    }

    // 3. Fallback: Merianschule Material You Green
    return WearMaterialYouGreenScheme
}

/**
 * Erstellt die Wear OS Typografie mit Google Sans Rounded (Standardgewicht Normal, schlank und nicht fett).
 */
@Composable
fun getWearTypography(): Typography {
    return Typography(
        defaultFontFamily = GoogleSansRounded,
        displayLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 40.sp),
        displayMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 34.sp),
        displaySmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 30.sp),
        titleLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 14.sp),
        labelLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 15.sp),
        labelMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 11.sp),
        bodyLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 12.sp),
        bodyExtraSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 10.sp),
        numeralExtraLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 52.sp),
        numeralLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 44.sp),
        numeralMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 38.sp),
        numeralSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 30.sp),
        numeralExtraSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None, fontSize = 24.sp),
        arcLarge = CurvedTextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 15.sp),
        arcMedium = CurvedTextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 13.sp),
        arcSmall = CurvedTextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 11.sp)
    )
}

@Composable
fun WearAppTheme(
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember(context, useDynamicColors) {
        if (useDynamicColors) {
            getDynamicWearColorScheme(context)
        } else {
            WearMaterialYouGreenScheme
        }
    }
    val typography = getWearTypography()

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.wear.compose.material3.LocalTextStyle provides typography.bodyMedium
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
