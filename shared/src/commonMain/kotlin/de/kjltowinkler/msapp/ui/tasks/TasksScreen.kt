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
import de.kjltowinkler.msapp.data.task.TaskCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import de.kjltowinkler.msapp.data.task.TasksRepository
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

private enum class TaskListContentState {
    EMPTY,
    ALL_DONE,
    HAS_ACTIVE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    onAddTaskClicked: (() -> Unit)? = null
) {
    val categories = remember {
        listOf(TaskCategory.STARRED, TaskCategory.SCHOOL, TaskCategory.MY_TASKS)
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    var showNewTaskBottomSheet by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<de.kjltowinkler.msapp.data.task.TaskItem?>(null) }
    var showEditListSheet by remember { mutableStateOf(false) }
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
    val myTasks = allTasks.filter { !it.isSchoolTask }
    val starredTasks = allTasks.filter { it.isStarred }

    val schoolTasksCount = schoolTasks.count { !it.isCompleted }
    val myTasksCount = myTasks.count { !it.isCompleted }

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
                schoolTasksCount = schoolTasksCount,
                myTasksCount = myTasksCount,
                myTasksTitle = TasksRepository.myTasksTitle
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
                val category = categories[page]

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
                    val currentTabTasks = when (category) {
                        TaskCategory.STARRED -> starredTasks
                        TaskCategory.SCHOOL -> schoolTasks
                        TaskCategory.MY_TASKS -> myTasks
                    }
                    val activeTasks = currentTabTasks.filter { !it.isCompleted }
                    val completedTasks = currentTabTasks.filter { it.isCompleted }

                    val contentState = when {
                        category == TaskCategory.STARRED -> {
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
                                    text = when (category) {
                                        TaskCategory.STARRED -> "Markiert"
                                        TaskCategory.SCHOOL -> "Schul-Aufgaben"
                                        TaskCategory.MY_TASKS -> TasksRepository.myTasksTitle
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

                                    if (category == TaskCategory.MY_TASKS) {
                                        IconButton(
                                            onClick = { showEditListSheet = true },
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
                                            when (category) {
                                                TaskCategory.STARRED -> {
                                                    TaskEmptyState(
                                                        imageDrawable = Res.drawable.tasks_starred_empty,
                                                        title = "Keine markierten\nAufgaben",
                                                        subtitle = "Markieren Sie wichtige Aufgaben mit einem Stern, damit Sie sie hier leichter wiederfinden"
                                                    )
                                                }
                                                TaskCategory.SCHOOL -> {
                                                    TaskEmptyState(
                                                        imageDrawable = Res.drawable.tasks_school_empty,
                                                        title = "Keine Schul-\nAufgaben",
                                                        subtitle = "Hier werden dir deine Hausaufgaben/Aufgaben von deinen Lehrkräfte aus deiner Schule angezeigt"
                                                    )
                                                }
                                                TaskCategory.MY_TASKS -> {
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
                        visible = category != TaskCategory.STARRED && completedTasks.isNotEmpty(),
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

                            var isErledigtExpanded by rememberSaveable(category) { mutableStateOf(false) }

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

        // --- FLOATING ACTION BUTTON (FAB) unten rechts ---
        FloatingActionButton(
            onClick = {
                if (onAddTaskClicked != null) {
                    onAddTaskClicked()
                } else {
                    showNewTaskBottomSheet = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .size(56.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_add),
                contentDescription = "Aufgabe hinzufügen",
                modifier = Modifier.size(26.dp)
            )
        }

        // --- BOTTOM SHEET FÜR NEUE AUFGABEN (tasks#neu+detail.png) ---
        if (onAddTaskClicked == null && showNewTaskBottomSheet) {
            NewTaskBottomSheet(
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
                    TasksRepository.deleteTask(taskId)
                }
            )
        }

        // --- BOTTOM SHEET FÜR LISTE BEARBEITEN (tasks#editlistesheet.png) ---
        if (showEditListSheet) {
            EditListBottomSheet(
                listTitle = TasksRepository.myTasksTitle,
                onDismissRequest = { showEditListSheet = false },
                onRenameList = { newTitle ->
                    TasksRepository.myTasksTitle = newTitle
                },
                onDeleteCompletedTasks = {
                    TasksRepository.deleteCompletedTasks(isSchoolTask = false)
                },
                onDeleteAllTasks = {
                    TasksRepository.deleteAllMyTasks()
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
private fun TasksTabRow(
    currentPage: Int,
    onTabSelected: (Int) -> Unit,
    schoolTasksCount: Int,
    myTasksCount: Int,
    myTasksTitle: String = TasksRepository.myTasksTitle
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeColor = colorScheme.primary
    val inactiveColor = colorScheme.onSurfaceVariant
    val indicatorColor = colorScheme.primary

    val density = LocalDensity.current
    var tabRowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var tab0Coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var tab1Coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var tab2Coords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Dynamisch gemessene Positionen und Breiten der 3 Tabs für die sanfte Animation
    var tab0Offset by remember { mutableStateOf(0.dp) }
    var tab0Width by remember { mutableStateOf(24.dp) }

    var tab1Offset by remember { mutableStateOf(0.dp) }
    var tab1Width by remember { mutableStateOf(80.dp) }

    var tab2Offset by remember { mutableStateOf(0.dp) }
    var tab2Width by remember { mutableStateOf(80.dp) }
    var isMeasured by remember { mutableStateOf(false) }

    fun updateMeasurements() {
        val root = tabRowCoordinates ?: return
        if (!root.isAttached) return
        with(density) {
            tab0Coords?.let { c ->
                if (c.isAttached) {
                    val pos = root.localPositionOf(c, Offset.Zero)
                    tab0Offset = pos.x.toDp()
                    tab0Width = c.size.width.toDp()
                }
            }
            tab1Coords?.let { c ->
                if (c.isAttached) {
                    val pos = root.localPositionOf(c, Offset.Zero)
                    tab1Offset = pos.x.toDp()
                    tab1Width = c.size.width.toDp()
                }
            }
            tab2Coords?.let { c ->
                if (c.isAttached) {
                    val pos = root.localPositionOf(c, Offset.Zero)
                    tab2Offset = pos.x.toDp()
                    tab2Width = c.size.width.toDp()
                }
            }
            isMeasured = true
        }
    }

    LaunchedEffect(currentPage, schoolTasksCount, myTasksCount, myTasksTitle) {
        updateMeasurements()
    }

    val targetOffset = when (currentPage) {
        0 -> (tab0Offset - 2.dp).coerceAtLeast(0.dp)
        1 -> tab1Offset
        else -> tab2Offset
    }

    val targetWidth = when (currentPage) {
        0 -> 24.dp
        1 -> tab1Width
        else -> tab2Width
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
            .onGloballyPositioned { coords ->
                tabRowCoordinates = coords
                updateMeasurements()
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.Start)
        ) {
            // Tab 0: Stern (Markiert)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(0) }
                    .padding(vertical = 4.dp)
            ) {
                val isSelected = currentPage == 0
                Icon(
                    painter = painterResource(Res.drawable.ic_star_filled),
                    contentDescription = "Markiert",
                    tint = if (isSelected) activeColor else inactiveColor,
                    modifier = Modifier
                        .size(20.dp)
                        .onGloballyPositioned { coords ->
                            tab0Coords = coords
                            updateMeasurements()
                        }
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Unsichtbarer Platzhalter für die Indikator-Höhe
                Spacer(modifier = Modifier.height(3.dp))
            }

            // Tab 1: Schul-Aufgaben
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(1) }
                    .padding(vertical = 4.dp)
            ) {
                val isSelected = currentPage == 1
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
                            tab1Coords = coords
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

            // Tab 2: Meine Aufgaben
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(2) }
                    .padding(vertical = 4.dp)
            ) {
                val isSelected = currentPage == 2
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = myTasksTitle,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) activeColor else inactiveColor,
                        maxLines = 1,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            tab2Coords = coords
                            updateMeasurements()
                        }
                    )
                    if (myTasksCount > 0) {
                        TaskCountBadge(
                            count = myTasksCount,
                            backgroundColor = colorScheme.surfaceContainerHighest,
                            textColor = colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        // Gleitender Indikator-Strich (flüssige Animation)
        if (isMeasured && animatedWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = animatedOffset)
                    .width(animatedWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )
        }
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
