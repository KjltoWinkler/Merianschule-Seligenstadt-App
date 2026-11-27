package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.settings.SettingsRepository
import de.kjltowinkler.msapp.data.settings.UserSettings
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.ui.account.getAvatarColorForName
import de.kjltowinkler.msapp.ui.account.getInitialsForName
import de.kjltowinkler.msapp.ui.account.rememberAvatarBitmap
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.GoogleSansRounded
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Einstellungs-Screen für Wear OS (Google Pixel Watch).
 *
 * Orientiert sich an den Pixel Watch System-Einstellungen mit Material 3 Expressive:
 * - Klare Gruppierung über ListSubheader
 * - Native Wear M3 SwitchButtons für schnelle Toggles
 * - Direkte Synchronisation über SettingsRepository mit dem Backend/Smartphone
 */
@Composable
fun WearSettingsScreen(
    onPairPhoneClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val settings = SettingsRepository.settings
    val currentUser = BackendApiClient.currentUser
    val userClass = TimetableRepository.getEffectiveUserClass()

    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (BackendApiClient.isLoggedIn) {
            SettingsRepository.syncWithServer()
        }
    }

    WearSettingsScreenContent(
        settings = settings,
        isLoggedIn = BackendApiClient.isLoggedIn,
        accountTitle = currentUser?.fullName?.takeIf { it.isNotBlank() }
            ?: currentUser?.username?.takeIf { it.isNotBlank() }
            ?: "Nicht gekoppelt",
        avatarUrl = currentUser?.avatarUrl,
        avatarColorHex = currentUser?.avatarColorHex ?: 0L,
        userClass = userClass,
        isSyncing = isSyncing,
        syncStatusText = syncStatusText,
        onVplanUpdatesChange = { SettingsRepository.updateVplanUpdates(it) },
        onTaskReminderChange = { SettingsRepository.updateTaskReminder(it) },
        onDelayWarningsChange = { SettingsRepository.updateDelayWarnings(it) },
        onDynamicColorChange = { SettingsRepository.updateDynamicColor(it) },
        onPairPhoneClick = onPairPhoneClick,
        onSyncNowClick = {
            if (!isSyncing) {
                isSyncing = true
                syncStatusText = "Synchronisiere…"
                coroutineScope.launch {
                    try {
                        if (BackendApiClient.isLoggedIn) {
                            SettingsRepository.syncWithServer()
                            TasksRepository.reloadAndSync()
                            TimetableRepository.loadFromServer()
                        }
                        SubstitutionRepository.loadSubstitutions(forceRefresh = true)
                        syncStatusText = "Aktuell"
                        delay(2000)
                        syncStatusText = null
                    } catch (_: Throwable) {
                        syncStatusText = "Fehler beim Sync"
                    } finally {
                        isSyncing = false
                    }
                }
            }
        }
    )
}

@Composable
fun WearSettingsScreenContent(
    settings: UserSettings,
    isLoggedIn: Boolean,
    accountTitle: String,
    avatarUrl: String? = null,
    avatarColorHex: Long = 0L,
    userClass: String,
    isSyncing: Boolean,
    syncStatusText: String?,
    onVplanUpdatesChange: (Boolean) -> Unit,
    onTaskReminderChange: (Boolean) -> Unit,
    onDelayWarningsChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPairPhoneClick: () -> Unit,
    onSyncNowClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Text(
                        text = "Einstellungen",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Gruppe: Benachrichtigungen
            item(key = "sub_notifications") {
                WearSettingsSubheader(
                    title = "Benachrichtigungen",
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                )
            }

            item(key = "toggle_vplan") {
                SwitchButton(
                    checked = settings.vplanUpdatesEnabled,
                    onCheckedChange = onVplanUpdatesChange,
                    label = {
                        Text(
                            text = "Vertretungsplan",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Bei Änderungen melden",
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            item(key = "toggle_tasks") {
                SwitchButton(
                    checked = settings.taskReminderEnabled,
                    onCheckedChange = onTaskReminderChange,
                    label = {
                        Text(
                            text = "Aufgaben",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Vor Fälligkeit erinnern",
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            item(key = "toggle_delay") {
                SwitchButton(
                    checked = settings.delayWarningsEnabled,
                    onCheckedChange = onDelayWarningsChange,
                    label = {
                        Text(
                            text = "Schulweg-Warnung",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Bus & Bahn Verspätungen",
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            // Gruppe: Darstellung
            item(key = "sub_appearance") {
                WearSettingsSubheader(
                    title = "Darstellung",
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                )
            }

            item(key = "toggle_dynamic_color") {
                SwitchButton(
                    checked = settings.isDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    label = {
                        Text(
                            text = "Dynamische Farben",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Watchface-Akzent nutzen",
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            // Gruppe: Konto & Synchronisation
            item(key = "sub_account") {
                WearSettingsSubheader(
                    title = "Konto & Sync",
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                )
            }

            item(key = "btn_pair") {
                FilledTonalButton(
                    onClick = onPairPhoneClick,
                    icon = {
                        if (isLoggedIn) {
                            val bitmap = rememberAvatarBitmap(avatarUrl)
                            val initials = remember(accountTitle) { getInitialsForName(accountTitle) }
                            val effectiveColor = if (avatarColorHex != 0L) avatarColorHex else getAvatarColorForName(accountTitle)

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(effectiveColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Profilbild von $accountTitle",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_person),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = if (isLoggedIn) accountTitle else "Smartphone koppeln",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (isLoggedIn) {
                                "Klasse $userClass • Neu koppeln"
                            } else {
                                "Mit Handy-App verbinden"
                            },
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            item(key = "btn_sync") {
                FilledTonalButton(
                    onClick = onSyncNowClick,
                    enabled = !isSyncing,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Jetzt synchronisieren",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = syncStatusText?.let { status ->
                        {
                            Text(
                                text = status,
                                fontFamily = GoogleSansRounded,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            // Gruppe: App-Info
            item(key = "sub_about") {
                WearSettingsSubheader(
                    title = "Über",
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                )
            }

            item(key = "about_card") {
                Card(
                    onClick = {},
                    enabled = false,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Merianschule App",
                            fontFamily = GoogleSansRounded,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Wear OS • Version 1.0",
                            fontFamily = GoogleSansRounded,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun WearSettingsSubheader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontFamily = GoogleSansRounded,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun WearSettingsScreenPreview() {
    WearAppTheme(useDynamicColors = false) {
        WearSettingsScreenContent(
            settings = UserSettings(),
            isLoggedIn = true,
            accountTitle = "Sebastian Wagner",
            userClass = "09AR",
            isSyncing = false,
            syncStatusText = null,
            onVplanUpdatesChange = {},
            onTaskReminderChange = {},
            onDelayWarningsChange = {},
            onDynamicColorChange = {},
            onPairPhoneClick = {},
            onSyncNowClick = {}
        )
    }
}
