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
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import de.kjltowinkler.msapp.data.task.TaskNotificationManager
import de.kjltowinkler.msapp.ui.components.PredictiveBackContainer
import de.kjltowinkler.msapp.ui.components.handCursor
 
@Composable
fun SettingsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    studentId: String = "#104829",
    accountName: String = "Sebastian Wagner",
    useMaterialYou: Boolean = true,
    onMaterialYouChanged: (Boolean) -> Unit = {},
    initialDarkTheme: Boolean = true,
    onDarkThemeChanged: (Boolean) -> Unit = {},
    predictiveBackEnabled: Boolean = true,
    onPredictiveBackChanged: (Boolean) -> Unit = {},
    onSchoolWayClicked: () -> Unit = {},
    onCoursesClicked: () -> Unit = {},
    isTwoFactorEnabled: Boolean = false,
    onNavigateTo2FaSetup: () -> Unit = {},
    onDisable2Fa: () -> Unit = {}
) {
    var delayWarningsEnabled by rememberSaveable { mutableStateOf(true) }
    var relaxedDismissalEnabled by rememberSaveable { mutableStateOf(true) }
    var taskReminderEnabled by rememberSaveable { mutableStateOf(TaskNotificationManager.isRemindersEnabled) }
    var darkThemeEnabled by rememberSaveable { mutableStateOf(initialDarkTheme) }
    var materialYouEnabled by rememberSaveable { mutableStateOf(useMaterialYou) }
    var isPredictiveBackOn by rememberSaveable { mutableStateOf(predictiveBackEnabled) }

    var selectedReminderTime by rememberSaveable { mutableStateOf(TaskNotificationManager.reminderLeadTime) }
    var showPickTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomPickTimeDialog by rememberSaveable { mutableStateOf(false) }
    var isTwoFactorOn by remember(isTwoFactorEnabled) { mutableStateOf(isTwoFactorEnabled) }
    var showDisable2FaDialog by remember { mutableStateOf(false) }

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
                        .handCursor()
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
                        fontFamily = getQuicksandFontFamily(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BEREICH 0: Unterricht & Kurse ---
            CategoryHeader(title = "Unterricht & Kurse", color = categoryHeader)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val courses = de.kjltowinkler.msapp.data.courses.CourseRepository.courses
                    val courseSubtitle = if (courses.isEmpty()) {
                        "Noch keine Kurse hinterlegt"
                    } else {
                        "${courses.size} Kurse hinterlegt (${courses.take(3).joinToString { it.subject }})"
                    }
                    SettingsItemRow(
                        title = "Meine Kurse",
                        subtitle = courseSubtitle,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = onCoursesClicked
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

                    // Item 2: Aufgaben-Erinnerung mit Zeitauswahl-Chevron
                    SettingsItemRow(
                        title = "Aufgaben-Erinnerung",
                        subtitle = "$selectedReminderTime vor Ablauf\nsowie bei Überfälligkeit.",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = { showPickTimeDialog = true },
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
                                    onCheckedChange = {
                                        taskReminderEnabled = it
                                        TaskNotificationManager.isRemindersEnabled = it
                                        if (it) TaskNotificationManager.checkAndNotify()
                                    },
                                    contentDescription = "Aufgaben-Erinnerung"
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BEREICH: Sicherheit & Konto ---
            CategoryHeader(title = "Sicherheit & Konto", color = categoryHeader)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItemRow(
                        title = "Zwei-Faktor-Authentifizierung (2FA)",
                        subtitle = if (isTwoFactorOn) {
                            "Aktiviert • Authenticator-App verknüpft"
                        } else {
                            "Zusätzlicher Schutz bei der Anmeldung"
                        },
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onClick = {
                            if (!isTwoFactorOn) {
                                onNavigateTo2FaSetup()
                            } else {
                                showDisable2FaDialog = true
                            }
                        },
                        trailing = {
                            MaterialYouSwitch(
                                checked = isTwoFactorOn,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onNavigateTo2FaSetup()
                                    } else {
                                        showDisable2FaDialog = true
                                    }
                                },
                                contentDescription = "Zwei-Faktor-Authentifizierung"
                            )
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

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 3: Intelligente Zurück-Geste (Predictive Back)
                    SettingsItemRow(
                        title = "Intelligente „Zurück“-Geste",
                        subtitle = "Vorausschauende Wischgesten &\nAnimationen (Predictive Back)",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = isPredictiveBackOn,
                                onCheckedChange = {
                                    isPredictiveBackOn = it
                                    onPredictiveBackChanged(it)
                                },
                                contentDescription = "Intelligente Zurück-Geste"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Predictive Back Handler für offene Zeit-Dialoge
    PredictiveBackContainer(
        enabled = showPickTimeDialog || showCustomPickTimeDialog,
        onBack = {
            showPickTimeDialog = false
            showCustomPickTimeDialog = false
        }
    ) {}

    // Dialog 1: Vordefinierte Zeiten (picktime.png)
    if (showPickTimeDialog) {
        PickReminderTimeDialog(
            selectedOption = selectedReminderTime,
            onOptionSelected = { option ->
                selectedReminderTime = option
                TaskNotificationManager.reminderLeadTime = option
                TaskNotificationManager.checkAndNotify()
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
                TaskNotificationManager.reminderLeadTime = result
                TaskNotificationManager.checkAndNotify()
                showCustomPickTimeDialog = false
            },
            onDismissRequest = { showCustomPickTimeDialog = false },
            useMaterialYou = materialYouEnabled
        )
    }

    // Dialog 3: 2FA Deaktivierung bestätigen
    if (showDisable2FaDialog) {
        AlertDialog(
            onDismissRequest = { showDisable2FaDialog = false },
            title = {
                Text(
                    text = "2FA deaktivieren?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = getQuicksandFontFamily(),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Möchtest du die Zwei-Faktor-Authentifizierung für deinen Account wirklich deaktivieren?\n\nDein Account ist danach nur noch mit deinem Passwort geschützt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisable2FaDialog = false
                        onDisable2Fa()
                    }
                ) {
                    Text(
                        text = "Deaktivieren",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisable2FaDialog = false }) {
                    Text(
                        text = "Abbrechen",
                        color = textPrimary
                    )
                }
            }
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
            fontWeight = FontWeight.Normal,
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
                if (onClick != null) Modifier.handCursor().clickable(onClick = onClick) else Modifier
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
                    fontWeight = FontWeight.Normal,
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
        modifier = modifier
            .handCursor()
            .semantics { this.contentDescription = contentDescription },
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        )
    )
}
