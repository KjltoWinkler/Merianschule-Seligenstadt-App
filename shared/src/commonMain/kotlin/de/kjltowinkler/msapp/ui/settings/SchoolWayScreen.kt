package de.kjltowinkler.msapp.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.schoolway.SchoolWayRepository
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Einstellungen-Screen für "Mein Schulweg" exakt nach Figma-Design `settings#meinschulweg.png`:
 *
 * - Top-Bar mit Zurück-Pfeil, Titel "Mein Schulweg" und 3-Punkte-Menü
 * - Bereich 1: "Hinfahrt (Morgens)" mit Einstiegshaltestelle & Zielhaltestelle
 *   -> Beim Klick auf "Einstiegshaltestelle" oder "Zielhaltestelle" öffnet sich
 *      das Bottom-Sheet nach `settings#einstiegsheet.png`
 * - Bereich 2: "Rückfahrt (Nachmittags)" mit Ausstiegshaltestelle & Gehzeit zum Bussteig
 * - Interaktiver Gehzeit-Slider / Stepper mit Minus-/Plus-Buttons und Pill-Thumb
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolWayScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    useMaterialYou: Boolean = true
) {
    val config = SchoolWayRepository.currentConfig
    var activeStopSheet by remember { mutableStateOf<StopSelectionTarget?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Farbwerte: Exakte Figma-Werte mit Material You Fallback
    val screenBackground = if (useMaterialYou) {
        MaterialTheme.colorScheme.background
    } else {
        Color(0xFF0C0F0B)
    }

    val cardBackground = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF151B15)
    }

    val textPrimary = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFDFE9DE)
    }

    val textSecondary = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5ADA2)
    }

    val categoryHeaderColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFF7FFBC) // Exakter Figma-Hexwert für Hinfahrt/Rückfahrt Überschriften
    }

    val accentColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFABD0AA) // Exakter Figma-Hexwert für Slider-Füllung & Buttons
    }

    val sliderTrackInactive = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color(0xFF202820) // Exakter Figma-Hexwert für inaktiven Slider-Track
    }

    val dividerColor = screenBackground

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- TOP BAR (Zurück-Pfeil, Titel, 3-Punkte-Menü) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "Zurück",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Mein Schulweg",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = "Weitere Optionen",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auf Standard zurücksetzen") },
                            onClick = {
                                SchoolWayRepository.resetToDefaults()
                                showOverflowMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BEREICH 1: Hinfahrt (Morgens) ---
            Text(
                text = "Hinfahrt (Morgens)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryHeaderColor
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp, top = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Einstiegshaltestelle (öffnet einstiegsheet.png)
                    SchoolWayItemRow(
                        title = "Einstiegshaltestelle",
                        subtitle = config.morningStartStop,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = { activeStopSheet = StopSelectionTarget.START }
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerColor)

                    // Item 2: Zielhaltestelle (öffnet einstiegsheet.png)
                    SchoolWayItemRow(
                        title = "Zielhaltestelle",
                        subtitle = config.morningEndStop,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = { activeStopSheet = StopSelectionTarget.END }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- BEREICH 2: Rückfahrt (Nachmittags) ---
            Text(
                text = "Rückfahrt (Nachmittags)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryHeaderColor
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp, top = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Ausstiegshaltestelle
                    SchoolWayItemRow(
                        title = "Ausstiegshaltestelle",
                        subtitle = config.afternoonEndStop,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = null
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerColor)

                    // Item 2: Gehzeit zum Bussteig mit Stepper & Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "Gehzeit zum Bussteig",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${config.walkingMinutes} Min. Fußweg vom\nKlassenzimmer",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = textSecondary,
                                lineHeight = 21.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Custom Stepper / Slider Row (Minus, Slider, Plus)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Minus Button
                            IconButton(
                                onClick = {
                                    if (config.walkingMinutes > 1) {
                                        SchoolWayRepository.updateWalkingMinutes(config.walkingMinutes - 1)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_remove),
                                    contentDescription = "Gehzeit verringern",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Steps-Slider basierend auf StepsSliderSample
                            StepsSlider(
                                value = config.walkingMinutes,
                                onValueChange = { SchoolWayRepository.updateWalkingMinutes(it) },
                                activeColor = accentColor,
                                inactiveColor = sliderTrackInactive,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Plus Button
                            IconButton(
                                onClick = {
                                    if (config.walkingMinutes < 10) {
                                        SchoolWayRepository.updateWalkingMinutes(config.walkingMinutes + 1)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_add),
                                    contentDescription = "Gehzeit erhöhen",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- BOTTOM SHEET BEIM KLICK AUF "Einstiegshaltestelle" ODER "Zielhaltestelle" (settings#einstiegsheet.png) ---
    activeStopSheet?.let { target ->
        SelectStopBottomSheet(
            initialTarget = target,
            onDismissRequest = { activeStopSheet = null },
            useMaterialYou = useMaterialYou
        )
    }
}

/**
 * Zeile für eine Haltestelle mit Titel und Untertitel nach Figma-Design.
 */
@Composable
private fun SchoolWayItemRow(
    title: String,
    subtitle: String,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 78.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleColor,
                lineHeight = 21.sp
            )
        )
    }
}

/**
 * Gehzeit-Slider implementiert nach dem offiziellen Material 3 StepsSliderSample:
 * - Nutzt rememberSliderState mit steps = 8 (Werte 1 bis 10) und valueRange = 1f..10f
 * - onValueChangeFinished für das Aktualisieren der gespeicherten Gehzeit
 * - Offizieller Material 3 Slider mit diskreten Schritten (Step-Ticks)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepsSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val sliderState = rememberSliderState(
        value = value.toFloat(),
        // 8 Schritte zwischen Endpunkten 1 und 10 -> diskrete Werte: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        steps = 8,
        valueRange = 1f..10f
    )
    sliderState.onValueChangeFinished = {
        onValueChange(kotlin.math.round(sliderState.value).toInt())
    }

    LaunchedEffect(value) {
        if (kotlin.math.abs(sliderState.value - value.toFloat()) >= 0.5f) {
            sliderState.value = value.toFloat()
        }
    }

    LaunchedEffect(sliderState.value) {
        val rounded = kotlin.math.round(sliderState.value).toInt()
        if (rounded != value) {
            onValueChange(rounded)
        }
    }

    val colors = SliderDefaults.colors(
        thumbColor = activeColor,
        activeTrackColor = activeColor,
        inactiveTrackColor = inactiveColor,
        activeTickColor = inactiveColor,
        inactiveTickColor = activeColor
    )

    Slider(
        state = sliderState,
        modifier = modifier,
        colors = colors
    )
}
