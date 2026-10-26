package de.kjltowinkler.msapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Einstellungen-Screen mit voller Unterstützung für Material You (Dynamic Colors):
 * 
 * - Nutzt die dynamischen Systemfarben von MaterialTheme.colorScheme (Hintergrund, Karten, Switches, Akzente).
 * - Enthält in der Sektion "Darstellung" Umschalter für:
 *   1. "Dunkles Design"
 *   2. "Material You" (Dynamische Farben)
 * - Switch-Design nach Material 3 ohne Thumb-Icons.
 */
@Composable
fun SettingsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    useMaterialYou: Boolean = true,
    onMaterialYouChanged: (Boolean) -> Unit = {},
    initialDarkTheme: Boolean = true,
    onDarkThemeChanged: (Boolean) -> Unit = {},
    onSchoolWayClicked: () -> Unit = {}
) {
    var delayWarningsEnabled by rememberSaveable { mutableStateOf(true) }
    var relaxedDismissalEnabled by rememberSaveable { mutableStateOf(true) }
    var taskReminderEnabled by rememberSaveable { mutableStateOf(true) }
    var darkThemeEnabled by rememberSaveable { mutableStateOf(initialDarkTheme) }
    var materialYouEnabled by rememberSaveable { mutableStateOf(useMaterialYou) }

    var selectedReminderTime by rememberSaveable { mutableStateOf("10 Minuten vorher") }
    var showPickTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomPickTimeDialog by rememberSaveable { mutableStateOf(false) }

    // Dynamische Material You Farbwerte aus dem aktiven MaterialTheme
    val screenBackground = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surfaceContainer
    val backButtonBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val categoryHeader = MaterialTheme.colorScheme.primary
    val dividerHorizontal = MaterialTheme.colorScheme.background
    val dividerVertical = MaterialTheme.colorScheme.outlineVariant

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
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backButtonBg)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "Zurück",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Einstellungen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BEREICH 1: Schulweg & Mobilität ---
            CategoryHeader(title = "Schulweg & Mobilität", color = categoryHeader)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Mein Schulweg
                    val schoolWayConfig = de.kjltowinkler.msapp.data.schoolway.SchoolWayRepository.currentConfig
                    SettingsItemRow(
                        title = "Mein Schulweg",
                        subtitle = "${schoolWayConfig.busLine} • ${schoolWayConfig.morningEndStop}",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = onSchoolWayClicked
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 2: Verspätungs-Warnungen
                    SettingsItemRow(
                        title = "Verspätungs-Warnungen",
                        subtitle = "Push-Nachricht bei Verspätungen\noder Ausfällen.",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = delayWarningsEnabled,
                                onCheckedChange = { delayWarningsEnabled = it },
                                contentDescription = "Verspätungs-Warnungen"
                            )
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 3: Entspannter Schulschluss
                    SettingsItemRow(
                        title = "Entspannter Schulschluss",
                        subtitle = "Smarte Hinweise zur Gehzeit\nnach dem Gong",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = relaxedDismissalEnabled,
                                onCheckedChange = { relaxedDismissalEnabled = it },
                                contentDescription = "Entspannter Schulschluss"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BEREICH 2: Benachrichtigungen ---
            CategoryHeader(title = "Benachrichtigungen", color = categoryHeader)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    var vplanUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
                    SettingsItemRow(
                        title = "Vertretungsplan-Updates",
                        subtitle = "Sofort bei Entfall oder Raumänderung\nbenachrichtigen",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = vplanUpdatesEnabled,
                                onCheckedChange = { vplanUpdatesEnabled = it },
                                contentDescription = "Vertretungsplan-Updates"
                            )
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 1b: Test-Benachrichtigung senden
                    SettingsItemRow(
                        title = "Test-Benachrichtigung",
                        subtitle = "Beispiel-Push für Ausfall & Vertretung\nsofort testen",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            FilledTonalButton(
                                onClick = {
                                    de.kjltowinkler.msapp.util.PlatformNotifier.sendNotification(
                                        title = "Vertretungsplan: Unterrichtsentfall",
                                        summary = "Heute • 3. Std Deutsch (Af) entfällt",
                                        details = listOf(
                                            "• Heute, 3. Std Deutsch (Af): Entfall ersatzlos",
                                            "• Heute, 5. Std Klassenlehrerstunde (Tho): Entfall"
                                        )
                                    )
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Testen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 2: Aufgaben-Erinnerung mit Zeitauswahl-Chevron
                    SettingsItemRow(
                        title = "Aufgaben-Erinnerung",
                        subtitle = "Einstellbare Erinnerungszeit\nvor Ablauf einer Aufgabe.",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chevron-Button zur Zeitauswahl
                                IconButton(
                                    onClick = { showPickTimeDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_chevron_right),
                                        contentDescription = "Erinnerungszeit ändern",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Vertikaler Trenner wie im Screenshot
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .width(1.dp)
                                        .background(dividerVertical)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                MaterialYouSwitch(
                                    checked = taskReminderEnabled,
                                    onCheckedChange = { taskReminderEnabled = it },
                                    contentDescription = "Aufgaben-Erinnerung"
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BEREICH 3: Darstellung ---
            CategoryHeader(title = "Darstellung", color = categoryHeader)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Dunkles Design
                    SettingsItemRow(
                        title = "Dunkles Design",
                        subtitle = "Dunkles Design für die App\naktivieren",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = darkThemeEnabled,
                                onCheckedChange = {
                                    darkThemeEnabled = it
                                    onDarkThemeChanged(it)
                                },
                                contentDescription = "Dunkles Design"
                            )
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 2: Material You (Dynamische Farben)
                    SettingsItemRow(
                        title = "Material You",
                        subtitle = "Farben an System & Hintergrundbild\nanpassen",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = materialYouEnabled,
                                onCheckedChange = {
                                    materialYouEnabled = it
                                    onMaterialYouChanged(it)
                                },
                                contentDescription = "Material You"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog 1: Vordefinierte Zeiten (picktime.png)
    if (showPickTimeDialog) {
        PickReminderTimeDialog(
            selectedOption = selectedReminderTime,
            onOptionSelected = { option ->
                selectedReminderTime = option
                showPickTimeDialog = false
            },
            onCustomClicked = {
                showPickTimeDialog = false
                showCustomPickTimeDialog = true
            },
            onDismissRequest = { showPickTimeDialog = false },
            useMaterialYou = materialYouEnabled
        )
    }

    // Dialog 2: Benutzerdefinierte Zeit (picktimedefined.png)
    if (showCustomPickTimeDialog) {
        PickCustomReminderTimeDialog(
            initialAmount = "10",
            initialUnitIndex = 0,
            onConfirm = { result ->
                selectedReminderTime = result
                showCustomPickTimeDialog = false
            },
            onDismissRequest = { showCustomPickTimeDialog = false },
            useMaterialYou = materialYouEnabled
        )
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = color
        ),
        modifier = modifier.padding(start = 8.dp, bottom = 10.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsItemRow(
    title: String,
    subtitle: String,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 82.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
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
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor,
                    lineHeight = 22.sp
                )
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailing()
        }
    }
}

/**
 * Switch mit Material You Farben im cleanen Material 3 Design ohne Thumb-Icons.
 */
@Composable
private fun MaterialYouSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Switch(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        )
    )
}
