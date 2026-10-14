package de.kjltowinkler.msapp.ui.substitution

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_star_filled
import merianschuleseligenstadtapp.shared.generated.resources.ic_star_outline
import org.jetbrains.compose.resources.painterResource

/**
 * Einzelne Vertretungskarte entsprechend `FigmaDesigns/vertretungsplan#alle.png`.
 *
 * Eigenschaften:
 * - Hintergrund: `#0C0F0B`, abgerundete Ecken: 28dp
 * - Kopfzeile: "3. Std - Mathematik - 9aR"
 * - Zeitangabe rechts oben: z.B. "7:41" oder "Gestern"
 * - Grüner Neu-Indikator: kleiner Kreis (#B8CCB5), wenn isNew = true
 * - Details: z.B. "Hr. Müller statt Fr. Schmidt"
 * - Raum / Info: z.B. "Raum 204 (statt 102) - Aufgaben..."
 * - Stern-Icon rechts unten: gefüllt wenn markiert, sonst Outline
 */
@Composable
fun SubstitutionItemCard(
    entry: SubstitutionEntry,
    onToggleMark: (SubstitutionEntry) -> Unit,
    onClick: () -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF000000)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Zeile 1: Kopfzeile (Stunde - Fach - Klasse) & Zeit/Neu-Indikator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // z.B. "3. Std - Mathematik - 9aR"
                val headerTitle = buildString {
                    append(entry.period)
                    if (entry.subject.isNotBlank()) append(" - ${entry.subject}")
                    if (entry.className.isNotBlank()) append(" - ${entry.className}")
                }
                Text(
                    text = headerTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE0E8DC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Zeit & Neu-Indikator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.time,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (entry.isNew) Color(0xFFE1E9DD) else Color(0xFFA5ADA2)
                    )

                    if (entry.isNew) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB8CCB5))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zeile 2: Details (z.B. "Hr. Müller statt Fr. Schmidt")
            Text(
                text = entry.details,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFEBE8DC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Zeile 3: Raum / Info links & Stern-Button rechts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.roomInfo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFA6AEA3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Stern Markierungs-Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggleMark(entry) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (entry.isMarked) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                        ),
                        contentDescription = if (entry.isMarked) "Eintrag entmarkieren" else "Eintrag markieren",
                        tint = if (entry.isMarked) Color(0xFF92998F) else Color(0xFFA5ADA2),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
