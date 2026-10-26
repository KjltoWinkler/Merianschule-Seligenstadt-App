package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import de.kjltowinkler.msapp.data.task.TaskCategory
import de.kjltowinkler.msapp.data.task.TaskList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.PlatformBackHandler
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

private sealed class TasksTab {
    data object Starred : TasksTab()
    data object School : TasksTab()
    data class Private(val list: TaskList) : TasksTab()
}

private enum class TaskListContentState {
    EMPTY,
    ALL_DONE,
    HAS_ACTIVE
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    onAddTaskClicked: ((Boolean) -> Unit)? = null,
    onNewListClicked: (() -> Unit)? = null
) {
    val privateLists = TasksRepository.privateLists
    val tabs: List<TasksTab> = remember(privateLists.toList()) {
        listOf(TasksTab.Starred, TasksTab.School) + privateLists.map { TasksTab.Private(it) }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    var previousListsSize by remember { mutableIntStateOf(privateLists.size) }
    LaunchedEffect(privateLists.size) {
        if (privateLists.size > previousListsSize) {
            val newIndex = tabs.size - 1
            if (newIndex >= 0) {
                pagerState.animateScrollToPage(newIndex)
            }
        }
        previousListsSize = privateLists.size
    }

    var showNewTaskBottomSheet by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<de.kjltowinkler.msapp.data.task.TaskItem?>(null) }
    var showEditListSheet by remember { mutableStateOf(false) }
    var selectedListForEdit by remember { mutableStateOf<TaskList?>(null) }
    var showSchoolTasksSheet by remember { mutableStateOf(false) }
    var showStarredTasksSheet by remember { mutableStateOf(false) }
    var showNewListSheet by remember { mutableStateOf(false) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var activeBursts by remember { mutableStateOf(listOf<ConfettiBurst>()) }
    var burstCounter by remember { mutableStateOf(0L) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            delay(1000)
            isRefreshing = false
        }
    }

    val allTasks = TasksRepository.tasks
    val schoolTasks = allTasks.filter { it.isSchoolTask }
    val starredTasks = allTasks.filter { it.isStarred }

    val schoolTasksCount = schoolTasks.count { !it.isCompleted }

    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                rootCoordinates = coordinates
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 8.dp, bottom = 96.dp)
        ) {
            // --- TOP TAB ROW mit animiertem Indikator-Strich ---
            TasksTabRow(
                currentPage = pagerState.currentPage,
                onTabSelected = { pageIndex ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pageIndex)
                    }
                },
                onNewListClicked = {
                    if (onNewListClicked != null) {
                        onNewListClicked()
                    } else {
                        showNewListSheet = true
                    }
                },
                schoolTasksCount = schoolTasksCount,
                tabs = tabs,
                allTasks = allTasks
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- PULL TO REFRESH & HORIZONTAL PAGER ---
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = colorScheme.surfaceContainerHighest,
                        color = colorScheme.primary
                    )
                }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                val tab = tabs.getOrElse(page) { TasksTab.Starred }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                ) {
                    val currentTabTasks = when (tab) {
                        TasksTab.Starred -> starredTasks
                        TasksTab.School -> schoolTasks
                        is TasksTab.Private -> allTasks.filter {
                            !it.isSchoolTask && (it.listId == tab.list.id || (tab.list.id == "my_tasks" && it.listId.isEmpty()))
                        }
                    }
                    val activeTasks = currentTabTasks.filter { !it.isCompleted }
                    val completedTasks = currentTabTasks.filter { it.isCompleted }

                    val contentState = when {
                        tab is TasksTab.Starred -> {
                            if (activeTasks.isEmpty()) TaskListContentState.EMPTY
                            else TaskListContentState.HAS_ACTIVE
                        }
                        activeTasks.isEmpty() && completedTasks.isEmpty() -> TaskListContentState.EMPTY
                        activeTasks.isEmpty() -> TaskListContentState.ALL_DONE
                        else -> TaskListContentState.HAS_ACTIVE
                    }

                    // --- 1. HAUPTKARTE (Aktive Aufgaben, All Done oder Empty State mit weicher Transition) ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                        ) {
                            // --- CARD HEADER (Titel links, Sortieren- & Optionen-Icons rechts) ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (tab) {
                                        TasksTab.Starred -> "Markiert"
                                        TasksTab.School -> "Schul-Aufgaben"
                                        is TasksTab.Private -> tab.list.title
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { /* Sortier-Aktion */ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_swap_vert),
                                            contentDescription = "Sortieren",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            when (tab) {
                                                TasksTab.Starred -> showStarredTasksSheet = true
                                                TasksTab.School -> showSchoolTasksSheet = true
                                                is TasksTab.Private -> {
                                                    selectedListForEdit = tab.list
                                                    showEditListSheet = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_more_vert),
                                            contentDescription = "Weitere Optionen",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // --- CARD INHALT (Animierter Wechsel zwischen EMPTY, ALL_DONE und HAS_ACTIVE) ---
                            AnimatedContent(
                                targetState = contentState,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                     scaleIn(initialScale = 0.98f, animationSpec = tween(180, easing = FastOutSlowInEasing)))
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)) +
                                            scaleOut(targetScale = 0.98f, animationSpec = tween(140, easing = FastOutSlowInEasing))
                                        )
                                },
                                label = "CardContentStateTransition"
                            ) { state ->
                                when (state) {
                                    TaskListContentState.EMPTY -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            when (tab) {
                                                TasksTab.Starred -> {
                                                    TaskEmptyState(
                                                        imageDrawable = Res.drawable.tasks_starred_empty,
                                                        title = "Keine markierten\nAufgaben",
                                                        subtitle = "Markieren Sie wichtige Aufgaben mit einem Stern, damit Sie sie hier leichter wiederfinden"
                                                    )
                                                }
                                                TasksTab.School -> {
                                                    TaskEmptyState(
                                                        imageDrawable = Res.drawable.tasks_school_empty,
                                                        title = "Keine Schul-\nAufgaben",
                                                        subtitle = "Hier werden dir deine Hausaufgaben/Aufgaben von deinen Lehrkräfte aus deiner Schule angezeigt"
                                                    )
                                                }
                                                is TasksTab.Private -> {
                                                    TaskEmptyState(
                                                        imageDrawable = Res.drawable.tasks_my_empty,
                                                        title = "Noch keine aufgaben",
                                                        subtitle = "Hier kannst du deine To-dos hinzufügen und diese mit deinen Schulaufgaben verwalten und erledigen."
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(18.dp))
                                        }
                                    }
                                    TaskListContentState.ALL_DONE -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            AllTasksCompletedIllustration()
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }
                                    TaskListContentState.HAS_ACTIVE -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                ),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            activeTasks.forEach { task ->
                                                var isDisappearing by remember(task.id) { mutableStateOf(false) }

                                                AnimatedVisibility(
                                                    visible = !isDisappearing,
                                                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                                                    exit = fadeOut(animationSpec = tween(130)) + shrinkVertically(animationSpec = tween(150, easing = FastOutSlowInEasing))
                                                ) {
                                                    ActiveTaskRowItem(
                                                        task = task,
                                                        onToggleComplete = { circleCoords ->
                                                            try {
                                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                            } catch (_: Exception) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                            }
                                                            val root = rootCoordinates
                                                            val localOrigin = if (root != null && root.isAttached && circleCoords.isAttached) {
                                                                val circleCenter = Offset(
                                                                    circleCoords.size.width / 2f,
                                                                    circleCoords.size.height / 2f
                                                                )
                                                                root.localPositionOf(circleCoords, circleCenter)
                                                            } else {
                                                                val pos = circleCoords.positionInRoot()
                                                                Offset(pos.x + circleCoords.size.width / 2f, pos.y + circleCoords.size.height / 2f)
                                                            }
                                                            val nextId = ++burstCounter
                                                            activeBursts = activeBursts + ConfettiBurst(
                                                                id = nextId,
                                                                origin = localOrigin
                                                            )
                                                            isDisappearing = true
                                                            coroutineScope.launch {
                                                                delay(140)
                                                                TasksRepository.toggleCompleted(task.id)
                                                            }
                                                        },
                                                        onToggleStarred = { TasksRepository.toggleStarred(task.id) },
                                                        onTaskClick = { selectedTaskForEdit = task }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 2. ERLEDIGT-DROPDOWN-KARTE (sanftes Einblenden/Aufklappen ohne Bouncing) ---
                    AnimatedVisibility(
                        visible = tab !is TasksTab.Starred && completedTasks.isNotEmpty(),
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(animationSpec = tween(180)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(160, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(140)) + scaleOut(
                            targetScale = 0.98f,
                            animationSpec = tween(140, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            var isErledigtExpanded by rememberSaveable(page) { mutableStateOf(false) }

                            Card(
                                modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .clickable { isErledigtExpanded = !isErledigtExpanded }
                                            .padding(horizontal = 18.dp)
                                            .padding(top = 16.dp, bottom = if (isErledigtExpanded) 10.dp else 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Erledigt (${completedTasks.size})",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )

                                        val rotationAngle by animateFloatAsState(
                                            targetValue = if (isErledigtExpanded) 180f else 0f,
                                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                                        )

                                        Icon(
                                            painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                                            contentDescription = if (isErledigtExpanded) "Einklappen" else "Ausklappen",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .rotate(rotationAngle)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isErledigtExpanded,
                                        enter = expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(160)),
                                        exit = shrinkVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(140))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                                .padding(horizontal = 18.dp)
                                                .padding(bottom = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            completedTasks.forEach { task ->
                                                var isReactivating by remember(task.id) { mutableStateOf(false) }

                                                AnimatedVisibility(
                                                    visible = !isReactivating,
                                                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                                                    exit = fadeOut(animationSpec = tween(130)) + shrinkVertically(animationSpec = tween(150, easing = FastOutSlowInEasing))
                                                ) {
                                                    CompletedTaskRowItem(
                                                        task = task,
                                                        onToggleComplete = {
                                                            isReactivating = true
                                                            coroutineScope.launch {
                                                                delay(130)
                                                                TasksRepository.toggleCompleted(task.id)
                                                            }
                                                        },
                                                        onTaskClick = { selectedTaskForEdit = task }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        val currentTab = tabs.getOrNull(pagerState.currentPage) ?: TasksTab.Starred
        val isCurrentTabStarred = currentTab is TasksTab.Starred
        val currentTargetListId = (currentTab as? TasksTab.Private)?.list?.id ?: "my_tasks"

        val focusRequester = remember { FocusRequester() }
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        PlatformBackHandler(enabled = fabMenuExpanded) {
            fabMenuExpanded = false
        }

        // Klick außerhalb schließt das FAB-Menü
        if (fabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        fabMenuExpanded = false
                    }
            )
        }

        // --- FLOATING ACTION BUTTON MENU (FAB-Menü) unten rechts ---
        FloatingActionButtonMenu(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
            expanded = fabMenuExpanded,
            button = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        if (fabMenuExpanded) {
                            TooltipAnchorPosition.Start
                        } else {
                            TooltipAnchorPosition.Above
                        }
                    ),
                    tooltip = {
                        PlainTooltip(
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Assertive
                                paneTitle = "Toggle menu"
                            }
                        ) {
                            Text("Aktionsmenü")
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    ToggleFloatingActionButton(
                        modifier = Modifier
                            .semantics {
                                traversalIndex = -1f
                                stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                                contentDescription = "Aktionsmenü umschalten"
                            }
                            .animateFloatingActionButton(
                                visible = true,
                                alignment = Alignment.BottomEnd,
                            )
                            .focusRequester(focusRequester),
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                    ) {
                        Icon(
                            painter = painterResource(if (checkedProgress > 0.5f) Res.drawable.ic_close else Res.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.animateIcon({ checkedProgress }),
                        )
                    }
                }
            },
        ) {
            FloatingActionButtonMenuItem(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                },
                onClick = {
                    fabMenuExpanded = false
                    if (onNewListClicked != null) {
                        onNewListClicked()
                    } else {
                        showNewListSheet = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit_note),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = "Neue Liste",
                        fontWeight = FontWeight.Medium
                    )
                },
            )

            FloatingActionButtonMenuItem(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = "Menü schließen",
                            action = {
                                fabMenuExpanded = false
                                true
                            },
                        )
                    )
                },
                onClick = {
                    fabMenuExpanded = false
                    if (onAddTaskClicked != null) {
                        onAddTaskClicked(isCurrentTabStarred)
                    } else {
                        showNewTaskBottomSheet = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add_task),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = "Neue Aufgabe",
                        fontWeight = FontWeight.Medium
                    )
                },
            )
        }

        // --- BOTTOM SHEET FÜR NEUE AUFGABEN (tasks#neu+detail.png) ---
        if (onAddTaskClicked == null && showNewTaskBottomSheet) {
            NewTaskBottomSheet(
                initialIsStarred = isCurrentTabStarred,
                targetListId = currentTargetListId,
                onDismissRequest = { showNewTaskBottomSheet = false },
                onSaveTask = { newTask ->
                    TasksRepository.addTask(newTask)
                }
            )
        }

        // --- BOTTOM SHEET FÜR AUFGABEN BEARBEITEN / INFO (tasks#editsheet.png) ---
        selectedTaskForEdit?.let { taskToEdit ->
            val currentTask = allTasks.find { it.id == taskToEdit.id } ?: taskToEdit
            TaskEditBottomSheet(
                task = currentTask,
                onDismissRequest = { selectedTaskForEdit = null },
                onUpdateTask = { updatedTask ->
                    TasksRepository.updateTask(updatedTask)
                },
                onDeleteTask = { taskId ->
                    val task = allTasks.find { it.id == taskId }
                    if (task?.isSchoolTask != true) {
                        TasksRepository.deleteTask(taskId)
                    }
                }
            )
        }

        // --- BOTTOM SHEET FÜR LISTE BEARBEITEN (tasks#editlistesheet.png) ---
        if (showEditListSheet && selectedListForEdit != null) {
            val list = selectedListForEdit!!
            val isCustomList = list.id != "my_tasks"
            val listTasks = allTasks.filter {
                !it.isSchoolTask && (it.listId == list.id || (list.id == "my_tasks" && it.listId.isEmpty()))
            }
            val areAllListCompleted = listTasks.isNotEmpty() && listTasks.all { it.isCompleted }
            val areAllListStarred = listTasks.isNotEmpty() && listTasks.all { it.isStarred }
            EditListBottomSheet(
                listTitle = list.title,
                areAllCompleted = areAllListCompleted,
                areAllStarred = areAllListStarred,
                onDismissRequest = {
                    showEditListSheet = false
                    selectedListForEdit = null
                },
                onRenameList = { newTitle ->
                    TasksRepository.renamePrivateList(list.id, newTitle)
                },
                onMarkAllCompleted = {
                    TasksRepository.markAllTasksInListCompleted(list.id)
                },
                onMarkAllUncompleted = {
                    TasksRepository.markAllTasksInListUncompleted(list.id)
                },
                onMarkAllStarred = {
                    TasksRepository.markAllTasksInListStarred(list.id)
                },
                onUnstarAll = {
                    TasksRepository.unstarAllTasksInList(list.id)
                },
                onDeleteCompletedTasks = {
                    TasksRepository.deleteCompletedTasksInList(list.id)
                },
                onDeleteAllTasks = {
                    TasksRepository.deleteAllTasksInList(list.id)
                },
                onDeleteList = if (isCustomList) {
                    {
                        TasksRepository.deletePrivateList(list.id)
                        coroutineScope.launch {
                            val newPageIndex = (pagerState.currentPage - 1).coerceAtLeast(0)
                            pagerState.animateScrollToPage(newPageIndex)
                        }
                    }
                } else null
            )
        }

        // --- VOLLBILD-SCREEN FÜR NEUE LISTE ERSTELLEN (tasks#neueliste.png) ---
        if (onNewListClicked == null && showNewListSheet) {
            NewListScreen(
                onDismissRequest = { showNewListSheet = false },
                onSaveList = { newTitle ->
                    val created = TasksRepository.createPrivateList(newTitle)
                    showNewListSheet = false
                    coroutineScope.launch {
                        delay(120)
                        val newIndex = tabs.indexOfFirst { (it as? TasksTab.Private)?.list?.id == created.id }
                        if (newIndex >= 0) {
                            pagerState.animateScrollToPage(newIndex)
                        } else {
                            pagerState.animateScrollToPage(tabs.size.coerceAtLeast(1) - 1)
                        }
                    }
                }
            )
        }

        // --- BOTTOM SHEET FÜR SCHUL-AUFGABEN OPTIONEN ---
        if (showSchoolTasksSheet) {
            val areAllSchoolCompleted = schoolTasks.isNotEmpty() && schoolTasks.all { it.isCompleted }
            val areAllSchoolStarred = schoolTasks.isNotEmpty() && schoolTasks.all { it.isStarred }
            SchoolTasksBottomSheet(
                onDismissRequest = { showSchoolTasksSheet = false },
                areAllCompleted = areAllSchoolCompleted,
                areAllStarred = areAllSchoolStarred,
                onMarkAllCompleted = {
                    TasksRepository.markAllSchoolTasksCompleted()
                },
                onMarkAllUncompleted = {
                    TasksRepository.markAllSchoolTasksUncompleted()
                },
                onMarkAllStarred = {
                    TasksRepository.markAllSchoolTasksStarred()
                },
                onUnstarAll = {
                    TasksRepository.unstarAllSchoolTasks()
                }
            )
        }

        // --- BOTTOM SHEET FÜR MARKIERT OPTIONEN ---
        if (showStarredTasksSheet) {
            val areAllStarredCompleted = starredTasks.isNotEmpty() && starredTasks.all { it.isCompleted }
            StarredTasksBottomSheet(
                onDismissRequest = { showStarredTasksSheet = false },
                areAllCompleted = areAllStarredCompleted,
                onMarkAllCompleted = {
                    TasksRepository.markAllStarredTasksCompleted()
                },
                onMarkAllUncompleted = {
                    TasksRepository.markAllStarredTasksUncompleted()
                },
                onUnstarAll = {
                    TasksRepository.unstarAllTasks()
                },
                onDeleteCompletedStarred = {
                    TasksRepository.deleteCompletedStarredTasks()
                },
                onDeleteAllStarred = {
                    TasksRepository.deleteAllStarredTasks()
                }
            )
        }

        // --- KONFETTI-EFFEKTE (beim Abhaken von Aufgaben) ---
        activeBursts.forEach { burst ->
            key(burst.id) {
                TaskConfettiEffect(
                    origin = burst.origin,
                    onFinished = {
                        activeBursts = activeBursts.filter { it.id != burst.id }
                    }
                )
            }
        }
    }
}

