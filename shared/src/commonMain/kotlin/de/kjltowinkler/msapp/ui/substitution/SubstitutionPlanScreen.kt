package de.kjltowinkler.msapp.ui.substitution

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.getTodayDate
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
    animateOnDismiss: Boolean = false,
    onDragActiveChange: ((Boolean) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var hasHapticClicked by remember(entry.id) { mutableStateOf(false) }
    var shouldExecuteActionOnRelease by remember(entry.id) { mutableStateOf(false) }
    var isItemLeaving by remember(entry.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentEntry by rememberUpdatedState(entry)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val currentAnimateOnDismiss by rememberUpdatedState(animateOnDismiss)
    val currentOnDragActiveChange by rememberUpdatedState(onDragActiveChange)

    var actionTriggered by remember(entry.id) { mutableStateOf(false) }
    var latestOffset by remember(entry.id) { mutableFloatStateOf(0f) }

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

    LaunchedEffect(dismissState) {
        snapshotFlow {
            try {
                dismissState.requireOffset()
            } catch (_: Exception) {
                0f
            }
        }.collect { offset ->
            val wasDragging = latestOffset != 0f
            val isDragging = offset != 0f
            latestOffset = offset
            // Parent über Drag-Status informieren, damit der Pager gesperrt werden kann
            if (wasDragging != isDragging) {
                currentOnDragActiveChange?.invoke(isDragging)
            }
        }
    }

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

private fun parseIsoDate(isoString: String): SimpleDate? {
    val parts = isoString.split("-")
    if (parts.size == 3) {
        val y = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        val d = parts[2].toIntOrNull()
        if (y != null && m != null && d != null) {
            return SimpleDate(y, m, d)
        }
    }
    return null
}

private fun formatDayHeader(dateIso: String, fallbackLabel: String, todayIso: String, tomorrowIso: String): String {
    val date = parseIsoDate(dateIso)
    val monthNames = listOf(
        "", "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )
    val weekdays = listOf("Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag")

    val baseLabel = if (date != null) {
        val w = weekdays.getOrElse(date.dayOfWeek) { "" }
        val m = monthNames.getOrElse(date.monthNumber) { "" }
        "$w, ${date.dayOfMonth}. $m"
    } else {
        fallbackLabel
    }

    return when (dateIso) {
        todayIso -> "Heute, $baseLabel"
        tomorrowIso -> "Morgen, $baseLabel"
        else -> baseLabel
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionPlanScreen(
    modifier: Modifier = Modifier,
    dateText: String = remember { de.kjltowinkler.msapp.getCurrentFormattedDate() }
) {
    val options = remember { SubstitutionFilter.entries }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { options.size })
    val tabScrollState = rememberScrollState()

    // State für den Umschalter (z.B. Kommende Tage / Heute)
    var isUpcoming by rememberSaveable { mutableStateOf(false) }

    // Wird true, wenn der Nutzer gerade einen Eintrag wischt → Pager-Scrolling sperren
    var isAnyItemDragging by remember { mutableStateOf(false) }

    val initialEntries = remember { SubstitutionRepository.getInitialOrCachedEntries() }
    var allEntries by remember {
        mutableStateOf(
            initialEntries.map { entry ->
                entry.copy(
                    isNew = SubstitutionStateManager.isNew(entry.id),
                    isMarked = SubstitutionStateManager.isMarked(entry.id)
                )
            }
        )
    }
    var selectedEntryForDetails by remember { mutableStateOf<SubstitutionEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Scrollt die Tab-Leiste nur dann nach, wenn die Tabs nicht alle gleichzeitig sichtbar sind.
    // Nutzt den tatsächlichen ScrollState.maxValue, damit Tab-Breitenunterschiede kein Problem sind.
    LaunchedEffect(pagerState.currentPage) {
        if (tabScrollState.maxValue > 0) {
            val fraction = pagerState.currentPage.toFloat() / (options.size - 1).coerceAtLeast(1)
            val targetScroll = (fraction * tabScrollState.maxValue).toInt()
            tabScrollState.animateScrollTo(targetScroll)
        }
    }

    var isInitialLoading by remember { mutableStateOf(allEntries.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            try {
                val loaded = SubstitutionRepository.loadSubstitutions(forceRefresh = true)
                de.kjltowinkler.msapp.data.substitution.SubstitutionSyncManager.initializeBaseline(loaded)
                SubstitutionStateManager.initMarked(loaded)
                allEntries = loaded.map { entry ->
                    entry.copy(
                        isNew = SubstitutionStateManager.isNew(entry.id),
                        isMarked = SubstitutionStateManager.isMarked(entry.id)
                    )
                }
            } catch (_: Exception) {
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (allEntries.isEmpty()) {
            isInitialLoading = true
        }
        val loaded = SubstitutionRepository.loadSubstitutions()
        de.kjltowinkler.msapp.data.substitution.SubstitutionSyncManager.initializeBaseline(loaded)
        SubstitutionStateManager.initMarked(loaded)
        allEntries = loaded.map { entry ->
            entry.copy(
                isNew = SubstitutionStateManager.isNew(entry.id),
                isMarked = SubstitutionStateManager.isMarked(entry.id)
            )
        }
        isInitialLoading = false

        de.kjltowinkler.msapp.data.substitution.SubstitutionSyncManager.changeEvents.collect { diff ->
            val updated = SubstitutionRepository.getInitialOrCachedEntries()
            SubstitutionStateManager.initMarked(updated)
            allEntries = updated.map { entry ->
                entry.copy(
                    isNew = SubstitutionStateManager.isNew(entry.id),
                    isMarked = SubstitutionStateManager.isMarked(entry.id)
                )
            }
            snackbarHostState.showSnackbar(
                message = diff.summary,
                duration = SnackbarDuration.Short
            )
        }
    }

    val today = remember { getTodayDate() }
    val todayIso = remember(today) { today.toIsoString() }
    val tomorrowIso = remember(today) { today.plusDays(1).toIsoString() }

    val distinctDates = remember(allEntries) {
        allEntries.mapNotNull { it.date.takeIf { d -> d.isNotBlank() } }.distinct().sorted()
    }
    val isWeekend = remember(today) { today.dayOfWeek == 0 || today.dayOfWeek == 6 }

    val heuteDate = remember(distinctDates, todayIso, isWeekend) {
        if (todayIso in distinctDates) {
            todayIso
        } else if (isWeekend && distinctDates.isNotEmpty()) {
            distinctDates[0]
        } else {
            todayIso
        }
    }

    val morgenDate = remember(distinctDates, heuteDate, tomorrowIso) {
        if (heuteDate in distinctDates) {
            val idx = distinctDates.indexOf(heuteDate)
            distinctDates.getOrNull(idx + 1) ?: tomorrowIso
        } else {
            tomorrowIso
        }
    }

    val onItemDismissed: (SubstitutionEntry) -> Unit = { swipedEntry ->
        val previousMarkedState = swipedEntry.isMarked
        val newMarked = !previousMarkedState
        SubstitutionStateManager.setMarked(swipedEntry.id, newMarked)

        allEntries = allEntries.map {
            if (it.id == swipedEntry.id) it.copy(isMarked = newMarked) else it
        }

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
    }

    val onItemClicked: (SubstitutionEntry) -> Unit = { entry ->
        SubstitutionStateManager.markAsRead(entry.id)
        allEntries = allEntries.map {
            if (it.id == entry.id) it.copy(isNew = false) else it
        }
        selectedEntryForDetails = entry.copy(isNew = false)
    }

    fun getGroupedMarkedEntries(filtered: List<SubstitutionEntry>): Map<String, List<SubstitutionEntry>> {
        val monthNames = listOf(
            "Januar", "Februar", "März", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Dezember"
        )

        fun extractMonthYear(entry: SubstitutionEntry): Pair<String, String> {
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

            for ((idx, name) in monthNames.withIndex()) {
                if (entry.dayLabel.contains(name, ignoreCase = true)) {
                    val monthNum = (idx + 1).toString().padStart(2, '0')
                    val year = if (dateParts.isNotEmpty() && dateParts[0].length == 4) dateParts[0] else "2026"
                    return "$year-$monthNum" to "$name $year"
                }
            }

            return "9999-99" to "Markierte Einträge"
        }

        return filtered
            .sortedByDescending { it.date.ifEmpty { it.id } }
            .groupBy { extractMonthYear(it).second }
    }

    if (isInitialLoading && allEntries.isEmpty()) {
        de.kjltowinkler.msapp.ui.components.AppLoadingView(modifier = modifier.fillMaxSize())
    } else {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // --- 1. BUTTON GROUP IN EINER REIHE (Oben fixiert) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScrollState)
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, filter ->
                    ToggleButton(
                        checked = pagerState.currentPage == index,
                        onCheckedChange = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
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

            // --- 2. PULL TO REFRESH & HORIZONTAL PAGER ---
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    // Pager-Scrolling sperren, wenn der Nutzer gerade einen Eintrag wischt
                    userScrollEnabled = !isAnyItemDragging
                ) { page ->
                    val pageFilter = options[page]
                    val entriesForTab = remember(allEntries, pageFilter) {
                        when (pageFilter) {
                            SubstitutionFilter.ALL -> allEntries
                            SubstitutionFilter.MARKED -> allEntries.filter { it.isMarked }
                            SubstitutionFilter.CANCELLATION -> allEntries.filter { it.type.equals("Entfall", ignoreCase = true) }
                            SubstitutionFilter.SUBSTITUTION -> allEntries.filter { it.type.equals("Vertretung", ignoreCase = true) }
                            SubstitutionFilter.RELOCATION -> allEntries.filter { it.type.equals("Verlegung", ignoreCase = true) }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if (pageFilter == SubstitutionFilter.MARKED) {
                            val markedGroups = remember(entriesForTab) {
                                getGroupedMarkedEntries(entriesForTab)
                            }

                            if (entriesForTab.isEmpty()) {
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Markierte Einträge",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFAEB6AB),
                                                textAlign = TextAlign.Start
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))
                                        Image(
                                            painter = painterResource(Res.drawable.marked_substitutions_character),
                                            contentDescription = "Noch keine Markierungen",
                                            modifier = Modifier.size(236.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.height(38.dp))
                                        Text(
                                            text = "Noch keine Markierungen",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE4EDE0),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "Wenn du einen Eintrag markierst,\nwird er dir hier angezeigt.",
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
                                    markedGroups.entries.forEach { (_, entriesForDay) ->
                                        Text(
                                            text = "Markierte Einträge",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFAEB6AB),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 14.dp, top = 6.dp, bottom = 10.dp),
                                            textAlign = TextAlign.Start
                                        )

                                        entriesForDay.forEachIndexed { index, entry ->
                                            val cardShape = when {
                                                entriesForDay.size == 1 -> RoundedCornerShape(20.dp)
                                                index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 5.dp)
                                                index == entriesForDay.lastIndex -> RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                                else -> RoundedCornerShape(5.dp)
                                            }

                                            key(entry.id) {
                                                SwipeableSubstitutionItem(
                                                    entry = entry,
                                                    cardShape = cardShape,
                                                    animateOnDismiss = true,
                                                    onDragActiveChange = { dragging -> isAnyItemDragging = dragging },
                                                    onDismissed = onItemDismissed,
                                                    onItemClick = { onItemClicked(entry) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }
                            }
                        } else {
                            val targetDate = if (isUpcoming) morgenDate else heuteDate
                            val focusDayTitle = remember(targetDate, dateText, todayIso, tomorrowIso) {
                                formatDayHeader(targetDate, dateText, todayIso, tomorrowIso)
                            }

                            val focusEntries = remember(entriesForTab, targetDate, isUpcoming) {
                                entriesForTab.filter { it.date == targetDate || (it.date.isBlank() && !isUpcoming) }
                            }
                            val pastDays: List<Pair<String, List<SubstitutionEntry>>> = remember(entriesForTab, heuteDate) {
                                entriesForTab
                                    .filter { it.date.isNotBlank() && it.date < heuteDate }
                                    .groupBy { it.date }
                                    .toList()
                                    .sortedByDescending { it.first }
                            }

                            if (focusEntries.isEmpty() && pastDays.isEmpty()) {
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = focusDayTitle,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFAEB6AB),
                                                textAlign = TextAlign.Start
                                            )

                                            AppSwitch(
                                                checked = isUpcoming,
                                                onCheckedChange = { isUpcoming = it }
                                            )
                                        }

                                        val emptyTitle = when (pageFilter) {
                                            SubstitutionFilter.ALL -> "Noch keine Vertretungen"
                                            SubstitutionFilter.CANCELLATION -> "Keine Unterrichtsausfälle"
                                            SubstitutionFilter.SUBSTITUTION -> "Keine Vertretungsstunden"
                                            SubstitutionFilter.RELOCATION -> "Keine Raumverlegungen"
                                            SubstitutionFilter.MARKED -> "Noch keine Markierungen"
                                        }

                                        val emptySubtitle = when (pageFilter) {
                                            SubstitutionFilter.ALL -> "Aktuell findet der gesamte\nUnterricht nach regulärem\nStundenplan statt."
                                            SubstitutionFilter.CANCELLATION -> "Für die gewählte Woche liegen\nkeine Ausfallstunden vor."
                                            SubstitutionFilter.SUBSTITUTION -> "Aktuell müssen keine Stunden\nvertreten werden."
                                            SubstitutionFilter.RELOCATION -> "Alle Stunden finden in den regulär\nvorgesehenen Räumen statt."
                                            SubstitutionFilter.MARKED -> "Wenn du einen Eintrag markierst,\nwird er dir hier angezeigt."
                                        }

                                        val emptyGraphic = when (pageFilter) {
                                            SubstitutionFilter.ALL -> Res.drawable.substitution_plan_empty
                                            SubstitutionFilter.CANCELLATION -> Res.drawable.substitution_plan_entfall
                                            SubstitutionFilter.SUBSTITUTION -> Res.drawable.substitution_plan_vertretung
                                            SubstitutionFilter.RELOCATION -> Res.drawable.substitution_plan_verlegung
                                            SubstitutionFilter.MARKED -> Res.drawable.marked_substitutions_character
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

                                        val lastUpdated = SubstitutionRepository.lastUpdatedText
                                        if (lastUpdated.isNotBlank()) {
                                            Text(
                                                text = lastUpdated,
                                                fontSize = 12.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = Color(0xFFA8B0A5),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(34.dp))
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // --- 1. HAUPTTAG (Heute oder Morgen je nach Switch) ---
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = focusDayTitle,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFAEB6AB),
                                            textAlign = TextAlign.Start
                                        )

                                        AppSwitch(
                                            checked = isUpcoming,
                                            onCheckedChange = { isUpcoming = it }
                                        )
                                    }

                                    if (focusEntries.isEmpty()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_check),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = if (isUpcoming) "Keine Einträge für morgen" else "Keine Einträge für heute",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        focusEntries.forEachIndexed { index, entry ->
                                            val cardShape = when {
                                                focusEntries.size == 1 -> RoundedCornerShape(20.dp)
                                                index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 5.dp)
                                                index == focusEntries.lastIndex -> RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                                else -> RoundedCornerShape(5.dp)
                                            }

                                            key(entry.id) {
                                                SwipeableSubstitutionItem(
                                                    entry = entry,
                                                    cardShape = cardShape,
                                                    animateOnDismiss = false,
                                                    onDragActiveChange = { dragging -> isAnyItemDragging = dragging },
                                                    onDismissed = onItemDismissed,
                                                    onItemClick = { onItemClicked(entry) }
                                                )
                                            }
                                        }
                                    }

                                    // --- 2. VERGANGENE EINTRÄGE (Weiter unten) ---
                                    if (pastDays.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Vergangene Einträge",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFAEB6AB)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            )
                                        }

                                        pastDays.forEach { (dayDate, entriesForDay) ->
                                            val dayHeader = formatDayHeader(dayDate, entriesForDay.firstOrNull()?.dayLabel ?: dayDate, todayIso, tomorrowIso)
                                            Text(
                                                text = dayHeader,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFAEB6AB),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp),
                                                textAlign = TextAlign.Start
                                            )

                                            entriesForDay.forEachIndexed { index, entry ->
                                                val cardShape = when {
                                                    entriesForDay.size == 1 -> RoundedCornerShape(20.dp)
                                                    index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 5.dp)
                                                    index == entriesForDay.lastIndex -> RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                                    else -> RoundedCornerShape(5.dp)
                                                }

                                                key(entry.id) {
                                                    SwipeableSubstitutionItem(
                                                        entry = entry,
                                                        cardShape = cardShape,
                                                        animateOnDismiss = false,
                                                        onDragActiveChange = { dragging -> isAnyItemDragging = dragging },
                                                        onDismissed = onItemDismissed,
                                                        onItemClick = { onItemClicked(entry) }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))
                                        }
                                    }

                                    // Hinweis zur letzten Aktualisierung (aus dem Schulportal Hessen)
                                    val lastUpdated = SubstitutionRepository.lastUpdatedText
                                    if (lastUpdated.isNotBlank()) {
                                        Text(
                                            text = lastUpdated,
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = Color(0xFFA8B0A5),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp, bottom = 12.dp)
                                                .clickable {
                                                    de.kjltowinkler.msapp.util.PlatformNotifier.sendNotification(
                                                        title = "Vertretungsplan: Unterrichtsentfall",
                                                        summary = "Heute • 3. Std Deutsch (Af) entfällt",
                                                        details = listOf(
                                                            "• Heute, 3. Std Deutsch (Af): Entfall ersatzlos",
                                                            "• Heute, 5. Std Klassenlehrerstunde (Tho): Entfall"
                                                        )
                                                    )
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Test-Push-Benachrichtigung gesendet!")
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    //- Details Infosheet (Bottom Sheet)
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
}

@Preview
@Composable
private fun SubstitutionPlanScreenPreview() {
    AppTheme(darkTheme = true) {
        SubstitutionPlanScreen()
    }
}