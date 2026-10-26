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

// --- Exakte Farben für die Desktop- und Webversion (aus FigmaDesigns/desktop/vertretungsplan#alle.png) ---
// Bei Desktop/Web wird KEIN Material You verwendet!
val DesktopBackground = Color(0xFF111114)
val DesktopCardBackground = Color(0xFF0B0B0D)
val DesktopAccentBlue = Color(0xFFA9CAE8)
val DesktopOnAccentBlue = Color(0xFF0B0B0D)
val DesktopTextPrimary = Color(0xFFFFFFFF)
val DesktopTextSecondary = Color(0xFFDFE9DE)
val DesktopLink = Color(0xFFA9CAE8)
val DesktopInputBorder = Color(0xFF79747E)
val DesktopInputPlaceholder = Color(0xFFAAADA6)
val DesktopDecorativeStroke = Color(0xFF41709C)
val DesktopAccountChipBackground = Color(0xFF004B76)
val DesktopAccountChipContent = Color(0xFFDFE9DE)

val DesktopDarkColorScheme = darkColorScheme(
    primary = DesktopAccentBlue,
    onPrimary = DesktopOnAccentBlue,
    primaryContainer = DesktopAccentBlue,
    onPrimaryContainer = DesktopOnAccentBlue,
    secondary = DesktopAccentBlue,
    onSecondary = DesktopOnAccentBlue,
    secondaryContainer = DesktopAccountChipBackground,
    onSecondaryContainer = DesktopAccountChipContent,
    background = DesktopBackground,
    onBackground = DesktopTextPrimary,
    surface = DesktopCardBackground,
    onSurface = DesktopTextPrimary,
    surfaceVariant = DesktopCardBackground,
    onSurfaceVariant = DesktopTextSecondary,
    surfaceContainer = DesktopCardBackground,
    surfaceContainerHigh = DesktopCardBackground,
    surfaceContainerHighest = Color(0xFF1E2832),
    outline = DesktopInputBorder,
    outlineVariant = DesktopInputBorder.copy(alpha = 0.5f)
)

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

// Material You Color Schemes (entsprechend Android 12+ M3 Dynamic Colors aus dem Screenshot)
val MaterialYouDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9CAE8),
    onPrimary = Color(0xFF0D324C),
    primaryContainer = Color(0xFF21435C),
    onPrimaryContainer = Color(0xFFA9CAE8),
    secondary = Color(0xFFA9CAE8),
    onSecondary = Color(0xFF0D324C),
    secondaryContainer = Color(0xFF262D33),
    onSecondaryContainer = Color(0xFFE0E6EE),
    background = Color(0xFF151A1F),
    onBackground = Color(0xFFE0E6EE),
    surface = Color(0xFF151A1F),
    onSurface = Color(0xFFE0E6EE),
    surfaceVariant = Color(0xFF262D33),
    onSurfaceVariant = Color(0xFFA4ACB3),
    surfaceContainer = Color(0xFF262D33),
    surfaceContainerHigh = Color(0xFF20262C),
    surfaceContainerHighest = Color(0xFF363D44),
    surfaceContainerLow = Color(0xFF1D2227),
    surfaceContainerLowest = Color(0xFF101418),
    outline = Color(0xFF535A60),
    outlineVariant = Color(0xFF3F464C)
)

val MaterialYouLightColorScheme = lightColorScheme(
    primary = Color(0xFF3B6486),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF51606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E4F6),
    onSecondaryContainer = Color(0xFF0E1D2A),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF171C20),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF171C20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainer = Color(0xFFEEF1F5),
    surfaceContainerHigh = Color(0xFFE8EBEF),
    surfaceContainerHighest = Color(0xFFE2E5E9),
    outline = Color(0xFF72787E),
    outlineVariant = Color(0xFFC2C7CE)
)
