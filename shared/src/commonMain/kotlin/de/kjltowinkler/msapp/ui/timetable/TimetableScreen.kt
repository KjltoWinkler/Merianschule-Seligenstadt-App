package de.kjltowinkler.msapp.ui.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.timetable.TeacherInfo
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.theme.FigmaLinkGreen
import de.kjltowinkler.msapp.theme.FigmaPrimaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen(
    modifier: Modifier = Modifier,
    useMaterialYou: Boolean = true,
    onAddTaskForSubject: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val days = TimetableDay.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { days.size })
    val tabScrollState = rememberScrollState()

    val listStates = List(days.size) { index ->
        key(index) {
            rememberLazyListState()
        }
    }

    val isFabExpanded by remember {
        derivedStateOf {
            val currentListState = listStates.getOrNull(pagerState.currentPage)
            currentListState?.let {
                it.firstVisibleItemIndex == 0 && it.firstVisibleItemScrollOffset <= 10
            } ?: true
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Sheet-States
    var selectedTeacherInfo by remember { mutableStateOf<TeacherInfo?>(null) }
    var selectedLessonForDetail by remember { mutableStateOf<TimetableLesson?>(null) }
    var showNewEntrySheet by remember { mutableStateOf(false) }

    // Automatisch zum ausgewählten Button scrollen, wenn die Seite gewechselt wird
    LaunchedEffect(pagerState.currentPage) {
        val buttonWidthEstimate = 100 // Ungefähre Breite pro Button in dp
        tabScrollState.animateScrollTo(
            (pagerState.currentPage * buttonWidthEstimate).coerceAtLeast(0)
        )
    }

    val teacherLinkColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        FigmaLinkGreen
    }

    val fabContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        FigmaPrimaryDark
    }

    val fabContentColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Color.White
    }

    val dividerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    } else {
        Color(0xFF2C352E)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. DASSELBE TAB-SYSTEM WIE IM VERTRETUNGSPLAN SCREEN (Connected Button Group) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScrollState)
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEachIndexed { index, day ->
                    ToggleButton(
                        checked = pagerState.currentPage == index,
                        onCheckedChange = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            days.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        Text(
                            text = day.displayName,
                            softWrap = false
                        )
                    }
                }
            }

            // --- 2. PULL TO REFRESH & HORIZONTAL PAGER FÜR WOCHENTAGE ---
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        delay(700)
                        isRefreshing = false
                    }
                },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                    beyondViewportPageCount = 1
                ) { page ->
                    val day = days[page]
                    val lessons = TimetableRepository.getLessonsForDay(day)

                    if (lessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Kein Unterricht für ${day.displayName} eingetragen",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        val listState = listStates.getOrElse(page) { rememberLazyListState() }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                        ) {
                            itemsIndexed(
                                items = lessons,
                                key = { _, lesson -> lesson.id }
                            ) { index, lesson ->
                                // Karte für die Unterrichtsstunde (Komplett schwarz "000000")
                                TimetableLessonCard(
                                    lesson = lesson,
                                    teacherLinkColor = teacherLinkColor,
                                    onCardClick = { selectedLessonForDetail = lesson },
                                    onTeacherClick = { code ->
                                        selectedTeacherInfo = TimetableRepository.getTeacherInfo(code)
                                    }
                                )

                                // Trenner / Wechselpause zur nächsten Stunde
                                if (lesson.breakLabel != null) {
                                    TimetableBreakDivider(
                                        breakLabel = lesson.breakLabel,
                                        dividerColor = dividerColor,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else if (index < lessons.lastIndex) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

// --- 3. EXTENDED FLOATING ACTION BUTTON (FAB) unten rechts mit Material You ---
        ExtendedFloatingActionButton(
            onClick = { showNewEntrySheet = true },
            expanded = isFabExpanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = fabContainerColor,
            contentColor = fabContentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = "Hinzufügen",
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = "Hinzufügen",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        )
    } // <-- SCHLIESST DAS Box(modifier = modifier.fillMaxSize())

    // --- 4. BOTTOM SHEETS ---
    selectedTeacherInfo?.let { teacher ->
        TeacherDetailBottomSheet(
            teacher = teacher,
            onDismissRequest = { selectedTeacherInfo = null },
            useMaterialYou = useMaterialYou
        )
    }

    selectedLessonForDetail?.let { lesson ->
        TimetableDetailBottomSheet(
            lesson = lesson,
            onDismissRequest = { selectedLessonForDetail = null },
            onTeacherClick = { code ->
                selectedTeacherInfo = TimetableRepository.getTeacherInfo(code)
            },
            onAddTaskForSubject = { subjectName ->
                onAddTaskForSubject?.invoke(subjectName)
            },
            useMaterialYou = useMaterialYou
        )
    }

    if (showNewEntrySheet) {
        val currentDay = days[pagerState.currentPage]
        NewTimetableEntryBottomSheet(
            initialDay = currentDay,
            onDismissRequest = { showNewEntrySheet = false },
            onEntryAdded = { _, _ -> },
            useMaterialYou = useMaterialYou
        )
    }
}

@Composable
fun TimetableLessonCard(
    lesson: TimetableLesson,
    teacherLinkColor: Color,
    onCardClick: () -> Unit,
    onTeacherClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF000000)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Kopfzeile: "1. Stunde · 07:40 - 08:25"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${lesson.periodNumber}. Stunde",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = lesson.timeRange,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Fachname: "Deutsch", "Mathematik", ...
            Text(
                text = lesson.subject,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fußzeile: Ort & Lehrer (nicht bold)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Raum-Pin Icon & Name
                Icon(
                    painter = painterResource(Res.drawable.ic_location_on),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = lesson.room,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )

                // Trennpunkt
                Text(
                    text = "  ·  ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )

                // Unterstrichenes Lehrer-Kürzel mit dynamischer Link-Farbe (nicht bold)
                Text(
                    text = "Lehrer: ${lesson.teacherCode}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = teacherLinkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.clickable {
                        onTeacherClick(lesson.teacherCode)
                    }
                )
            }
        }
    }
}

/**
 * Pausen-Trenner zwischen Unterrichtsstunden mit horizontalen Linien und Uhr-Icon
 * z. B. "--- (clock) 5 Min. Wechselpause ---"
 */
@Composable
fun TimetableBreakDivider(
    breakLabel: String,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_access_time),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = breakLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor,
            thickness = 1.dp
        )
    }
}
