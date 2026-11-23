package de.kjltowinkler.msapp.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import de.kjltowinkler.msapp.ui.components.PredictiveBackContainer
import de.kjltowinkler.msapp.ui.components.AppSnackbarManager
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.util.PlatformNetworkMonitor
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import kotlinx.coroutines.async
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accountName: String = "Sebastian Wagner",
    accountEmail: String = "sebastian.wagner@mssof.onmicrosoft.com",
    className: String = "9aR",
    schoolType: String = "Realschule",
    classTeacher: String = "Hr. Thomas",
    studentId: String = "",
    avatarType: String = "initials",
    avatarColorHex: Long = 0xFF395A3CL,
    avatarUrl: String? = null,
    onAvatarChanged: (avatarType: String, avatarColorHex: Long) -> Unit = { _, _ -> },
    onAvatarUrlChanged: ((avatarType: String, avatarColorHex: Long, avatarUrl: String?) -> Unit)? = null,
    onProfileClicked: () -> Unit = {},
    onNotificationsClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    useMaterialYou: Boolean = true
) {
    var showAccountBottomSheet by rememberSaveable { mutableStateOf(false) }

    val handleProfileClick: () -> Unit = {
        showAccountBottomSheet = true
        onProfileClicked()
    }

    LaunchedEffect(Unit) {
        de.kjltowinkler.msapp.data.substitution.SubstitutionSyncManager.startPeriodicSync(intervalMinutes = 5)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            de.kjltowinkler.msapp.ui.desktop.DesktopHomeScreen(
                accountName = accountName,
                gradeAndSchool = "Klasse $className • $schoolType",
                avatarType = avatarType,
                avatarColorHex = avatarColorHex,
                avatarUrl = avatarUrl,
                onProfileClicked = handleProfileClick
            )
        } else {
            MobileHomeScreen(
                accountName = accountName,
                avatarType = avatarType,
                avatarColorHex = avatarColorHex,
                avatarUrl = avatarUrl,
                onProfileClicked = handleProfileClick,
                onNotificationsClicked = onNotificationsClicked,
                useMaterialYou = useMaterialYou
            )
        }
    }

    if (showAccountBottomSheet) {
        de.kjltowinkler.msapp.ui.account.AccountBottomSheet(
            onDismissRequest = { showAccountBottomSheet = false },
            accountName = accountName,
            accountEmail = accountEmail,
            className = className,
            schoolType = schoolType,
            classTeacher = classTeacher,
            studentId = studentId,
            avatarType = avatarType,
            avatarColorHex = avatarColorHex,
            avatarUrl = avatarUrl,
            onAvatarChanged = onAvatarChanged,
            onAvatarUrlChanged = onAvatarUrlChanged,
            onSettingsClicked = {
                showAccountBottomSheet = false
                onSettingsClicked()
            },
            onLogoutClicked = {
                showAccountBottomSheet = false
                onLogoutClicked()
            },
            useMaterialYou = useMaterialYou
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MobileHomeScreen(
    accountName: String = "Sebastian Wagner",
    avatarType: String = "initials",
    avatarColorHex: Long = 0xFF395A3CL,
    avatarUrl: String? = null,
    onProfileClicked: () -> Unit = {},
    onNotificationsClicked: () -> Unit = {},
    useMaterialYou: Boolean = true
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var highlightedSubstitutionEntryId by remember { mutableStateOf<String?>(null) }
    var highlightedTaskId by remember { mutableStateOf<String?>(null) }
    var showNeuigkeitenScreen by rememberSaveable { mutableStateOf(false) }
    var selectedArticleItem by remember { mutableStateOf<de.kjltowinkler.msapp.data.news.NewsItem?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showCreateNewsDialog by remember { mutableStateOf(false) }

    val isTeacherOrAdmin = remember {
        val role = de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.role?.uppercase()
        role in listOf("TEACHER", "ADMIN", "LEHRER")
    }

    LaunchedEffect(Unit) {
        de.kjltowinkler.msapp.util.NavigationEvents.targetFlow.collect { target ->
            when (target.route) {
                "news", "neuigkeiten" -> {
                    showNeuigkeitenScreen = true
                }
                "substitution", "vplan" -> {
                    selectedTab = 2
                    highlightedSubstitutionEntryId = target.highlightId
                }
                "tasks", "aufgaben" -> {
                    selectedTab = 3
                    highlightedTaskId = target.highlightId
                }
                "home", "startseite" -> {
                    selectedTab = 0
                }
                "timetable", "stundenplan" -> {
                    selectedTab = 1
                }
            }
        }
    }

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
                    text = "Notes+",
                    modifier = Modifier.clearAndSetSemantics {},
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None
                    )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onNotificationsClicked,
                        modifier = Modifier
                            .size(38.dp)
                            .handCursor()
                    ) {
                        val unreadCount = de.kjltowinkler.msapp.data.notifications.NotificationRepository.unreadCount
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_notifications),
                                contentDescription = "Benachrichtigungen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onProfileClicked)
                            .handCursor(),
                        contentAlignment = Alignment.Center
                    ) {
                        de.kjltowinkler.msapp.ui.account.AccountAvatar(
                            accountName = accountName,
                            avatarType = avatarType,
                            avatarColorHex = avatarColorHex,
                            avatarUrl = avatarUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
    var isNewTaskStarred by remember { mutableStateOf(false) }
    var newTaskTargetListId by remember { mutableStateOf("my_tasks") }
    var isNewTaskSchoolTask by remember { mutableStateOf(false) }
    var newTaskSubject by remember { mutableStateOf("") }
    var showNewListScreen by remember { mutableStateOf(false) }
    var shareListTarget by remember { mutableStateOf<de.kjltowinkler.msapp.data.task.TaskList?>(null) }

    // Hinweis: Die Zurück-Gesten für Unter-Tabs und Suchleiste werden
    // über PredictiveBackContainer mit vorausschauender Animation gesteuert.

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        AppSnackbarManager.bindToNetworkMonitor(this)
        AppSnackbarManager.messages.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel,
                withDismissAction = msg.withDismissAction,
                duration = msg.duration
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.onActionPerformed?.invoke()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState
                )
            },
            floatingActionButton = {
                if (selectedTab == 0 && !showNeuigkeitenScreen) {
                    FloatingActionButton(
                        onClick = {
                            if (isTeacherOrAdmin) {
                                showFabMenu = true
                            } else {
                                isNewTaskStarred = false
                                isNewTaskSchoolTask = false
                                newTaskSubject = ""
                                newTaskTargetListId = "my_tasks"
                                showNewTaskBottomSheet = true
                            }
                        },
                        containerColor = Color(0xFF395A3C),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .handCursor()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = "Hinzufügen",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
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
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSynthesis = androidx.compose.ui.text.font.FontSynthesis.None
                                )
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }

                // Content für den ausgewählten Tab mit intelligenter Predictive-Back-Geste zurück zur Startseite (Tab 0)
                PredictiveBackContainer(
                    enabled = selectedTab != 0 &&
                        searchBarState.currentValue != SearchBarValue.Expanded &&
                        !showNewTaskBottomSheet &&
                        !showNewListScreen &&
                        shareListTarget == null &&
                        !showNeuigkeitenScreen &&
                        selectedArticleItem == null &&
                        !showFabMenu &&
                        !showCreateNewsDialog,
                    onBack = { selectedTab = 0 },
                    maxScaleReduction = 0.08f,
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
                                        if (!PlatformNetworkMonitor.isOnline.value) {
                                            AppSnackbarManager.showOfflineActionMessage("Aktualisierung der Startseite")
                                        }
                                        try {
                                            val minTimeJob = async { delay(400) }
                                            de.kjltowinkler.msapp.data.news.NewsRepository.loadNews(forceRefresh = true)
                                            de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.loadSubstitutions(forceRefresh = true)
                                            if (de.kjltowinkler.msapp.data.backend.BackendApiClient.isLoggedIn) {
                                                de.kjltowinkler.msapp.data.task.TasksRepository.reloadAndSync()
                                                de.kjltowinkler.msapp.data.timetable.TimetableRepository.loadFromServer()
                                            }
                                            try {
                                                de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate()
                                            } catch (_: Throwable) {}
                                            minTimeJob.await()
                                        } finally {
                                            isStartseiteRefreshing = false
                                        }
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val newsList by de.kjltowinkler.msapp.data.news.NewsRepository.newsFlow.collectAsState()

                                    // 1. Das Carousel ganz oben entsprechend FigmaDesigns/mobile/startseite.png
                                    StartseiteCarousel(
                                        newsItems = newsList,
                                        onItemClick = { item -> selectedArticleItem = item },
                                        modifier = Modifier.padding(top = 10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 2. "Das ist neu" Header mit "Alle anzeigen" Button
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Das ist neu",
                                            fontFamily = getQuicksandFontFamily(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TextButton(
                                            onClick = { showNeuigkeitenScreen = true },
                                            modifier = Modifier.handCursor()
                                        ) {
                                            Text(
                                                text = "Alle anzeigen",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_chevron_right),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val updateStatus by de.kjltowinkler.msapp.data.update.AppUpdateManager.checkStatus
                                    if (updateStatus is de.kjltowinkler.msapp.data.update.UpdateCheckStatus.UpdateAvailable) {
                                        val info = (updateStatus as de.kjltowinkler.msapp.data.update.UpdateCheckStatus.UpdateAvailable).updateInfo
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.ic_refresh),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = "Neues Update verfügbar: v${info.versionName}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                if (info.releaseNotes.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = info.releaseNotes,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = {
                                                        de.kjltowinkler.msapp.util.NavigationEvents.emitRoute("settings")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Text("Jetzt aktualisieren")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            de.kjltowinkler.msapp.ui.timetable.TimetableScreen(
                                modifier = Modifier.fillMaxSize(),
                                useMaterialYou = useMaterialYou,
                                onAddTaskForSubject = { subjectName ->
                                    selectedTab = 3
                                    isNewTaskStarred = false
                                    isNewTaskSchoolTask = true
                                    newTaskSubject = subjectName
                                    newTaskTargetListId = "my_tasks"
                                    showNewTaskBottomSheet = true
                                },
                                onNavigateToSubstitution = { entry ->
                                    highlightedSubstitutionEntryId = entry.id
                                    selectedTab = 2
                                    de.kjltowinkler.msapp.util.NavigationEvents.emit("substitution", entry.id)
                                }
                            )
                        }
                        2 -> {
                            de.kjltowinkler.msapp.ui.substitution.SubstitutionPlanScreen(
                                modifier = Modifier.fillMaxSize(),
                                useMaterialYou = useMaterialYou,
                                highlightEntryId = highlightedSubstitutionEntryId,
                                onHighlightHandled = { highlightedSubstitutionEntryId = null }
                            )
                        }
                        else -> {
                            de.kjltowinkler.msapp.ui.tasks.TasksScreen(
                                modifier = Modifier.fillMaxSize(),
                                useMaterialYou = useMaterialYou,
                                highlightTaskId = highlightedTaskId,
                                onHighlightHandled = { highlightedTaskId = null },
                                onAddTaskClicked = { isStarred, targetListId, isSchoolTask ->
                                    isNewTaskStarred = isStarred
                                    newTaskTargetListId = targetListId
                                    isNewTaskSchoolTask = isSchoolTask
                                    showNewTaskBottomSheet = true
                                },
                                onNewListClicked = {
                                    showNewListScreen = true
                                },
                                onShareListClicked = { listToShare ->
                                    shareListTarget = listToShare
                                }
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
                PredictiveBackContainer(
                    enabled = searchBarState.currentValue == SearchBarValue.Expanded,
                    onBack = { scope.launch { searchBarState.animateToCollapsed() } },
                    modifier = Modifier.fillMaxSize()
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
    }

        if (showNewTaskBottomSheet) {
            de.kjltowinkler.msapp.ui.tasks.NewTaskBottomSheet(
                initialIsStarred = isNewTaskStarred,
                initialIsSchoolTask = isNewTaskSchoolTask,
                initialSubject = newTaskSubject,
                targetListId = newTaskTargetListId,
                onDismissRequest = {
                    showNewTaskBottomSheet = false
                    newTaskSubject = ""
                },
                onSaveTask = { newTask ->
                    de.kjltowinkler.msapp.data.task.TasksRepository.addTask(newTask)
                    newTaskSubject = ""
                }
            )
        }

        if (showNewListScreen) {
            de.kjltowinkler.msapp.ui.tasks.NewListScreen(
                onDismissRequest = { showNewListScreen = false },
                onSaveList = { newTitle ->
                    de.kjltowinkler.msapp.data.task.TasksRepository.createPrivateList(newTitle)
                    showNewListScreen = false
                },
                useMaterialYou = useMaterialYou
            )
        }

        AnimatedVisibility(
            visible = shareListTarget != null,
            enter = slideInVertically(
                initialOffsetY = { (it * 0.12f).toInt() },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(240)),
            exit = slideOutVertically(
                targetOffsetY = { (it * 0.12f).toInt() },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180))
        ) {
            shareListTarget?.let { list ->
                de.kjltowinkler.msapp.ui.tasks.ShareListScreen(
                    list = list,
                    onDismissRequest = { shareListTarget = null },
                    useMaterialYou = useMaterialYou
                )
            }
        }

        // Vollbild-Ansicht: "Das ist neu" (NeuigkeitenScreen) entsprechend den Figma-Designs
        AnimatedVisibility(
            visible = showNeuigkeitenScreen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            PredictiveBackContainer(
                enabled = showNeuigkeitenScreen && !showCreateNewsDialog,
                onBack = { showNeuigkeitenScreen = false },
                modifier = Modifier.fillMaxSize()
            ) {
                de.kjltowinkler.msapp.ui.news.NeuigkeitenScreen(
                    onBackClicked = { showNeuigkeitenScreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Vollbild-Ansicht: Ausgewählter Artikel mit Karussell für alle Bilder
        AnimatedVisibility(
            visible = selectedArticleItem != null,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            selectedArticleItem?.let { article ->
                PredictiveBackContainer(
                    enabled = true,
                    onBack = { selectedArticleItem = null },
                    modifier = Modifier.fillMaxSize()
                ) {
                    de.kjltowinkler.msapp.ui.news.NewsArticleScreen(
                        item = article,
                        onBackClicked = { selectedArticleItem = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // FAB Aktionsmenü für Lehrkräfte / Admins
        if (showFabMenu) {
            ModalBottomSheet(
                onDismissRequest = { showFabMenu = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Neu erstellen",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Aufgabe erstellen", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Private Aufgabe oder Schulaufgabe anlegen") },
                        leadingContent = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_add_task),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showFabMenu = false
                                isNewTaskStarred = false
                                isNewTaskSchoolTask = false
                                newTaskSubject = ""
                                newTaskTargetListId = "my_tasks"
                                showNewTaskBottomSheet = true
                            }
                    )

                    ListItem(
                        headlineContent = { Text("Neuigkeit / Event verfassen", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Erscheint im Startseiten-Karussell und unter 'Das ist neu'") },
                        leadingContent = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_tag_label),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showFabMenu = false
                                showCreateNewsDialog = true
                            }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showCreateNewsDialog) {
            de.kjltowinkler.msapp.ui.news.CreateNewsDialog(
                onDismissRequest = { showCreateNewsDialog = false },
                onSave = { request ->
                    scope.launch {
                        de.kjltowinkler.msapp.data.news.NewsRepository.createNews(request)
                        showCreateNewsDialog = false
                    }
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
