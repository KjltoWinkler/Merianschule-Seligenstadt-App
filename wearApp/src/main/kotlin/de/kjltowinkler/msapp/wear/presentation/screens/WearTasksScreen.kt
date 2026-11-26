package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.CheckboxButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledTonalButton
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
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.RobotoFlex
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.launch

/**
 * Aufgaben-Übersicht für Wear OS (Pixel Watch).
 * Zeigt Aufgaben aus [TasksRepository] an, ermöglicht das Abhaken und das Erstellen neuer Aufgaben.
 */
@Composable
fun WearTasksScreen(
    onCreateTaskClick: (selectedListId: String) -> Unit,
    onLoginRequiredClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedListId by remember { mutableStateOf("my_tasks") }

    // Beim Öffnen vom Backend synchronisieren
    LaunchedEffect(Unit) {
        if (BackendApiClient.isLoggedIn) {
            TasksRepository.reloadAndSync()
        }
    }

    val lists = TasksRepository.privateLists
    val currentListTitle = lists.firstOrNull { it.id == selectedListId }?.title ?: "Meine Aufgaben"
    val allTasks = TasksRepository.tasks.filter { task ->
        if (selectedListId == "my_tasks") {
            !task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty())
        } else {
            !task.isSchoolTask && task.listId == selectedListId
        }
    }

    val openTasks = allTasks
        .filter { !it.isCompleted }
        .sortedByDescending { it.isStarred }
    val completedTasks = allTasks.filter { it.isCompleted }

    WearTasksScreenContent(
        listTitle = currentListTitle,
        hasMultipleLists = lists.size > 1,
        openTasks = openTasks,
        completedTasks = completedTasks,
        isLoggedIn = BackendApiClient.isLoggedIn,
        isSyncing = TasksRepository.isSyncing,
        syncError = TasksRepository.lastSyncError,
        onCycleList = {
            if (lists.size > 1) {
                val idx = lists.indexOfFirst { it.id == selectedListId }
                val nextIdx = (idx + 1) % lists.size
                selectedListId = lists[nextIdx].id
            }
        },
        onToggleTask = { taskId ->
            TasksRepository.toggleCompleted(taskId)
        },
        onSyncClick = {
            coroutineScope.launch {
                TasksRepository.reloadAndSync()
            }
        },
        onCreateTaskClick = { onCreateTaskClick(selectedListId) },
        onLoginRequiredClick = onLoginRequiredClick
    )
}

@Composable
fun WearTasksScreenContent(
    listTitle: String,
    hasMultipleLists: Boolean,
    openTasks: List<TaskItem>,
    completedTasks: List<TaskItem>,
    isLoggedIn: Boolean,
    isSyncing: Boolean,
    syncError: String?,
    onCycleList: () -> Unit,
    onToggleTask: (String) -> Unit,
    onSyncClick: () -> Unit,
    onCreateTaskClick: () -> Unit,
    onLoginRequiredClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onCreateTaskClick,
                buttonSize = EdgeButtonSize.Medium
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aufgabe",
                        fontFamily = RobotoFlex,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Kopfzeile mit Listennamen (antippbar zum Wechseln, falls mehrere Listen existieren)
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = listTitle,
                            fontFamily = RobotoFlex,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasMultipleLists) {
                            Text(
                                text = "Tippen zum Wechseln",
                                fontFamily = RobotoFlex,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (hasMultipleLists) {
                item(key = "switch_list") {
                    FilledTonalButton(
                        onClick = onCycleList,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Text(
                            text = "Liste wechseln",
                            fontFamily = RobotoFlex,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Direkter "Neue Aufgabe"-Button oben für schnellen Zugriff
            item(key = "quick_add") {
                Button(
                    onClick = onCreateTaskClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Neue Aufgabe",
                        fontFamily = RobotoFlex,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // Hinweis wenn nicht angemeldet
            if (!isLoggedIn) {
                item(key = "not_logged_in") {
                    FilledTonalButton(
                        onClick = onLoginRequiredClick,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Nicht gekoppelt",
                                fontFamily = RobotoFlex,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Tippen zum Anmelden",
                                fontFamily = RobotoFlex,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (!syncError.isNullOrBlank()) {
                item(key = "sync_error") {
                    FilledTonalButton(
                        onClick = onSyncClick,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Sync-Fehler (Tippen)",
                                fontFamily = RobotoFlex,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = syncError,
                                fontFamily = RobotoFlex,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isSyncing) {
                item(key = "syncing") {
                    Text(
                        text = "Synchronisiere…",
                        fontFamily = RobotoFlex,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Offene Aufgaben
            if (openTasks.isEmpty() && completedTasks.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Keine Aufgaben vorhanden.\nTippe oben auf + Neue Aufgabe.",
                        fontFamily = RobotoFlex,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(
                    items = openTasks,
                    key = { "open_${it.id}" }
                ) { task ->
                    WearTaskCheckboxItem(
                        task = task,
                        onCheckedChange = { onToggleTask(task.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }

                if (completedTasks.isNotEmpty()) {
                    item(key = "completed_header") {
                        Text(
                            text = "Erledigt (${completedTasks.size})",
                            fontFamily = RobotoFlex,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(
                        items = completedTasks,
                        key = { "done_${it.id}" }
                    ) { task ->
                        WearTaskCheckboxItem(
                            task = task,
                            onCheckedChange = { onToggleTask(task.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec)
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WearTaskCheckboxItem(
    task: TaskItem,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation
) {
    val subtitle = buildString {
        if (task.subject.isNotBlank()) append(task.subject)
        if (task.dueDate.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append(task.dueDate)
        }
        if (task.dueTime.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(task.dueTime)
        }
    }

    CheckboxButton(
        checked = task.isCompleted,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        transformation = transformation,
        colors = CheckboxButtonDefaults.checkboxButtonColors(),
        secondaryLabel = if (subtitle.isNotBlank()) {
            {
                Text(
                    text = subtitle,
                    fontFamily = RobotoFlex,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (task.isStarred && !task.isCompleted) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = "Wichtig",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = task.title,
                fontFamily = RobotoFlex,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearTasksScreenPreview() {
    WearAppTheme {
        WearTasksScreenContent(
            listTitle = "Meine Aufgaben",
            hasMultipleLists = true,
            openTasks = listOf(
                TaskItem(
                    id = "1",
                    title = "Mathe Übungsblatt S. 42",
                    subject = "Mathe",
                    dueDate = "Mo., 24. Nov.",
                    isStarred = true
                ),
                TaskItem(
                    id = "2",
                    title = "Englisch Vokabeln lernen",
                    dueDate = "Di., 25. Nov."
                )
            ),
            completedTasks = listOf(
                TaskItem(
                    id = "3",
                    title = "Sportsachen einpacken",
                    isCompleted = true
                )
            ),
            isLoggedIn = true,
            isSyncing = false,
            syncError = null,
            onCycleList = {},
            onToggleTask = {},
            onSyncClick = {},
            onCreateTaskClick = {},
            onLoginRequiredClick = {}
        )
    }
}
