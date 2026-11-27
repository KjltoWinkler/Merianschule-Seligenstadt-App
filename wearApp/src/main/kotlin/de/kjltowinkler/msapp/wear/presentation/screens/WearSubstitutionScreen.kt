package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.GoogleSansRounded
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.launch

enum class WearSubstitutionTab(val label: String) {
    TODAY("Heute"),
    UPCOMING("Nächste")
}

/**
 * Vertretungsplan-Screen für Wear OS (Google Pixel Watch).
 *
 * Orientiert sich an Googles Wear OS Material 3 Expressive Guidelines:
 * - TransformingLazyColumn mit ScreenScaffold, ListHeader & EdgeButton
 * - Material 3 Icon-Pill-Tabs für Heute (Uhr) / Nächste (Kalender)
 * - Schneller Umschalter zwischen eigener Klasse und allen Klassen
 */
@Composable
fun WearSubstitutionScreen() {
    val coroutineScope = rememberCoroutineScope()
    var entries by remember {
        mutableStateOf(SubstitutionRepository.getInitialOrCachedEntries())
    }
    var selectedTab by remember { mutableStateOf(WearSubstitutionTab.TODAY) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = SubstitutionRepository.loadSubstitutions(forceRefresh = false)
        if (loaded.isNotEmpty() || entries.isEmpty()) {
            entries = loaded
        }
    }

    val userClass = TimetableRepository.getEffectiveUserClass()
    val todayIso = remember { getTodayDate().toIsoString() }

    val filteredEntries = remember(entries, selectedTab, userClass, todayIso) {
        val normalizedUserClass = userClass.trim().lowercase().removePrefix("0")
        val classFiltered = entries.filter { entry ->
            val normalizedEntryClass = entry.className.lowercase()
            normalizedEntryClass.contains(userClass.lowercase()) ||
                (normalizedUserClass.isNotBlank() && normalizedEntryClass.contains(normalizedUserClass))
        }

        classFiltered.filter { entry ->
            val isTodayEntry = entry.date == todayIso ||
                (entry.date.isBlank() && entry.dayLabel.startsWith("Heute", ignoreCase = true))
            val isUpcomingEntry = (entry.date.isNotBlank() && entry.date > todayIso) ||
                (entry.date.isBlank() && !entry.dayLabel.startsWith("Heute", ignoreCase = true))

            when (selectedTab) {
                WearSubstitutionTab.TODAY -> isTodayEntry
                WearSubstitutionTab.UPCOMING -> isUpcomingEntry
            }
        }
    }

    WearSubstitutionScreenContent(
        entries = filteredEntries,
        userClass = userClass,
        selectedTab = selectedTab,
        lastUpdatedText = SubstitutionRepository.lastUpdatedText,
        isRefreshing = isRefreshing,
        onSelectTab = { selectedTab = it },
        onRefreshClick = {
            if (!isRefreshing) {
                isRefreshing = true
                coroutineScope.launch {
                    try {
                        entries = SubstitutionRepository.loadSubstitutions(forceRefresh = true)
                    } finally {
                        isRefreshing = false
                    }
                }
            }
        }
    )
}

