package de.kjltowinkler.msapp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.google_sans_flex
import org.jetbrains.compose.resources.Font

@Composable
private fun googleSansRoundedFont(weight: FontWeight, axisWeight: Int) = Font(
    resource = Res.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(axisWeight),
        FontVariation.Setting("ROND", 100f)
    )
)

@Composable
fun getQuicksandFontFamily(): FontFamily {
    return getGoogleSansFlexFontFamily()
}

@Composable
fun getGoogleSansFlexFontFamily(): FontFamily {
    return FontFamily(
        googleSansRoundedFont(FontWeight.Light, axisWeight = 250),
        googleSansRoundedFont(FontWeight.Normal, axisWeight = 330),
        googleSansRoundedFont(FontWeight.Medium, axisWeight = 420),
        googleSansRoundedFont(FontWeight.SemiBold, axisWeight = 500),
        googleSansRoundedFont(FontWeight.Bold, axisWeight = 600)
    )
}

@Composable
fun getAppTypography(): Typography {
    val googleSans = getGoogleSansFlexFontFamily()

    return Typography(
        displayLarge = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 57.sp,
            lineHeight = 64.sp
        ),
        displayMedium = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Normal,
            fontSynthesis = FontSynthesis.None,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = googleSans,
            fontWeight = FontWeight.Medium,
            fontSynthesis = FontSynthesis.None,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )
}
