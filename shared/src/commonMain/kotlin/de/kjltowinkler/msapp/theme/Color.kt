package de.kjltowinkler.msapp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Exakte Figma Werte aus schulapp.fig (onboarding#1)
val FigmaBackgroundDark = Color(0xFF141A16)
val FigmaOnBackgroundDark = Color(0xFFDFE9DE)

val FigmaPrimaryDark = Color(0xFF395A3C)
val FigmaOnPrimaryDark = Color(0xFFFFFFFF)
val FigmaPrimaryContainerDark = Color(0xFF395A3C)
val FigmaOnPrimaryContainerDark = Color(0xFFDFE9DE)

val FigmaButtonSecondaryDark = Color(0xFF272E26)
val FigmaMenuBackgroundDark = Color(0xFF202820)
val FigmaOutlineStrokeDark = Color(0xFFB1C5AE)

// Spezifische Farben für Textfelder und Links aus onboarding#nutzername & einmalpasswort
val FigmaLinkGreen = Color(0xFFD7FED5)
val FigmaInputBackgroundDark = Color(0xFF272C28)
val FigmaInputBorderDark = Color(0xFF79747E)
val FigmaPlaceholderDark = Color(0xFFAAADA6)
val FigmaAccountChipBackground = Color(0xFF2F402F)
val FigmaAccountChipText = Color(0xFFA1B59E)

// Fallback Scheme orientiert an den Figma Hex-Werten
val FigmaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD88C), // M3 Tone 80 Hellgrün: barrierefreier Kontrast für Links, Cursors & Dialog-Aktionen auf FigmaBackgroundDark
    onPrimary = Color(0xFF00390E),
    primaryContainer = FigmaPrimaryDark,
    onPrimaryContainer = FigmaOnPrimaryContainerDark,
    secondary = FigmaButtonSecondaryDark,
    onSecondary = FigmaOnBackgroundDark,
    secondaryContainer = FigmaAccountChipBackground,
    onSecondaryContainer = FigmaAccountChipText,
    background = FigmaBackgroundDark,
    onBackground = FigmaOnBackgroundDark,
    surface = FigmaBackgroundDark,
    onSurface = FigmaOnBackgroundDark,
    surfaceVariant = FigmaMenuBackgroundDark,
    onSurfaceVariant = FigmaOnBackgroundDark.copy(alpha = 0.7f),
    surfaceContainer = FigmaMenuBackgroundDark,
    surfaceContainerHigh = FigmaButtonSecondaryDark,
    surfaceContainerHighest = Color(0xFF2C382D),
    outline = FigmaOutlineStrokeDark,
    outlineVariant = FigmaOutlineStrokeDark.copy(alpha = 0.4f)
)

val FigmaLightColorScheme = lightColorScheme(
    primary = Color(0xFF395A3C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7EBC6),
    onPrimaryContainer = Color(0xFF002209),
    secondary = Color(0xFF4F6352),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E7D4),
    onSecondaryContainer = Color(0xFF1E3A21),
    background = Color(0xFFF7FBF4),
    onBackground = Color(0xFF141A16),
    surface = Color(0xFFF7FBF4),
    onSurface = Color(0xFF141A16),
    surfaceContainer = Color(0xFFE8EFE5),
    surfaceContainerHighest = Color(0xFFE2ECE0)
)
