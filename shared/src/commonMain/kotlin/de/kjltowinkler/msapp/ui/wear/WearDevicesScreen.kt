package de.kjltowinkler.msapp.ui.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.wear.PairedWatchInfo
import de.kjltowinkler.msapp.data.wear.PlatformWearPairing
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_add
import merianschuleseligenstadtapp.shared.generated.resources.ic_arrow_back
import merianschuleseligenstadtapp.shared.generated.resources.ic_delete
import merianschuleseligenstadtapp.shared.generated.resources.ic_more_vert
import merianschuleseligenstadtapp.shared.generated.resources.ic_notes_plus_logo
import merianschuleseligenstadtapp.shared.generated.resources.ic_refresh
import merianschuleseligenstadtapp.shared.generated.resources.ic_watch
import org.jetbrains.compose.resources.painterResource

/**
 * Google-inspiriertes Smartwatch-Verwaltungsmenü (Einstellungen -> Smartwatch verbinden).
 *
 * Zeigt verbundene und gekoppelte Wear-OS-Smartwatches an, ermöglicht das erneute
 * Synchronisieren des Kontos, das Entfernen gekoppelter Uhren sowie das Koppeln
 * einer neuen Smartwatch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearDevicesScreen(
    onBackClicked: () -> Unit,
    onPairNewWatchClicked: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var watches by remember { mutableStateOf<List<PairedWatchInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var watchToRemove by remember { mutableStateOf<PairedWatchInfo?>(null) }

    fun refreshWatches() {
        isLoading = true
        coroutineScope.launch {
            try {
                watches = PlatformWearPairing.getWatchDevices()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshWatches()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Smartwatch",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Zurück"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            statusMessage = null
                            refreshWatches()
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_refresh),
                            contentDescription = "Aktualisieren"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hinweis-Karte oben (Google Companion Style)
            item(key = "header_info") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_notes_plus_logo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notes+ auf Wear OS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Stundenplan, Vertretungen und Aufgaben direkt auf deiner Uhr synchronisieren.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                    )
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                item(key = "status_banner") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // Sektions-Überschrift
            item(key = "section_devices") {
                Text(
                    text = "Meine Smartwatches",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                )
            }

            if (!isLoading && watches.isEmpty()) {
                item(key = "empty_devices") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_watch),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Keine Smartwatch verbunden",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verbinde deine Wear OS Uhr per Bluetooth oder WLAN und tippe unten auf „Neue Smartwatch koppeln“.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = watches,
                    key = { it.nodeId }
                ) { watch ->
                    WearDeviceCard(
                        watch = watch,
                        onPairOrResyncClick = {
                            val token = BackendApiClient.authToken
                            val user = BackendApiClient.currentUser
                            if (token.isNullOrBlank() || user == null) {
                                statusMessage = "Bitte melde dich zuerst in der App an."
                            } else {
                                isLoading = true
                                statusMessage = null
                                coroutineScope.launch {
                                    val result = PlatformWearPairing.sendSessionToWatch(token, user)
                                    statusMessage = if (result.isSuccess) {
                                        "${watch.displayName} wurde synchronisiert."
                                    } else {
                                        result.exceptionOrNull()?.message
                                            ?: "Synchronisierung fehlgeschlagen."
                                    }
                                    watches = PlatformWearPairing.getWatchDevices()
                                    isLoading = false
                                }
                            }
                        },
                        onRemoveClick = {
                            watchToRemove = watch
                        }
                    )
                }
            }

            // Primärer Button: Neue Smartwatch koppeln
            item(key = "pair_new_button") {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onPairNewWatchClicked,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Neue Smartwatch koppeln",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Bestätigungsdialog zum Entfernen einer gekoppelten Uhr
    val pendingRemove = watchToRemove
    if (pendingRemove != null) {
        AlertDialog(
            onDismissRequest = { watchToRemove = null },
            title = {
                Text(
                    text = "Smartwatch entfernen?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Möchtest du „${pendingRemove.displayName}“ aus der Notes+ App entfernen? Du kannst die Uhr jederzeit wieder koppeln."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingRemove
                        watchToRemove = null
                        isLoading = true
                        coroutineScope.launch {
                            PlatformWearPairing.unpairWatch(target.nodeId)
                            watches = PlatformWearPairing.getWatchDevices()
                            statusMessage = "${target.displayName} wurde entfernt."
                            isLoading = false
                        }
                    }
                ) {
                    Text(
                        text = "Entfernen",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { watchToRemove = null }) {
                    Text(text = "Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun WearDeviceCard(
    watch: PairedWatchInfo,
    onPairOrResyncClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (watch.isConnected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_watch),
                    contentDescription = null,
                    tint = if (watch.isConnected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = watch.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (watch.isConnected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val statusLabel = buildString {
                        append(if (watch.isConnected) "Verbunden" else "Nicht in Reichweite")
                        if (watch.isPairedInApp) {
                            append(" • Gekoppelt")
                        } else if (watch.isConnected) {
                            append(" • Nicht gekoppelt")
                        }
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (watch.isConnected && !watch.isPairedInApp) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onPairOrResyncClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Koppeln",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more_vert),
                            contentDescription = "Optionen"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (watch.isConnected) {
                            DropdownMenuItem(
                                text = { Text("Konto erneut senden") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_refresh),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onPairOrResyncClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Smartwatch entfernen",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRemoveClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
