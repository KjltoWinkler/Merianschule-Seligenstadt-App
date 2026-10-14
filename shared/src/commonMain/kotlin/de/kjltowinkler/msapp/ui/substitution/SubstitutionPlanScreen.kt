package de.kjltowinkler.msapp.ui.substitution

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.data.substitution.SubstitutionStateManager
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.components.AppSwitch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

enum class SubstitutionFilter(val label: String, val hasStar: Boolean = false) {
    ALL("Alle"),
    MARKED("Markiert", hasStar = true),
    CANCELLATION("Entfall"),
    SUBSTITUTION("Vertretung"),
    RELOCATION("Verlegung")
}

@Composable
fun SwipeableSubstitutionItem(
    entry: SubstitutionEntry,
    cardShape: Shape,
    onDismissed: (SubstitutionEntry) -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    animateOnDismiss: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var hasHapticClicked by remember(entry.id) { mutableStateOf(false) }
    var shouldExecuteActionOnRelease by remember(entry.id) { mutableStateOf(false) }
    var isItemLeaving by remember(entry.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentEntry by rememberUpdatedState(entry)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val currentAnimateOnDismiss by rememberUpdatedState(animateOnDismiss)

    var actionTriggered by remember(entry.id) { mutableStateOf(false) }

    // Wisch-Schwellenwert auf 35 % setzen (genau wie in Gmail)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { dismissValue ->
            // Wird aufgerufen, wenn der Nutzer den Finger hebt (Loslassen):
            if ((dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) && !actionTriggered) {
                actionTriggered = true
                if (currentAnimateOnDismiss) {
                    // Im "Markiert"-Tab: Die Karte gleitet vollständig bis an den Rand hinaus und der Platz schrumpft zusammen
                    coroutineScope.launch {
                        delay(180)
                        isItemLeaving = true
                        delay(220)
                        currentOnDismissed(currentEntry)
                    }
                    true // Erlaubt SwipeToDismissBox, vollständig aus dem Bildschirm zu sliden
                } else {
                    // In den anderen Tabs federt die Karte nach der Markierung zurück
                    currentOnDismissed(currentEntry)
                    coroutineScope.launch {
                        delay(350)
                        actionTriggered = false
                    }
                    false
                }
            } else {
                false
            }
        }
    )

    // --- GENAU BEIM ERREICHEN DES THRESHOLDS: Haptischer Klick als Bestätigung ---
    val isThresholdReached = dismissState.targetValue != SwipeToDismissBoxValue.Settled

    LaunchedEffect(isThresholdReached) {
        if (isThresholdReached && !hasHapticClicked) {
            hasHapticClicked = true
            shouldExecuteActionOnRelease = true
            try {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            } catch (_: Exception) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else if (!isThresholdReached) {
            // Wenn der Nutzer wieder zurückzieht (Abbruch vor dem Loslassen)
            hasHapticClicked = false
            shouldExecuteActionOnRelease = false
        }
    }

    AnimatedVisibility(
        visible = !isItemLeaving,
        enter = expandVertically(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 250)
        ),
        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(220))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                // Dynamisches Icon-Verhalten wie in Gmail
                val progress = dismissState.progress
                val iconScale = (progress * 1.25f).coerceIn(0.6f, 1.15f)
                val iconAlpha = (progress * 2.5f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    if (direction != SwipeToDismissBoxValue.Settled) {
                        Icon(
                            painter = painterResource(
                                if (entry.isMarked) Res.drawable.ic_star_outline else Res.drawable.ic_star_filled
                            ),
                            contentDescription = if (entry.isMarked) "Entmarkieren" else "Markieren",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    alpha = iconAlpha
                                }
                        )
                    }
                }
            }
        ) {
            SubstitutionItemCard(
                entry = entry,
                shape = cardShape,
                onClick = onItemClick,
                onToggleMark = { toggled -> onDismissed(toggled) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionPlanScreen(
    modifier: Modifier = Modifier,
    dateText: String = remember { de.kjltowinkler.msapp.getCurrentFormattedDate() }
) {
    val options = SubstitutionFilter.entries
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedFilter = options.getOrNull(selectedIndex) ?: SubstitutionFilter.ALL

    // State für den Umschalter (z.B. Kommende Tage / Heute)
    var isUpcoming by rememberSaveable { mutableStateOf(false) }

    var allEntries by remember { mutableStateOf<List<SubstitutionEntry>>(emptyList()) }
    var selectedEntryForDetails by remember { mutableStateOf<SubstitutionEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            delay(800)
            val loaded = SubstitutionRepository.loadSubstitutions()
            SubstitutionStateManager.initMarked(loaded)
            allEntries = loaded.map { entry ->
                entry.copy(
                    isNew = SubstitutionStateManager.isNew(entry.id),
                    isMarked = SubstitutionStateManager.isMarked(entry.id)
                )
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        val loaded = SubstitutionRepository.loadSubstitutions()
        SubstitutionStateManager.initMarked(loaded)
        allEntries = loaded.map { entry ->
            entry.copy(
                isNew = SubstitutionStateManager.isNew(entry.id),
                isMarked = SubstitutionStateManager.isMarked(entry.id)
            )
        }
    }

    val filteredEntries = remember(allEntries, selectedFilter, isUpcoming) {
        val byType = when (selectedFilter) {
            SubstitutionFilter.ALL -> allEntries
            SubstitutionFilter.MARKED -> allEntries.filter { it.isMarked }
            SubstitutionFilter.CANCELLATION -> allEntries.filter { it.type.equals("Entfall", ignoreCase = true) }
            SubstitutionFilter.SUBSTITUTION -> allEntries.filter { it.type.equals("Vertretung", ignoreCase = true) }
            SubstitutionFilter.RELOCATION -> allEntries.filter { it.type.equals("Verlegung", ignoreCase = true) }
        }

        if (selectedFilter == SubstitutionFilter.MARKED) {
            byType
        } else {
            byType.filter { entry ->
                val isNextDay = entry.dayLabel.contains("Morgen", ignoreCase = true)
                if (isUpcoming) {
                    isNextDay
                } else {
                    !isNextDay
                }
            }
        }
    }

    val groupedEntries = remember(filteredEntries, selectedFilter) {
        if (selectedFilter == SubstitutionFilter.MARKED) {
            // Sortiere und gruppiere markierte Einträge nach Monat und Jahr (z.B. "Oktober 2026")
            val monthNames = listOf(
                "Januar", "Februar", "März", "April", "Mai", "Juni",
                "Juli", "August", "September", "Oktober", "November", "Dezember"
            )

            fun extractMonthYear(entry: SubstitutionEntry): Pair<String, String> {
                // 1. Aus ISO date (z.B. "2026-10-12")
                val dateParts = entry.date.split("-")
                if (dateParts.size >= 2) {
                    val year = dateParts[0]
                    val monthInt = dateParts[1].toIntOrNull()
                    if (monthInt != null && monthInt in 1..12) {
                        val sortKey = "$year-${dateParts[1]}"
                        val label = "${monthNames[monthInt - 1]} $year"
                        return sortKey to label
                    }
                }

                // 2. Fallback aus dayLabel (z.B. "Heute, 13. Oktober", "Morgen, 14. Oktober")
                for ((idx, name) in monthNames.withIndex()) {
                    if (entry.dayLabel.contains(name, ignoreCase = true)) {
                        val monthNum = (idx + 1).toString().padStart(2, '0')
                        val year = if (dateParts.isNotEmpty() && dateParts[0].length == 4) dateParts[0] else "2026"
                        return "$year-$monthNum" to "$name $year"
                    }
                }

                return "9999-99" to "Markierte Einträge"
            }

            filteredEntries
                .sortedByDescending { it.date.ifEmpty { it.id } }
                .groupBy { extractMonthYear(it).second }
        } else {
            filteredEntries.groupBy { it.dayLabel }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // --- BUTTON GROUP IN EINER REIHE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, filter ->
                    ToggleButton(
                        checked = selectedIndex == index,
                        onCheckedChange = { selectedIndex = index },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        if (filter.hasStar) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_star_filled),
                                contentDescription = null,
                                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        }
                        Text(
                            text = filter.label,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = filteredEntries.isEmpty(),
                transitionSpec = {
                    if (targetState) {
                        // Empty-State card blendet sanft ein mit dezentem Scale-Up
                        (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.94f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                    } else {
                        fadeIn(animationSpec = tween(300))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                    }
                },
                label = "EmptyOrContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    // Leer-Zustand Card (Bleibt identisch mit integriertem AppSwitch)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val cardHeaderTitle = if (selectedFilter == SubstitutionFilter.MARKED) {
                                "Markierte Einträge"
                            } else {
                                dateText
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cardHeaderTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFAEB6AB),
                                    textAlign = TextAlign.Start
                                )

                                if (selectedFilter != SubstitutionFilter.MARKED) {
                                    AppSwitch(
                                        checked = isUpcoming,
                                        onCheckedChange = { isUpcoming = it }
                                    )
                                }
                            }

                            val emptyTitle = when (selectedFilter) {
                                SubstitutionFilter.ALL -> "Noch keine Vertretungen"
                                SubstitutionFilter.MARKED -> "Noch keine Markierungen"
                                SubstitutionFilter.CANCELLATION -> "Keine Unterrichtsausfälle"
                                SubstitutionFilter.SUBSTITUTION -> "Keine Vertretungsstunden"
                                SubstitutionFilter.RELOCATION -> "Keine Raumverlegungen"
                            }

                            val emptySubtitle = when (selectedFilter) {
                                SubstitutionFilter.ALL -> "Aktuell findet der gesamte\nUnterricht nach regulärem\nStundenplan statt."
                                SubstitutionFilter.MARKED -> "Wenn du einen Eintrag markierst,\nwird er dir hier angezeigt."
                                SubstitutionFilter.CANCELLATION -> "Für die gewählte Woche liegen\nkeine Ausfallstunden vor."
                                SubstitutionFilter.SUBSTITUTION -> "Aktuell müssen keine Stunden\nvertreten werden."
                                SubstitutionFilter.RELOCATION -> "Alle Stunden finden in den regulär\nvorgesehenen Räumen statt."
                            }

                            val emptyGraphic = when (selectedFilter) {
                                SubstitutionFilter.ALL -> Res.drawable.substitution_plan_empty
                                SubstitutionFilter.MARKED -> Res.drawable.marked_substitutions_character
                                SubstitutionFilter.CANCELLATION -> Res.drawable.substitution_plan_entfall
                                SubstitutionFilter.SUBSTITUTION -> Res.drawable.substitution_plan_vertretung
                                SubstitutionFilter.RELOCATION -> Res.drawable.substitution_plan_verlegung
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Image(
                                painter = painterResource(emptyGraphic),
                                contentDescription = emptyTitle,
                                modifier = Modifier.size(236.dp),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(38.dp))

                            Text(
                                text = emptyTitle,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE4EDE0),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = emptySubtitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 22.sp,
                                color = Color(0xFFA8B0A5),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(34.dp))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- LISTE DER VERTRETUNGSEINTRÄGE ---
                        groupedEntries.entries.forEachIndexed { groupIndex, (dayLabel, entriesForDay) ->
                            val sectionTitle = dayLabel

                            if (groupIndex == 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sectionTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFAEB6AB),
                                        textAlign = TextAlign.Start
                                    )

                                    if (selectedFilter != SubstitutionFilter.MARKED) {
                                        AppSwitch(
                                            checked = isUpcoming,
                                            onCheckedChange = { isUpcoming = it }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = sectionTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFAEB6AB),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, top = 6.dp, bottom = 10.dp),
                                    textAlign = TextAlign.Start
                                )
                            }

                            entriesForDay.forEachIndexed { index, entry ->
                                val cardShape = when {
                                    entriesForDay.size == 1 -> RoundedCornerShape(20.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 5.dp)
                                    index == entriesForDay.lastIndex -> RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                    else -> RoundedCornerShape(5.dp)
                                }

                                // Jedes Item wird in eine separate Komponente gekapselt
                                key(entry.id) {
                                    SwipeableSubstitutionItem(
                                        entry = entry,
                                        cardShape = cardShape,
                                        animateOnDismiss = (selectedFilter == SubstitutionFilter.MARKED),
                                        onDismissed = { swipedEntry ->
                                            val previousMarkedState = swipedEntry.isMarked
                                            val newMarked = !previousMarkedState
                                            SubstitutionStateManager.setMarked(swipedEntry.id, newMarked)

                                            // Eintrag aktualisieren
                                            allEntries = allEntries.map {
                                                if (it.id == swipedEntry.id) it.copy(isMarked = newMarked) else it
                                            }

                                            // Snackbar mit "Rückgängig" wie in Gmail anzeigen
                                            coroutineScope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                val result = snackbarHostState.showSnackbar(
                                                    message = if (newMarked) "Eintrag markiert" else "Markierung entfernt",
                                                    actionLabel = "Rückgängig",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    SubstitutionStateManager.setMarked(swipedEntry.id, previousMarkedState)
                                                    allEntries = allEntries.map {
                                                        if (it.id == swipedEntry.id) it.copy(isMarked = previousMarkedState) else it
                                                    }
                                                }
                                            }
                                        },
                                        onItemClick = {
                                            SubstitutionStateManager.markAsRead(entry.id)
                                            allEntries = allEntries.map {
                                                if (it.id == entry.id) it.copy(isNew = false) else it
                                            }
                                            selectedEntryForDetails = entry.copy(isNew = false)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

    // Details Infosheet (Bottom Sheet)
    selectedEntryForDetails?.let { detailEntry ->
        // Synchronisiere Zustand falls zwischenzeitlich markiert wurde
        val currentEntry = allEntries.find { it.id == detailEntry.id } ?: detailEntry

        SubstitutionDetailBottomSheet(
            entry = currentEntry,
            onDismissRequest = {
                selectedEntryForDetails = null
            },
            onToggleMark = {
                val previousMarkedState = currentEntry.isMarked
                val newMarked = !previousMarkedState
                SubstitutionStateManager.setMarked(currentEntry.id, newMarked)
                allEntries = allEntries.map {
                    if (it.id == currentEntry.id) it.copy(isMarked = newMarked) else it
                }
                selectedEntryForDetails = currentEntry.copy(isMarked = newMarked)

                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = if (newMarked) "Eintrag markiert" else "Markierung entfernt",
                        actionLabel = "Rückgängig",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        SubstitutionStateManager.setMarked(currentEntry.id, previousMarkedState)
                        allEntries = allEntries.map {
                            if (it.id == currentEntry.id) it.copy(isMarked = previousMarkedState) else it
                        }
                        if (selectedEntryForDetails?.id == currentEntry.id) {
                            selectedEntryForDetails = currentEntry.copy(isMarked = previousMarkedState)
                        }
                    }
                }
            },
            onShare = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Eintrag geteilt", duration = SnackbarDuration.Short)
                }
            },
            onSaveAsTask = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Als Aufgabe gespeichert", duration = SnackbarDuration.Short)
                }
            }
        )
    }
}

@Preview
@Composable
private fun SubstitutionPlanScreenPreview() {
    AppTheme(darkTheme = true) {
        SubstitutionPlanScreen()
    }
}