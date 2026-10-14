package de.kjltowinkler.msapp.ui.substitution

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Hochwertig designtes Info-Card-Layout, speziell für das Teilen als Bild (Share-Sheet).
 * Sieht wie eine eigenständige, professionelle Grafikkarte aus ("Branded Social Share Card")
 * anstelle eines einfachen App-Screenshots:
 * - Edler dunkler Farbverlauf-Hintergrund mit dezentem Border & Glow-Akzent
 * - Prominenter Status-Pill (z. B. "ENTFALL", "VERTRETUNG", "VERLEGUNG")
 * - Merianschule Header-Branding mit Schullogo und App-Kennzeichnung
 * - Groß typografierter Kernbereich (Fach, Klasse, Stunde)
 * - Strukturierte Infoboxen für Lehrkraft, Raum und Aufgaben
 * - Schicker Footer mit Aktualisierungszeit & Merianschule Seligenstadt Tagline
 */
@Composable
fun SubstitutionShareCard(
    entry: SubstitutionEntry,
    modifier: Modifier = Modifier
) {
    // Dynamische Akzentfarben je nach Typ
    val (statusLabel, statusBg, statusTextColor, statusBorderColor) = when {
        entry.type.contains("Entfall", ignoreCase = true) || entry.details.contains("entfällt", ignoreCase = true) -> {
            Quad(
                "ENTFALL",
                Color(0xFF3B1D1D),
                Color(0xFFFFB4AB),
                Color(0xFF853535)
            )
        }
        entry.type.contains("Verlegung", ignoreCase = true) || entry.details.contains("verlegt", ignoreCase = true) -> {
            Quad(
                "RAUMÄNDERUNG",
                Color(0xFF352B19),
                Color(0xFFFFDE9E),
                Color(0xFF8A6C2B)
            )
        }
        else -> {
            Quad(
                "VERTRETUNG",
                Color(0xFF1B331E),
                Color(0xFFB5E8B9),
                Color(0xFF386A3E)
            )
        }
    }

    val (teacherMain, teacherSub) = if (entry.details.contains(" statt ")) {
        val parts = entry.details.split(" statt ", limit = 2)
        parts[0].trim() to "statt ${parts[1].trim()}"
    } else {
        entry.details to ""
    }

    val (roomMain, roomSub) = if (entry.roomInfo.contains(" | ")) {
        val parts = entry.roomInfo.split(" | ", limit = 2)
        parts[0].trim() to parts[1].trim()
    } else if (entry.roomInfo.contains(" - ")) {
        val parts = entry.roomInfo.split(" - ", limit = 2)
        parts[0].trim() to parts[1].trim()
    } else {
        entry.roomInfo to ""
    }

    val periodTimeDetail = when {
        entry.period.contains("1") -> "1. Stunde (07:55 – 08:40)"
        entry.period.contains("2") -> "2. Stunde (08:45 – 09:30)"
        entry.period.contains("3") -> "3. Stunde (09:45 – 10:30)"
        entry.period.contains("4") -> "4. Stunde (10:35 – 11:20)"
        entry.period.contains("5") -> "5. Stunde (11:35 – 12:20)"
        entry.period.contains("6") -> "6. Stunde (12:25 – 13:10)"
        else -> "${entry.period} Stunde"
    }

    // Äußerer Container mit dezentem Glow & Rahmen
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF182219),
                        Color(0xFF101611),
                        Color(0xFF0C100D)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3F5540),
                        Color(0xFF233024),
                        Color(0xFF151C15)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // --- HEADER: Branding & Status Badge ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF263828))
                            .border(1.dp, Color(0xFF4B6B4E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_school),
                            contentDescription = null,
                            tint = Color(0xFFBBE5BD),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Merianschule",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4EDE0),
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "Seligenstadt • Vertretungsplan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8F9E8C)
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusBg)
                        .border(1.dp, statusBorderColor, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- HERO-SECTION: Fach, Klasse & Stunde ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF151D16))
                    .border(1.dp, Color(0xFF253326), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.subject.ifBlank { "Vertretung" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0F7EE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.className.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF263728))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = entry.className,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC7ECC9)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_schedule),
                            contentDescription = null,
                            tint = Color(0xFF98AA96),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${entry.dayLabel}  •  $periodTimeDetail",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC1CDC0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- DETAILS RASTER: Lehrkraft & Raum ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kachel 1: Lehrkraft
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141A15))
                        .border(1.dp, Color(0xFF222B23), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_person),
                                contentDescription = null,
                                tint = Color(0xFF8CA58A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "LEHRKRAFT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8CA58A),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = teacherMain.ifBlank { "–" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE4EDE0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (teacherSub.isNotBlank()) {
                            Text(
                                text = teacherSub,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF9DA89B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Kachel 2: Raum
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141A15))
                        .border(1.dp, Color(0xFF222B23), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_location_on),
                                contentDescription = null,
                                tint = Color(0xFF8CA58A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "RAUM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8CA58A),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = roomMain.ifBlank { "–" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE4EDE0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Unterrichtsort",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF9DA89B)
                        )
                    }
                }
            }

            // --- ZUSATZ-HINWEISE / AUFGABEN (falls vorhanden) ---
            if (roomSub.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141A15))
                        .border(1.dp, Color(0xFF222B23), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_notes),
                            contentDescription = null,
                            tint = Color(0xFF8CA58A),
                            modifier = Modifier.padding(top = 2.dp).size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "AUFGABEN & NOTIZEN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8CA58A),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = roomSub,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFDFE8DD),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FOOTER: Zeitstempel & Signatur ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (entry.time.contains(":")) "Stand: ${entry.time} Uhr" else "Stand: ${entry.time}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF758573)
                )
                Text(
                    text = "Erstellt mit der Merianschule App",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF758573)
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
