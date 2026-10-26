package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.substitution.SubstitutionStateManager
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.theme.DesktopBackground
import de.kjltowinkler.msapp.theme.DesktopTheme
import de.kjltowinkler.msapp.ui.substitution.SubstitutionPlanScreen
import de.kjltowinkler.msapp.ui.tasks.TasksScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Vollständiges Desktop-Layout für die Merianschule App:
 * 1. Feste linke Sidebar mit Schulbranding, Navigation & Benutzerstatus
 * 2. Einheitlicher Top-Header mit Titel, Datum, Schnellsuche & Shortcuts
 * 3. Flüssiger Wechsel zwischen den Hauptbereichen mit Animation
 * 4. Desktop-Dialoge anstelle mobiler Bottom-Sheets
 * 5. Tastenkürzel (Strg+K für Suche, Strg+N für neue Aufgaben, Strg+1-4 für Tabs)
 */
@Composable
fun DesktopHomeScreen(
    accountName: String = "Sebastian Wagner",
    gradeAndSchool: String = "Klasse 9aR • Realschule",
    avatarType: String = "initials",
    avatarColorHex: Long = 0xFF395A3CL,
    onProfileClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var showNewTaskDialog by remember { mutableStateOf(false) }
    var isNewTaskStarred by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Live Badge-Zähler
    val tasksCount = TasksRepository.tasks.count { !it.isCompleted }
    val substitutionsCount = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries.count {
        SubstitutionStateManager.isNew(it.id) || SubstitutionStateManager.isMarked(it.id)
    }

    DesktopTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DesktopBackground)
                .onKeyEvent { keyEvent ->
                    // Tastatur-Shortcuts für Desktop Power-User
                    if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                        when (keyEvent.key) {
                            Key.K -> {
                                searchFocusRequester.requestFocus()
                                true
                            }
                            Key.N -> {
                                showNewTaskDialog = true
                                true
                            }
                            Key.One -> {
                                selectedTab = 0
                                true
                            }
                            Key.Two -> {
                                selectedTab = 1
                                true
                            }
                            Key.Three -> {
                                selectedTab = 2
                                true
                            }
                            Key.Four -> {
                                selectedTab = 3
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // --- 1. LINKE NAVIGATIONS-SIDEBAR ---
                DesktopNavigationSidebar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    accountName = accountName,
                    gradeAndSchool = gradeAndSchool,
                    avatarType = avatarType,
                    avatarColorHex = avatarColorHex,
                    onProfileClicked = onProfileClicked,
                    substitutionBadgeCount = substitutionsCount,
                    tasksBadgeCount = tasksCount
                )

                // --- 2. HAUPT-INHALTSBEREICH ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DesktopBackground)
                ) {
                    // Top-Header mit Titeln, Suche & Aktionen
                    DesktopTopHeader(
                        selectedTab = selectedTab,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchFocusRequester = searchFocusRequester,
                        isRefreshing = isRefreshing,
                        onRefreshClicked = {
                            isRefreshing = true
                            coroutineScope.launch {
                                delay(900)
                                isRefreshing = false
                            }
                        },
                        onPrimaryActionClicked = {
                            showNewTaskDialog = true
                        }
                    )

                    // Hauptbereich für den aktiven Tab mit weichem Animationsübergang
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) +
                                 scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                                    .togetherWith(fadeOut(animationSpec = tween(180)))
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> {
                                    // Startseite / Dashboard
                                    DesktopStartseiteDashboard(
                                        accountName = accountName,
                                        onNavigateToTab = { selectedTab = it },
                                        onAddNewTask = { showNewTaskDialog = true }
                                    )
                                }
                                1 -> {
                                    // Stundenplan Desktop Ansicht
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Stundenplan Wochenübersicht",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Hier wird das interaktive Wochenraster (Montag bis Freitag) dargestellt.",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                                2 -> {
                                    // Vertretungsplan
                                    SubstitutionPlanScreen(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                3 -> {
                                    // Aufgaben
                                    TasksScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onAddTaskClicked = { isStarred ->
                                            isNewTaskStarred = isStarred
                                            showNewTaskDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. MODAL-DIALOG FÜR NEUE AUFGABE (DESKTOP) ---
            if (showNewTaskDialog) {
                DesktopNewTaskDialog(
                    initialIsStarred = isNewTaskStarred,
                    onDismissRequest = {
                        showNewTaskDialog = false
                        isNewTaskStarred = false
                    },
                    onSaveTask = { newTask ->
                        TasksRepository.addTask(newTask)
                    }
                )
            }
        }
    }
}
