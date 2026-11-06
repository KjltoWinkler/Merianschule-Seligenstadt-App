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
import de.kjltowinkler.msapp.data.update.AppUpdateManager
import de.kjltowinkler.msapp.data.update.UpdateCheckStatus
import de.kjltowinkler.msapp.data.update.UpdateDownloadStatus
import de.kjltowinkler.msapp.util.PlatformAppUpdater
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
    val currentSettings = de.kjltowinkler.msapp.data.settings.SettingsRepository.settings
    val delayWarningsEnabled = currentSettings.delayWarningsEnabled
    val relaxedDismissalEnabled = currentSettings.relaxedDismissalEnabled
    val vplanUpdatesEnabled = currentSettings.vplanUpdatesEnabled
    val taskReminderEnabled = currentSettings.taskReminderEnabled
    val darkThemeEnabled = currentSettings.isDarkTheme
    val materialYouEnabled = currentSettings.isDynamicColor
    val isPredictiveBackOn = currentSettings.isPredictiveBackEnabled
    val selectedReminderTime = currentSettings.reminderLeadTime

    var showPickTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomPickTimeDialog by rememberSaveable { mutableStateOf(false) }
    var isTwoFactorOn by remember(isTwoFactorEnabled) { mutableStateOf(isTwoFactorEnabled) }
    var showDisable2FaDialog by remember { mutableStateOf(false) }

    val checkStatus by AppUpdateManager.checkStatus
    val downloadStatus by AppUpdateManager.downloadStatus
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdates()
    }

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
                                onCheckedChange = { de.kjltowinkler.msapp.data.settings.SettingsRepository.updateDelayWarnings(it) },
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
                                onCheckedChange = { de.kjltowinkler.msapp.data.settings.SettingsRepository.updateRelaxedDismissal(it) },
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
                    SettingsItemRow(
                        title = "Vertretungsplan-Updates",
                        subtitle = "Sofort bei Entfall oder Raumänderung\nbenachrichtigen",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            MaterialYouSwitch(
                                checked = vplanUpdatesEnabled,
                                onCheckedChange = { de.kjltowinkler.msapp.data.settings.SettingsRepository.updateVplanUpdates(it) },
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
                                        de.kjltowinkler.msapp.data.settings.SettingsRepository.updateTaskReminder(it)
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
                                    onDarkThemeChanged(it)
                                    de.kjltowinkler.msapp.data.settings.SettingsRepository.updateDarkTheme(it)
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
                                    onMaterialYouChanged(it)
                                    de.kjltowinkler.msapp.data.settings.SettingsRepository.updateDynamicColor(it)
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
                                    onPredictiveBackChanged(it)
                                    de.kjltowinkler.msapp.data.settings.SettingsRepository.updatePredictiveBack(it)
                                },
                                contentDescription = "Intelligente Zurück-Geste"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BEREICH 6: App & Updates ---
            CategoryHeader(title = "App & Updates", color = categoryHeader)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Item 1: Versionsinfo
                    val availableUpdate = (checkStatus as? UpdateCheckStatus.UpdateAvailable)?.updateInfo

                    SettingsItemRow(
                        title = "Merianschule App",
                        subtitle = "Version ${PlatformAppUpdater.currentVersionName} (Build ${PlatformAppUpdater.currentVersionCode})",
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        trailing = {
                            when {
                                availableUpdate != null -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Neu: v${availableUpdate.versionName} (Build ${availableUpdate.versionCode})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                checkStatus is UpdateCheckStatus.UpToDate -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_check_circle),
                                            contentDescription = "Aktuell",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Aktuell",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, color = dividerHorizontal)

                    // Item 2: Update suchen & installieren
                    val currentCheck = checkStatus
                    val currentDownload = downloadStatus

                    val (updateTitle, updateSubtitle) = when {
                        currentDownload is UpdateDownloadStatus.Downloading -> {
                            "Lade Update herunter..." to "${currentDownload.progressPercent}% abgeschlossen" +
                                (if (currentDownload.totalBytes > 0) " (${currentDownload.bytesDownloaded / 1024 / 1024} MB / ${currentDownload.totalBytes / 1024 / 1024} MB)" else "")
                        }
                        currentDownload is UpdateDownloadStatus.ReadyToInstall -> {
                            "Update bereit zur Installation" to "Die APK wurde heruntergeladen. Tippe hier, um das Update jetzt zu installieren."
                        }
                        currentDownload is UpdateDownloadStatus.Error -> {
                            "Download fehlgeschlagen" to "${currentDownload.message} (Tippen zum Wiederholen)"
                        }
                        currentCheck is UpdateCheckStatus.Checking -> {
                            "Suche nach Updates..." to "Serververbindung wird geprüft..."
                        }
                        currentCheck is UpdateCheckStatus.UpdateAvailable -> {
                            val notes = currentCheck.updateInfo.releaseNotes.ifBlank { "Tippe hier, um die neueste Version zu installieren." }
                            val sizeText = if (currentCheck.updateInfo.fileSizeBytes > 0) " (${currentCheck.updateInfo.fileSizeBytes / 1024 / 1024} MB)" else ""
                            "Neues Update: v${currentCheck.updateInfo.versionName}$sizeText" to notes
                        }
                        currentCheck is UpdateCheckStatus.UpToDate -> {
                            "Nach Updates suchen" to "Du hast bereits die neueste Version installiert."
                        }
                        currentCheck is UpdateCheckStatus.Error -> {
                            "Prüfung fehlgeschlagen" to "${currentCheck.message} (Tippen zum Wiederholen)"
                        }
                        else -> {
                            "Nach Updates suchen" to "Prüft auf neue Versionen und Updates auf dem Server."
                        }
                    }

                    SettingsItemRow(
                        title = updateTitle,
                        subtitle = updateSubtitle,
                        titleColor = if (currentCheck is UpdateCheckStatus.UpdateAvailable || currentDownload is UpdateDownloadStatus.ReadyToInstall) MaterialTheme.colorScheme.primary else textPrimary,
                        subtitleColor = textSecondary,
                        onClick = {
                            when {
                                currentDownload is UpdateDownloadStatus.Downloading -> {}
                                currentDownload is UpdateDownloadStatus.ReadyToInstall -> {
                                    if (!PlatformAppUpdater.canInstallUnknownApps()) {
                                        showPermissionDialog = true
                                    } else {
                                        AppUpdateManager.installApk(currentDownload.localPath)
                                    }
                                }
                                currentCheck is UpdateCheckStatus.UpdateAvailable -> {
                                    if (!PlatformAppUpdater.canInstallUnknownApps()) {
                                        showPermissionDialog = true
                                    } else {
                                        AppUpdateManager.startDownloadAndInstall(currentCheck.updateInfo)
                                    }
                                }
                                currentCheck is UpdateCheckStatus.Checking -> {}
                                else -> {
                                    AppUpdateManager.checkForUpdates()
                                }
                            }
                        },
                        trailing = {
                            when {
                                currentDownload is UpdateDownloadStatus.Downloading -> {
                                    CircularProgressIndicator(
                                        progress = { currentDownload.progressPercent / 100f },
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                currentDownload is UpdateDownloadStatus.ReadyToInstall -> {
                                    Button(
                                        onClick = {
                                            if (!PlatformAppUpdater.canInstallUnknownApps()) {
                                                showPermissionDialog = true
                                            } else {
                                                AppUpdateManager.installApk(currentDownload.localPath)
                                            }
                                        },
                                        modifier = Modifier.handCursor()
                                    ) {
                                        Text("Installieren")
                                    }
                                }
                                currentCheck is UpdateCheckStatus.Checking -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                currentCheck is UpdateCheckStatus.UpdateAvailable -> {
                                    Button(
                                        onClick = {
                                            if (!PlatformAppUpdater.canInstallUnknownApps()) {
                                                showPermissionDialog = true
                                            } else {
                                                AppUpdateManager.startDownloadAndInstall(currentCheck.updateInfo)
                                            }
                                        },
                                        modifier = Modifier.handCursor()
                                    ) {
                                        Text("Aktualisieren")
                                    }
                                }
                                else -> {
                                    IconButton(
                                        onClick = { AppUpdateManager.checkForUpdates() },
                                        modifier = Modifier.handCursor()
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_refresh),
                                            contentDescription = "Nach Updates suchen",
                                            tint = textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
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
                de.kjltowinkler.msapp.data.settings.SettingsRepository.updateReminderLeadTime(option)
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
                de.kjltowinkler.msapp.data.settings.SettingsRepository.updateReminderLeadTime(result)
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

    // Dialog 4: Berechtigung für App-Updates anfordern
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = "Installation erlauben",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = getQuicksandFontFamily(),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Damit die Merianschule App das Update direkt installieren kann, muss in den Android-Einstellungen die Berechtigung „Unbekannte Apps installieren“ für diese App aktiviert werden.\n\nMöchtest du die Einstellungen jetzt öffnen?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        PlatformAppUpdater.openUnknownAppSourcesSettings()
                    }
                ) {
                    Text("Einstellungen öffnen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
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
