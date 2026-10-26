package de.kjltowinkler.msapp.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.schoolway.SchoolWayRepository
import kotlinx.coroutines.delay
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

enum class StopSelectionTarget {
    START,
    END
}

/**
 * Bottom-Sheet exakt nach Figma-Design `settings#einstiegsheet.png` & `settings#zielstellesheet.png`:
 *
 * - Erscheint beim Klick auf "Einstiegshaltestelle" oder "Zielhaltestelle"
 * - Header mit Titel "Haltestelle wählen" und rundem Schließen-Button
 * - Zwei separate abgerundete Pillen-Felder:
 *     1. "Einstiegshaltestelle wählen" mit Kreis-Icon
 *     2. "Deine Zielhaltestelle" mit Location-Pin-Icon
 * - Vertikal gestrichelte Verbindungslinie zwischen beiden Feldern
 * - Sektion "Empfehlungen" mit Trennlinien
 * - Haltestellen in Hellgrün (`#ABD0AA`)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectStopBottomSheet(
    initialTarget: StopSelectionTarget = StopSelectionTarget.START,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val config = SchoolWayRepository.currentConfig
    val isStart = initialTarget == StopSelectionTarget.START
    val isEnd = initialTarget == StopSelectionTarget.END

    var startStopTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = config.morningStartStop,
                selection = if (isStart) TextRange(0, config.morningStartStop.length) else TextRange.Zero
            )
        )
    }
    var endStopTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = config.morningEndStop,
                selection = if (isEnd) TextRange(0, config.morningEndStop.length) else TextRange.Zero
            )
        )
    }

    val startFocusRequester = remember { FocusRequester() }
    val endFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(150)
        try {
            if (isStart) {
                startStopTextFieldValue = startStopTextFieldValue.copy(
                    selection = TextRange(0, startStopTextFieldValue.text.length)
                )
                startFocusRequester.requestFocus()
            } else {
                endStopTextFieldValue = endStopTextFieldValue.copy(
                    selection = TextRange(0, endStopTextFieldValue.text.length)
                )
                endFocusRequester.requestFocus()
            }
            keyboardController?.show()
        } catch (_: Exception) {
        }
    }

    val recommendations = SchoolWayRepository.customRecommendations

    // Exakte Figma-Farben aus settings#einstiegsheet.png & settings#zielstellesheet.png
    val sheetContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF151B15) // Exakter Figma Hex-Wert
    }

    val dragHandleColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        Color(0xFF484E47)
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

    val pillBackground = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color(0xFF0C0F0B) // Exakter Figma Hex-Wert für die beiden Haltestellen-Pillen
    }

    val textPrimary = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFE4EDE0)
    }

    val textSecondary = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5ADA2)
    }

    val activeAccent = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFABD0AA) // Exakter Figma Hex-Wert für empfohlene Haltestellen
    }

    val dividerColor = Color(0xFF1E251E)

    ModalBottomSheet(
        onDismissRequest = {
            SchoolWayRepository.updateMorningStops(startStopTextFieldValue.text, endStopTextFieldValue.text)
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(dragHandleColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
        ) {
            // --- TITELLEISTE: "Haltestelle wählen" & runder Schließen-Button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Haltestelle wählen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                )

                IconButton(
                    onClick = {
                        SchoolWayRepository.updateMorningStops(startStopTextFieldValue.text, endStopTextFieldValue.text)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(closeButtonBg)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Schließen",
                        tint = closeIconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // --- ZWEI SEPARATE PILLEN MIT GESTRICHELTER VERBINDUNGSLINIE ---
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Pille: Einstiegshaltestelle (settings#einstiegsheet.png aktiv / settings#zielstellesheet.png inaktiv)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(pillBackground)
                        .then(
                            if (isStart) {
                                Modifier.clickable {
                                    startFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kreis-Icon für Start (Weiß/textPrimary wenn aktiv, Grau/textSecondary wenn inaktiv)
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isStart) textPrimary else textSecondary,
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    BasicTextField(
                        value = startStopTextFieldValue,
                        onValueChange = {
                            if (isStart) {
                                startStopTextFieldValue = it
                            }
                        },
                        readOnly = !isStart,
                        enabled = isStart,
                        modifier = Modifier
                            .weight(1f)
                            .then(if (isStart) Modifier.focusRequester(startFocusRequester) else Modifier),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = if (isStart) FontWeight.Bold else FontWeight.Normal,
                            color = if (isStart) textPrimary else textSecondary
                        ),
                        cursorBrush = SolidColor(textPrimary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (startStopTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Einstiegshaltestelle wählen",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = if (isStart) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isStart) textPrimary else textSecondary
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Gestrichelte Verbindungslinie zwischen den beiden Pillen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .padding(start = 25.dp) // Exakt ausgerichtet auf die Mitte des Kreis-Icons (18dp Padding + 7.5dp)
                            .width(2.dp)
                            .fillMaxHeight()
                    ) {
                        drawLine(
                            color = textSecondary.copy(alpha = 0.8f),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }
                }

                // 2. Pille: Zielhaltestelle (settings#zielstellesheet.png aktiv / settings#einstiegsheet.png inaktiv)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(pillBackground)
                        .then(
                            if (isEnd) {
                                Modifier.clickable {
                                    endFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location-Pin für Ziel (Weiß/textPrimary wenn aktiv, Grau/textSecondary wenn inaktiv)
                    Icon(
                        painter = painterResource(Res.drawable.ic_location_on),
                        contentDescription = null,
                        tint = if (isEnd) textPrimary else textSecondary,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(modifier = Modifier.width(13.dp))

                    BasicTextField(
                        value = endStopTextFieldValue,
                        onValueChange = {
                            if (isEnd) {
                                endStopTextFieldValue = it
                            }
                        },
                        readOnly = !isEnd,
                        enabled = isEnd,
                        modifier = Modifier
                            .weight(1f)
                            .then(if (isEnd) Modifier.focusRequester(endFocusRequester) else Modifier),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = if (isEnd) FontWeight.Bold else FontWeight.Normal,
                            color = if (isEnd) textPrimary else textSecondary
                        ),
                        cursorBrush = SolidColor(textPrimary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (endStopTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Deine Zielhaltestelle",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = if (isEnd) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isEnd) textPrimary else textSecondary
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- EMPFEHLUNGEN SEKTION ---
            Text(
                text = "Empfehlungen",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Liste der Empfehlungen mit Trennlinien
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                itemsIndexed(recommendations) { index, stopName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isStart) {
                                    SchoolWayRepository.updateMorningStops(
                                        startStop = stopName,
                                        endStop = endStopTextFieldValue.text
                                    )
                                } else {
                                    SchoolWayRepository.updateMorningStops(
                                        startStop = startStopTextFieldValue.text,
                                        endStop = stopName
                                    )
                                }
                                onDismissRequest()
                            }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kreis-Icon
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = textSecondary,
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = stopName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent
                            )
                        )
                    }

                    if (index < recommendations.size - 1) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = dividerColor
                        )
                    }
                }
            }
        }
    }
}