@Composable
private fun TasksTabRow(
    currentPage: Int,
    onTabSelected: (Int) -> Unit,
    onNewListClicked: () -> Unit,
    schoolTasksCount: Int,
    tabs: List<TasksTab>,
    allTasks: List<de.kjltowinkler.msapp.data.task.TaskItem>
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeColor = colorScheme.primary
    val inactiveColor = colorScheme.onSurfaceVariant
    val indicatorColor = colorScheme.primary

    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var tabRowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Map von Tab-Index zu LayoutCoordinates
    val tabCoordsMap = remember { mutableStateMapOf<Int, LayoutCoordinates>() }

    // Dynamisch gemessene Positionen und Breiten der Tabs für die sanfte Animation
    val tabOffsets = remember { mutableStateMapOf<Int, Dp>() }
    val tabWidths = remember { mutableStateMapOf<Int, Dp>() }
    var isMeasured by remember { mutableStateOf(false) }

    fun updateMeasurements() {
        val root = tabRowCoordinates ?: return
        if (!root.isAttached) return
        with(density) {
            tabCoordsMap.forEach { (index, coords) ->
                if (coords.isAttached) {
                    val pos = root.localPositionOf(coords, Offset.Zero)
                    tabOffsets[index] = pos.x.toDp()
                    tabWidths[index] = coords.size.width.toDp()
                }
            }
            if (tabOffsets.isNotEmpty()) {
                isMeasured = true
            }
        }
    }

    LaunchedEffect(currentPage, schoolTasksCount, tabs, allTasks.size) {
        updateMeasurements()
    }

    // Auto-scroll to keep active tab visible when currentPage changes
    LaunchedEffect(currentPage, tabOffsets[currentPage]) {
        val offset = tabOffsets[currentPage]
        if (offset != null) {
            with(density) {
                val px = offset.toPx().toInt()
                scrollState.animateScrollTo((px - 72).coerceAtLeast(0))
            }
        }
    }

    val targetOffset = when (currentPage) {
        0 -> ((tabOffsets[0] ?: 0.dp) - 2.dp).coerceAtLeast(0.dp)
        else -> tabOffsets[currentPage] ?: 0.dp
    }

    val targetWidth = when (currentPage) {
        0 -> 24.dp
        else -> tabWidths[currentPage] ?: 60.dp
    }

    // Sanfte, flüssige Animation beim Wechseln der Tabs ohne Bouncing
    val animatedOffset: Dp by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 2500f
        ),
        label = "indicatorOffset"
    )

    val animatedWidth: Dp by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 2500f
        ),
        label = "indicatorWidth"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .onGloballyPositioned { coords ->
                tabRowCoordinates = coords
                updateMeasurements()
            }
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.Start)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentPage == index
                when (tab) {
                    TasksTab.Starred -> {
                        // Tab 0: Stern (Markiert)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(index) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_star_filled),
                                contentDescription = "Markiert",
                                tint = if (isSelected) activeColor else inactiveColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .onGloballyPositioned { coords ->
                                        tabCoordsMap[index] = coords
                                        updateMeasurements()
                                    }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Unsichtbarer Platzhalter für die Indikator-Höhe
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                    TasksTab.School -> {
                        // Tab: Schul-Aufgaben
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(index) }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Schul-Aufgaben",
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else inactiveColor,
                                    maxLines = 1,
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        tabCoordsMap[index] = coords
                                        updateMeasurements()
                                    }
                                )
                                if (schoolTasksCount > 0) {
                                    TaskCountBadge(
                                        count = schoolTasksCount,
                                        backgroundColor = colorScheme.surfaceContainerHighest,
                                        textColor = colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                    is TasksTab.Private -> {
                        val count = allTasks.count {
                            !it.isSchoolTask && !it.isCompleted && (it.listId == tab.list.id || (tab.list.id == "my_tasks" && it.listId.isEmpty()))
                        }
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(index) }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.list.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else inactiveColor,
                                    maxLines = 1,
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        tabCoordsMap[index] = coords
                                        updateMeasurements()
                                    }
                                )
                                if (count > 0) {
                                    TaskCountBadge(
                                        count = count,
                                        backgroundColor = colorScheme.surfaceContainerHighest,
                                        textColor = colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }

            // Tab: + Neue Liste
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNewListClicked() }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+ Neue Liste",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = inactiveColor,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Spacer(modifier = Modifier.height(3.dp))
            }

            // Ausreichend Abstand am Ende, damit "+ Neue Liste" beim Weiterscrollen nicht am Bildschirmrand klebt
            Spacer(modifier = Modifier.width(44.dp))
        }

        // Gleitender Indikator-Strich (flüssige Animation, oben abgerundet, unten gerade gemäß Design)
        if (isMeasured && animatedWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = animatedOffset)
                    .width(animatedWidth)
                    .height(3.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 3.dp,
                            topEnd = 3.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .background(indicatorColor)
            )
        }
    }
}

