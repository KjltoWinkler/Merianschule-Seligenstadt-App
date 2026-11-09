package de.kjltowinkler.msapp.ui.substitution

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.delay
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_star_filled
import merianschuleseligenstadtapp.shared.generated.resources.ic_star_outline
import org.jetbrains.compose.resources.painterResource

@Composable
fun SubstitutionItemCard(
    entry: SubstitutionEntry,
    onToggleMark: (SubstitutionEntry) -> Unit,
    onClick: () -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            delay(150)
            try {
                bringIntoViewRequester.bringIntoView()
            } catch (_: Throwable) {}
        }
    }

    val defaultContainerColor = de.kjltowinkler.msapp.theme.screenCardContainerColor()
    val highlightTargetColor = lerp(
        defaultContainerColor,
        MaterialTheme.colorScheme.primary,
        0.12f
    )

    val cardBg by animateColorAsState(
        targetValue = if (isHighlighted) highlightTargetColor else defaultContainerColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "SubstitutionCardBg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .bringIntoViewRequester(bringIntoViewRequester)
            .handCursor()
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = cardBg
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
                    fontFamily = getQuicksandFontFamily(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        color = if (entry.isNew) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (entry.isNew) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zeile 2: Details (z.B. "Hr. Müller statt Fr. Schmidt")
            val formattedDetails = de.kjltowinkler.msapp.data.timetable.TimetableRepository.formatDetailsWithTeacherNames(entry.details)
            Text(
                text = formattedDetails,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Stern Markierungs-Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .handCursor()
                        .clickable { onToggleMark(entry) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (entry.isMarked) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                        ),
                        contentDescription = if (entry.isMarked) "Eintrag entmarkieren" else "Eintrag markieren",
                        tint = if (entry.isMarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
