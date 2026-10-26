package de.kjltowinkler.msapp.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

val AvatarColorPalette = listOf(
    0xFF395A3CL, // Grün
    0xFF1B65A4L, // Blau
    0xFF007A78L, // Teal
    0xFF6B4FA2L, // Lila
    0xFFBF584DL, // Koralle
    0xFF4A5568L  // Schiefer
)

/**
 * Gibt eine deterministische Akzentfarbe aus der Palette basierend auf dem Namen zurück.
 */
fun getAvatarColorForName(name: String): Long {
    if (name.isBlank()) return AvatarColorPalette[0]
    val hash = name.trim().lowercase().fold(0) { acc, c -> (acc * 31 + c.code) and 0x7FFFFFFF }
    return AvatarColorPalette[hash % AvatarColorPalette.size]
}

/**
 * Ermittelt die Initialen/Buchstaben für einen Accountnamen (z. B. "SW" für "Sebastian Wagner", "S" für "Sebastian").
 */
fun getInitialsForName(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "U"
    val parts = clean.split("[\\s._-]+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        parts.size == 1 -> parts[0].first().uppercase()
        else -> "U"
    }
}

/**
 * Universelle Avatar-Darstellung für Nutzerprofile.
 * Standardmäßig ein schlichtes, modernes Buchstaben-Profilbild (Initials) basierend auf dem Accountnamen,
 * optional auch Vorlagen wie Schulmaskottchen oder Schullogo.
 */
@Composable
fun AccountAvatar(
    accountName: String,
    avatarType: String = "initials",
    avatarColorHex: Long = 0xFF395A3CL,
    modifier: Modifier = Modifier
) {
    when (avatarType) {
        "mascot_clean" -> {
            Image(
                painter = painterResource(Res.drawable.tasks_done_clean),
                contentDescription = "Maskottchen Profilbild",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .clip(CircleShape)
                    .background(Color(0xFF272E26))
            )
        }
        "mascot_char" -> {
            Image(
                painter = painterResource(Res.drawable.tasks_done_char),
                contentDescription = "Maskottchen Profilbild",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .clip(CircleShape)
                    .background(Color(0xFF272E26))
            )
        }
        "app_icon" -> {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "Schul-Wappen Profilbild",
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .clip(CircleShape)
                    .background(Color(0xFF202820))
                    .padding(6.dp)
            )
        }
        else -> {
            // "initials" oder Fallback: Modernes Buchstaben-Profilbild
            val initials = getInitialsForName(accountName)
            val effectiveColorHex = if (avatarColorHex != 0L) avatarColorHex else getAvatarColorForName(accountName)

            BoxWithConstraints(
                modifier = modifier
                    .clip(CircleShape)
                    .background(Color(effectiveColorHex)),
                contentAlignment = Alignment.Center
            ) {
                val sizeDp = if (maxWidth != Dp.Infinity && maxWidth > 0.dp) {
                    minOf(maxWidth, if (maxHeight != Dp.Infinity && maxHeight > 0.dp) maxHeight else maxWidth)
                } else if (maxHeight != Dp.Infinity && maxHeight > 0.dp) {
                    maxHeight
                } else {
                    32.dp
                }

                val factor = if (initials.length <= 1) 0.52f else 0.42f
                val fontSize = (sizeDp.value * factor).sp

                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = if (initials.length <= 1) 0.sp else (-0.5).sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
