package de.kjltowinkler.msapp.ui.substitution

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import de.kjltowinkler.msapp.util.shareImageBitmap
import kotlinx.coroutines.launch

/**
 * Informations-BottomSheet für einen Vertretungseintrag entsprechend:
 * `FigmaDesigns/vertretungsplan#infosheet.png`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionDetailBottomSheet(
    entry: SubstitutionEntry,
    onDismissRequest: () -> Unit,
    onToggleMark: () -> Unit,
    onShare: () -> Unit = {},
    onSaveAsTask: () -> Unit = {},
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    // Dynamische Material You-Farben mit Figma-Fallback
    val sheetContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color(0xFF141914)
    }

    val brickBackgroundColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color(0xFF1E241E)
    }

    val dragHandleColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        Color(0xFF5E655C)
    }

    val closeButtonBg = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color(0xFF222822)
    }

    val closeIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFB0B8AE)
    }

    val textPrimaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFE4EDE0)
    }

    val textSecondaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5AEA3)
    }

    val metaIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF9CA599)
    }

    val actionIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFE4EDE0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = textPrimaryColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            // Drag-Griff
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(dragHandleColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            val topBrickShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
            val middleBrickShape = RoundedCornerShape(3.dp)
            val bottomBrickShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            val cardBg = brickBackgroundColor

            // --- HEADER IM BOTTOMSHEET & AUFGEZEICHNETER BEREICH FÜR DAS TEILEN ---
            val headerTitle = buildString {
                append(entry.subject)
                if (entry.className.isNotBlank()) {
                    append(" • ${entry.className}")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = headerTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(closeButtonBg)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = "Schließen",
                            tint = closeIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // --- META-INFOS (Datum, Stunde & Zeit, Geändert um) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zeile 1: Datum (z.B. "Montag, 12. Oktober")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_schedule),
                            contentDescription = null,
                            tint = metaIconTint,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = entry.dayLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )
                    }

                    // Zeile 2: Stunde & Uhrzeit (z.B. "3. Stunde (09:45 – 10:30 Uhr)")
                    val periodTimeDetail = when {
                        entry.period.contains("1") -> "1. Stunde (07:55 – 08:40 Uhr)"
                        entry.period.contains("2") -> "2. Stunde (08:45 – 09:30 Uhr)"
                        entry.period.contains("3") -> "3. Stunde (09:45 – 10:30 Uhr)"
                        entry.period.contains("4") -> "4. Stunde (10:35 – 11:20 Uhr)"
                        entry.period.contains("5") -> "5. Stunde (11:35 – 12:20 Uhr)"
                        entry.period.contains("6") -> "6. Stunde (12:25 – 13:10 Uhr)"
                        else -> "${entry.period} Stunde"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_nav_schedule),
                            contentDescription = null,
                            tint = metaIconTint,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = periodTimeDetail,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )
                    }

                    // Zeile 3: Geändert um (z.B. "Geändert um 07:41 Uhr")
                    val modifiedText = if (entry.time.contains(":")) {
                        "Geändert um ${entry.time} Uhr"
                    } else {
                        "Geändert: ${entry.time}"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_history),
                            contentDescription = null,
                            tint = metaIconTint,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = modifiedText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )
                    }
                }


            // --- INFO-BLOCK GRUPPE (Lehrkraft, Raum, Aufgaben/Notizen als Bricks) ---
            // Parse details and roomInfo
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

            val notesText = if (roomSub.isNotBlank()) roomSub else "Keine weiteren Notizen vorhanden."

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Lehrkraft (oberer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(topBrickShape)
                        .background(brickBackgroundColor)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_person),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = teacherMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                        if (teacherSub.isNotBlank()) {
                            Text(
                                text = "· $teacherSub",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = textSecondaryColor
                            )
                        }
                    }
                }

                // 2. Raum (mittlerer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(middleBrickShape)
                        .background(brickBackgroundColor)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_location_on),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = roomMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                    }
                }

                // 3. Aufgaben & Notizen (unterer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bottomBrickShape)
                        .background(brickBackgroundColor)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_notes),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Aufgaben & Notizen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                        Text(
                            text = notesText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            } // Ende des aufgezeichneten Bereichs

            Spacer(modifier = Modifier.height(14.dp))

            // --- AKTIONEN-GRUPPE (Vertretung merken, Teilen, Als Aufgabe speichern ebenfalls als Bricks) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Aktion 1: Vertretung merken / Markierung entfernen (oberer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(topBrickShape)
                        .background(brickBackgroundColor)
                        .clickable { onToggleMark() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (entry.isMarked) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                        ),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (entry.isMarked) "Markierung entfernen" else "Eintrag merken",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                }

                // Aktion 2: Teilen (mittlerer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(middleBrickShape)
                        .background(brickBackgroundColor)
                        .clickable {
                            coroutineScope.launch {
                                try {
                                    val bitmap = graphicsLayer.toImageBitmap()
                                    val cleanSubject = entry.subject.replace(Regex("[^a-zA-Z0-9]"), "_")
                                    shareImageBitmap(
                                        imageBitmap = bitmap,
                                        title = "Vertretung teilen",
                                        filenamePrefix = "vertretung_${cleanSubject}"
                                    )
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                            onShare()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_share),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Als Bild teilen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                }

                // Aktion 3: Als Aufgabe speichern (unterer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bottomBrickShape)
                        .background(brickBackgroundColor)
                        .clickable { onSaveAsTask() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add_task),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Als Aufgabe speichern",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                }
            }
        }
    }
}
