package de.kjltowinkler.msapp.wear.presentation.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.data.task.getCurrentEpochMillis
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.RobotoFlex
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.launch

enum class WearDueDateOption(val label: String) {
    NONE("Ohne"),
    TODAY("Heute"),
    TOMORROW("Morgen")
}

/**
 * Screen zum Erstellen einer neuen Aufgabe auf der Pixel Watch.
 * Speichert die Aufgabe über [TasksRepository.addTask] und synchronisiert direkt mit dem Backend.
 */
@Composable
fun WearCreateTaskScreen(
    initialListId: String = "my_tasks",
    onTaskCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var selectedDueDate by remember { mutableStateOf(WearDueDateOption.NONE) }
    var isStarred by remember { mutableStateOf(false) }
    var selectedListId by remember(initialListId) { mutableStateOf(initialListId) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val lists = TasksRepository.privateLists
    val currentListTitle = lists.firstOrNull { it.id == selectedListId }?.title ?: "Meine Aufgaben"

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spokenText.isNullOrBlank()) {
                title = if (title.isBlank()) spokenText else "$title $spokenText"
                statusMessage = null
            }
        }
    }

    fun saveTask() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank() || isSaving) return

        keyboardController?.hide()
        isSaving = true
        statusMessage = null

        val dueDateStr = when (selectedDueDate) {
            WearDueDateOption.NONE -> ""
            WearDueDateOption.TODAY -> getTodayDate().formattedCompletedGerman()
            WearDueDateOption.TOMORROW -> getTodayDate().plusDays(1).formattedCompletedGerman()
        }

        val newTask = TaskItem(
            id = getCurrentEpochMillis().toString(),
            title = trimmedTitle,
            dueDate = dueDateStr,
            isStarred = isStarred,
            isSchoolTask = false,
            listId = selectedListId
        )

        TasksRepository.addTask(newTask)

        if (!BackendApiClient.isLoggedIn) {
            isSaving = false
            statusMessage = "Lokal gespeichert (nicht angemeldet)"
            onTaskCreated()
            return
        }

        coroutineScope.launch {
            val synced = TasksRepository.reloadAndSync()
            isSaving = false
            if (synced) {
                onTaskCreated()
            } else {
                statusMessage = TasksRepository.lastSyncError
                    ?: "Backend-Sync fehlgeschlagen (lokal vorgemerkt)"
            }
        }
    }

    WearCreateTaskScreenContent(
        title = title,
        onTitleChange = {
            title = it
            statusMessage = null
        },
        selectedDueDate = selectedDueDate,
        onDueDateChange = { selectedDueDate = it },
        isStarred = isStarred,
        onStarredChange = { isStarred = it },
        listTitle = currentListTitle,
        hasMultipleLists = lists.size > 1,
        onCycleList = {
            if (lists.size > 1) {
                val idx = lists.indexOfFirst { it.id == selectedListId }
                val nextIdx = (idx + 1) % lists.size
                selectedListId = lists[nextIdx].id
            }
        },
        isSaving = isSaving,
        statusMessage = statusMessage,
        focusRequester = focusRequester,
        onVoiceInputClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Aufgabe einsprechen")
            }
            val launched = runCatching { speechLauncher.launch(intent) }.isSuccess
            if (!launched) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        },
        onSaveClick = { saveTask() }
    )
}

@Composable
fun WearCreateTaskScreenContent(
    title: String,
    onTitleChange: (String) -> Unit,
    selectedDueDate: WearDueDateOption,
    onDueDateChange: (WearDueDateOption) -> Unit,
    isStarred: Boolean,
    onStarredChange: (Boolean) -> Unit,
    listTitle: String,
    hasMultipleLists: Boolean,
    onCycleList: () -> Unit,
    isSaving: Boolean,
    statusMessage: String?,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onVoiceInputClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onSaveClick,
                enabled = title.isNotBlank() && !isSaving,
                buttonSize = EdgeButtonSize.Medium
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSaving) "Speichert…" else "Speichern",
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Text(
                        text = "Neue Aufgabe",
                        fontFamily = RobotoFlex,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Titel-Eingabe + Spracheingabe-Button
            item(key = "title_input") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                width = 1.dp,
                                color = if (title.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { focusRequester.requestFocus() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Titel eingeben…",
                                fontFamily = RobotoFlex,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = RobotoFlex,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (title.isNotBlank() && !isSaving) {
                                        onSaveClick()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    // Spracheingabe-Button (mind. 48dp Touch Target)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onVoiceInputClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mic),
                            contentDescription = "Spracheingabe",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Fälligkeitsdatum Schnellauswahl
            item(key = "due_date_label") {
                Text(
                    text = "Fälligkeit",
                    fontFamily = RobotoFlex,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            item(key = "due_date_options") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WearDueDateOption.entries.forEach { option ->
                        val isSelected = selectedDueDate == option
                        Button(
                            onClick = { onDueDateChange(option) },
                            colors = if (isSelected) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = option.label,
                                fontFamily = RobotoFlex,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Wichtig-Schalter
            item(key = "starred_toggle") {
                SwitchButton(
                    checked = isStarred,
                    onCheckedChange = onStarredChange,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = if (isStarred) R.drawable.ic_star else R.drawable.ic_star_outline
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                ) {
                    Text(
                        text = "Wichtig",
                        fontFamily = RobotoFlex,
                        maxLines = 1
                    )
                }
            }

            // Liste auswählen (falls mehrere Listen existieren)
            if (hasMultipleLists) {
                item(key = "list_picker") {
                    FilledTonalButton(
                        onClick = onCycleList,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Liste",
                                fontFamily = RobotoFlex,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = listTitle,
                                fontFamily = RobotoFlex,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Fehler-/Statusmeldung
            if (!statusMessage.isNullOrBlank()) {
                item(key = "status_message") {
                    Text(
                        text = statusMessage,
                        fontFamily = RobotoFlex,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearCreateTaskScreenPreview() {
    WearAppTheme {
        WearCreateTaskScreenContent(
            title = "Bio Protokoll abgeben",
            onTitleChange = {},
            selectedDueDate = WearDueDateOption.TOMORROW,
            onDueDateChange = {},
            isStarred = true,
            onStarredChange = {},
            listTitle = "Meine Aufgaben",
            hasMultipleLists = true,
            onCycleList = {},
            isSaving = false,
            statusMessage = null,
            onVoiceInputClick = {},
            onSaveClick = {}
        )
    }
}