@Composable
private fun TaskCountBadge(
    count: Int,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 5.dp)
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            maxLines = 1
        )
    }
}


@Composable
private fun TaskEmptyState(
    imageDrawable: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    subtitle: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(imageDrawable),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 10.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth(0.92f)
        )
    }
}

/**
 * Illustration wenn alle Aufgaben in einer Liste erledigt sind.
 * Wechselt zufällig zwischen `tasks_done_char` (mit Person) und `tasks_done_clean` (ohne Person).
 */
@Composable
private fun AllTasksCompletedIllustration() {
    val illustration = remember {
        if (kotlin.random.Random.nextBoolean()) {
            Res.drawable.tasks_done_char
        } else {
            Res.drawable.tasks_done_clean
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(illustration),
            contentDescription = "Alle Aufgaben erledigt",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun ActiveTaskRowItem(
    task: de.kjltowinkler.msapp.data.task.TaskItem,
    onToggleComplete: (LayoutCoordinates) -> Unit,
    onToggleStarred: () -> Unit,
    onTaskClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var circleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Kreis / Radio-Button Outline zum Abhaken
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .onGloballyPositioned { coordinates ->
                        circleCoordinates = coordinates
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp)
                    ) {
                        circleCoordinates?.let { coords ->
                            onToggleComplete(coords)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            width = 2.dp,
                            color = Color(0xFF8E918F),
                            shape = CircleShape
                        )
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTaskClick() }
            ) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                if (task.dueDate.isNotBlank() || task.dueTime.isNotBlank()) {
                    val dateLabel = buildString {
                        if (task.dueDate.isNotBlank()) append(task.dueDate)
                        if (task.dueTime.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("${task.dueTime} Uhr")
                        }
                    }
                    Text(
                        text = dateLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary
                    )
                }
            }
        }

        IconButton(
            onClick = onToggleStarred,
            modifier = Modifier
                .padding(top = 0.dp)
                .size(28.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (task.isStarred) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                ),
                contentDescription = "Favorit",
                tint = if (task.isStarred) colorScheme.primary else Color(0xFF8E918F),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CompletedTaskRowItem(
    task: de.kjltowinkler.msapp.data.task.TaskItem,
    onToggleComplete: () -> Unit,
    onTaskClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val displayDate = if (task.completedDate.isNotBlank()) {
        task.completedDate
    } else {
        de.kjltowinkler.msapp.data.task.getTodayDate().formattedCompletedGerman()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Haken (Checkmark) zum Wiederaktivieren
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 20.dp)
                ) { onToggleComplete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = "Als unvollständig markieren",
                tint = colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onTaskClick() }
        ) {
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.LineThrough,
                color = colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Text(
                text = "Erledigt: $displayDate",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
