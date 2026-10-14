package de.kjltowinkler.msapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.theme.FigmaMenuBackgroundDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Startseite Screen entsprechend dem Entwurf in `FigmaDesigns/startseite.png`.
 * 
 * Beinhaltet:
 * 1. Material 3 Top Search Bar nach AOSP `SimpleSearchBarSample` (mit `rememberSearchBarState()`,
 *    `rememberTextFieldState()`, Suchergebnissen und Profil-Avatar).
 * 2. Material 3 Expressive `ShortNavigationBar` nach AOSP `ShortNavigationBarSample`
 *    mit Reitern: Startseite, Stundenpl., Vertretugspl., Aufgaben.
 * 3. Platzhalterbereich für den Hauptinhalt des gewählten Tabs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    accountName: String = "Sebastian Wagner",
    onProfileClicked: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(3) }

    val navItems = listOf(
        "Startseite" to (Res.drawable.ic_nav_home_filled to Res.drawable.ic_nav_home_outlined),
        "Stundenpl." to (Res.drawable.ic_nav_schedule to Res.drawable.ic_nav_schedule),
        "Vertretugspl." to (Res.drawable.ic_nav_event_note to Res.drawable.ic_nav_event_note),
        "Aufgaben" to (Res.drawable.ic_nav_checklist_rtl to Res.drawable.ic_nav_checklist_rtl)
    )

    // SearchBar States gemäß SimpleSearchBarSample
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = {
                Text(
                    text = "Schulapp",
                    modifier = Modifier.clearAndSetSemantics {},
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(
                        onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Zurück",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_search),
                        contentDescription = "Suchen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.avatar_sample),
                        contentDescription = "Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            colors = SearchBarDefaults.inputFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }

    var showNewTaskBottomSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ShortNavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                navItems.forEachIndexed { index, (label, icons) ->
                    val isSelected = selectedTab == index
                    ShortNavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(if (isSelected) icons.first else icons.second),
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        colors = ShortNavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                // Top SearchBar platzieren
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SearchBar(
                        state = searchBarState,
                        inputField = inputField,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SearchBarDefaults.colors(
                            containerColor = FigmaMenuBackgroundDark
                        )
                    )
                }

                // Content für den ausgewählten Tab
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> {
                            var isStartseiteRefreshing by remember { mutableStateOf(false) }
                            val startseitePullState = rememberPullToRefreshState()
                            PullToRefreshBox(
                                isRefreshing = isStartseiteRefreshing,
                                onRefresh = {
                                    isStartseiteRefreshing = true
                                    scope.launch {
                                        delay(1000)
                                        isStartseiteRefreshing = false
                                    }
                                },
                                state = startseitePullState,
                                modifier = Modifier.fillMaxSize(),
                                indicator = {
                                    PullToRefreshDefaults.Indicator(
                                        state = startseitePullState,
                                        isRefreshing = isStartseiteRefreshing,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Willkommen auf der Startseite",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        1 -> {
                            var isTimetableRefreshing by remember { mutableStateOf(false) }
                            val timetablePullState = rememberPullToRefreshState()
                            PullToRefreshBox(
                                isRefreshing = isTimetableRefreshing,
                                onRefresh = {
                                    isTimetableRefreshing = true
                                    scope.launch {
                                        delay(1000)
                                        isTimetableRefreshing = false
                                    }
                                },
                                state = timetablePullState,
                                modifier = Modifier.fillMaxSize(),
                                indicator = {
                                    PullToRefreshDefaults.Indicator(
                                        state = timetablePullState,
                                        isRefreshing = isTimetableRefreshing,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Stundenplan",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        2 -> {
                            de.kjltowinkler.msapp.ui.substitution.SubstitutionPlanScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            de.kjltowinkler.msapp.ui.tasks.TasksScreen(
                                modifier = Modifier.fillMaxSize(),
                                onAddTaskClicked = { showNewTaskBottomSheet = true }
                            )
                        }
                    }
                }
            }

            // Vollbild SearchBar Overlay beim Ausklappen
            ExpandedFullScreenSearchBar(
                state = searchBarState,
                inputField = inputField,
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val searchSuggestions = listOf(
                        "Vertretungsplan heute",
                        "Klausurentermine",
                        "Hausaufgaben Mathe",
                        "Lehrerzimmer",
                        "Schulkalender"
                    )
                    Text(
                        text = "Vorschläge",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    searchSuggestions.forEach { suggestion ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = suggestion,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    textFieldState.setTextAndPlaceCursorAtEnd(suggestion)
                                    scope.launch { searchBarState.animateToCollapsed() }
                                }
                        )
                    }
                }
            }
        }
    }

        if (showNewTaskBottomSheet) {
            de.kjltowinkler.msapp.ui.tasks.NewTaskBottomSheet(
                onDismissRequest = { showNewTaskBottomSheet = false },
                onSaveTask = { newTask ->
                    de.kjltowinkler.msapp.data.task.TasksRepository.addTask(newTask)
                }
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    AppTheme(darkTheme = true) {
        HomeScreen()
    }
}