@Composable
fun WearSubstitutionScreenContent(
    entries: List<SubstitutionEntry>,
    userClass: String,
    selectedTab: WearSubstitutionTab,
    lastUpdatedText: String,
    isRefreshing: Boolean,
    onSelectTab: (WearSubstitutionTab) -> Unit,
    onRefreshClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var expandedId by remember { mutableStateOf<String?>(null) }

    val groupedEntries = remember(entries) {
        entries.groupBy { it.dayLabel.ifBlank { it.date.ifBlank { "Aktuell" } } }
    }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onRefreshClick,
                buttonSize = EdgeButtonSize.Small
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Aktualisieren",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Kompakte Google Material 3 Icon-Pill-Tabs: Heute (Uhr) / Nächste (Kalender)
            item(key = "tab_bar") {
                WearSubstitutionTabBar(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // Kopfzeile unter den Tabs (wie in der Pixel Watch Wetter-App)
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Vertretungsplan",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = when {
                            isRefreshing -> "Aktualisiere…"
                            lastUpdatedText.isNotBlank() -> "${selectedTab.label} • $lastUpdatedText"
                            else -> "${selectedTab.label} • Klasse $userClass"
                        }
                        Text(
                            text = subtitle,
                            fontFamily = GoogleSansRounded,
                            fontSize = 11.sp,
                            color = if (isRefreshing) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item(key = "empty") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == WearSubstitutionTab.TODAY) {
                                "Heute keine Vertretungen"
                            } else {
                                "Keine kommenden Vertretungen"
                            },
                            fontFamily = GoogleSansRounded,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Für $userClass liegt aktuell nichts vor.",
                            fontFamily = GoogleSansRounded,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                groupedEntries.forEach { (dayTitle, dayEntries) ->
                    item(key = "day_$dayTitle") {
                        Text(
                            text = dayTitle,
                            fontFamily = GoogleSansRounded,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 2.dp)
                                .transformedHeight(this, transformationSpec)
                        )
                    }

                    items(
                        items = dayEntries,
                        key = { "${dayTitle}_${it.id}" }
                    ) { entry ->
                        val isExpanded = expandedId == entry.id
                        WearSubstitutionCard(
                            entry = entry,
                            showClassName = false,
                            isExpanded = isExpanded,
                            onClick = {
                                expandedId = if (isExpanded) null else entry.id
                            },
                            transformation = SurfaceTransformation(transformationSpec),
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = GoogleSansRounded,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WearSubstitutionCard(
    entry: SubstitutionEntry,
    showClassName: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier
) {
    val isCancelled = entry.type.equals("Entfall", ignoreCase = true)
    val isSubstitution = entry.type.equals("Vertretung", ignoreCase = true)

    val badgeContainer = when {
        isCancelled -> MaterialTheme.colorScheme.error
        isSubstitution -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeContent = when {
        isCancelled -> MaterialTheme.colorScheme.onError
        isSubstitution -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        transformation = transformation,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Kopfzeile: Stunde + Status-Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (entry.isNew) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = buildString {
                            append(entry.period)
                            if (showClassName && entry.className.isNotBlank()) {
                                append(" • ${entry.className}")
                            }
                        },
                        fontFamily = GoogleSansRounded,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                WearStatusBadge(
                    text = entry.type.ifBlank { "Info" },
                    containerColor = badgeContainer,
                    contentColor = badgeContent
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Fachname
            Text(
                text = TimetableRepository.resolveSubjectName(entry.subject).ifBlank { entry.subject },
                fontFamily = GoogleSansRounded,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Details (z.B. "Hr. Thomas entfällt ersatzlos")
            if (entry.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.details,
                    fontFamily = GoogleSansRounded,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Raum / Zusatzinfo
            if (entry.roomInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.roomInfo,
                    fontFamily = GoogleSansRounded,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WearSubstitutionTabBar(
    selectedTab: WearSubstitutionTab,
    onSelectTab: (WearSubstitutionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WearSubstitutionTabItem(
            iconRes = R.drawable.ic_schedule,
            contentDescription = "Heute",
            selected = selectedTab == WearSubstitutionTab.TODAY,
            onClick = { onSelectTab(WearSubstitutionTab.TODAY) }
        )
        WearSubstitutionTabItem(
            iconRes = R.drawable.ic_calendar_event,
            contentDescription = "Nächste",
            selected = selectedTab == WearSubstitutionTab.UPCOMING,
            onClick = { onSelectTab(WearSubstitutionTab.UPCOMING) }
        )
    }
}

@Composable
private fun WearSubstitutionTabItem(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "tabContainer"
    ).value
    val iconColor = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "tabIcon"
    ).value

    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 28.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun WearSubstitutionScreenPreview() {
    WearAppTheme(useDynamicColors = false) {
        WearSubstitutionScreenContent(
            entries = SubstitutionRepository.defaultEntries.take(4),
            userClass = "09AR",
            selectedTab = WearSubstitutionTab.TODAY,
            lastUpdatedText = "Stand: 07:45 Uhr",
            isRefreshing = false,
            onSelectTab = {},
            onRefreshClick = {}
        )
    }
}
