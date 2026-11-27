package de.kjltowinkler.msapp.wear.presentation.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.DatePicker
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
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import java.time.LocalDate
import java.time.LocalTime
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.data.task.getCurrentEpochMillis
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.data.timetable.findNextLesson
import de.kjltowinkler.msapp.theme.SubjectColor
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.GoogleSansRounded
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.launch

enum class WearDueDateOption(val label: String) {
    NONE("Ohne"),
    TODAY("Heute"),
    TOMORROW("Morgen"),
    NEXT_LESSON("Nächste Std"),
    CUSTOM("Datum")
}

private enum class CreateTaskSubView {
    FORM,
    PICK_SUBJECT,
    PICK_DATE,
    PICK_TIME
}

/**
 * Screen zum Erstellen einer neuen Aufgabe auf der Pixel Watch.
 * Unterstützt Titel, Details, Fach, Datum, Uhrzeit sowie „Bis zur nächsten Stunde“.
 */
@Composable
fun WearCreateTaskScreen(
    initialListId: String = "my_tasks",
    onTaskCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val detailsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        TimetableRepository.loadFromCache()
    }

    var subView by remember { mutableStateOf(CreateTaskSubView.FORM) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedDueDateMode by remember { mutableStateOf(WearDueDateOption.NONE) }
    var customDate by remember { mutableStateOf<SimpleDate?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }
    var isStarred by remember { mutableStateOf(false) }
    var selectedListId by remember(initialListId) { mutableStateOf(initialListId) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val availableSubjects = remember(TimetableRepository.isLoaded) {
        val fromTimetable = TimetableDay.entries
            .flatMap { TimetableRepository.getLessonsForDay(it) }
            .map { TimetableRepository.resolveSubjectName(it.subject).trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        fromTimetable.ifEmpty {
            listOf("Deutsch", "Mathematik", "Englisch", "Biologie", "Chemie", "Physik", "Geschichte", "Politik & Wirtschaft")
        }
    }

    val nextLesson = remember(selectedSubject, TimetableRepository.isLoaded) {
        findNextLesson(subject = selectedSubject)
    }

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

    fun applyDueDateMode(mode: WearDueDateOption) {
        selectedDueDateMode = mode
        when (mode) {
            WearDueDateOption.NONE -> {
                customDate = null
                selectedHour = null
                selectedMinute = null
            }
            WearDueDateOption.TODAY -> {
                customDate = getTodayDate()
            }
            WearDueDateOption.TOMORROW -> {
                customDate = getTodayDate().plusDays(1)
            }
            WearDueDateOption.NEXT_LESSON -> {
                val nl = findNextLesson(subject = selectedSubject)
                if (nl != null) {
                    customDate = nl.date
                    selectedHour = nl.startHour
                    selectedMinute = nl.startMinute
                }
            }
            WearDueDateOption.CUSTOM -> {
                subView = CreateTaskSubView.PICK_DATE
            }
        }
    }

    fun saveTask() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank() || isSaving) return

        keyboardController?.hide()
        isSaving = true
        statusMessage = null

        val resolvedDate: SimpleDate? = when (selectedDueDateMode) {
            WearDueDateOption.NONE -> null
            WearDueDateOption.TODAY -> getTodayDate()
            WearDueDateOption.TOMORROW -> getTodayDate().plusDays(1)
            WearDueDateOption.NEXT_LESSON -> nextLesson?.date ?: customDate
            WearDueDateOption.CUSTOM -> customDate
        }

        val dueDateStr = resolvedDate?.formattedCompletedGerman() ?: ""
        val dueTimeStr = if (selectedHour != null && selectedMinute != null) {
            "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
        } else {
            ""
        }

        val newTask = TaskItem(
            id = getCurrentEpochMillis().toString(),
            title = trimmedTitle,
            description = description.trim(),
            subject = selectedSubject,
            dueDate = dueDateStr,
            dueTime = dueTimeStr,
            isStarred = isStarred,
            isSchoolTask = false,
            listId = selectedListId
        )

        TasksRepository.addTask(newTask)

        if (!BackendApiClient.isLoggedIn) {
            isSaving = false
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

    BackHandler(enabled = subView != CreateTaskSubView.FORM) {
        subView = CreateTaskSubView.FORM
    }

    when (subView) {
        CreateTaskSubView.PICK_SUBJECT -> {
            WearSubjectPickerView(
                subjects = availableSubjects,
                selectedSubject = selectedSubject,
                onSelect = { subject ->
                    selectedSubject = subject
                    if (selectedDueDateMode == WearDueDateOption.NEXT_LESSON) {
                        val nl = findNextLesson(subject = subject)
                        if (nl != null) {
                            customDate = nl.date
                            selectedHour = nl.startHour
                            selectedMinute = nl.startMinute
                        }
                    }
                    subView = CreateTaskSubView.FORM
                },
                onBack = { subView = CreateTaskSubView.FORM }
            )
        }

        CreateTaskSubView.PICK_DATE -> {
            val todayLocal = remember { LocalDate.now() }
            val initialLocalDate = remember(customDate, todayLocal) {
                customDate?.let {
                    runCatching { LocalDate.of(it.year, it.monthNumber, it.dayOfMonth) }.getOrNull()
                }?.takeIf { !it.isBefore(todayLocal) } ?: todayLocal
            }
            DatePicker(
                initialDate = initialLocalDate,
                minValidDate = todayLocal,
                onDatePicked = { picked ->
                    customDate = SimpleDate(picked.year, picked.monthValue, picked.dayOfMonth)
                    selectedDueDateMode = WearDueDateOption.CUSTOM
                    subView = CreateTaskSubView.FORM
                }
            )
        }

        CreateTaskSubView.PICK_TIME -> {
            val initialLocalTime = remember(selectedHour, selectedMinute) {
                LocalTime.of((selectedHour ?: 8).coerceIn(0, 23), (selectedMinute ?: 0).coerceIn(0, 59))
            }
            TimePicker(
                initialTime = initialLocalTime,
                timePickerType = TimePickerType.HoursMinutes24H,
                onTimePicked = { picked ->
                    selectedHour = picked.hour
                    selectedMinute = picked.minute
                    if (selectedDueDateMode == WearDueDateOption.NONE) {
                        selectedDueDateMode = WearDueDateOption.TODAY
                        customDate = getTodayDate()
                    }
                    subView = CreateTaskSubView.FORM
                }
            )
        }

        CreateTaskSubView.FORM -> {
            val dateLabel = when (selectedDueDateMode) {
                WearDueDateOption.NONE -> "Kein Datum"
                WearDueDateOption.TODAY -> "Heute"
                WearDueDateOption.TOMORROW -> "Morgen"
                WearDueDateOption.NEXT_LESSON -> nextLesson?.let {
                    "${it.date.formattedCompletedGerman()} • ${it.startTimeFormatted}"
                } ?: "Keine Stunde gefunden"
                WearDueDateOption.CUSTOM -> customDate?.formattedCompletedGerman() ?: "Datum wählen"
            }

            val timeLabel = if (selectedHour != null && selectedMinute != null) {
                "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')} Uhr"
            } else {
                "Keine Uhrzeit"
            }

            WearCreateTaskScreenContent(
                title = title,
                onTitleChange = {
                    title = it
                    statusMessage = null
                },
                description = description,
                onDescriptionChange = { description = it },
                selectedSubject = selectedSubject,
                onPickSubjectClick = { subView = CreateTaskSubView.PICK_SUBJECT },
                selectedDueDate = selectedDueDateMode,
                dateDisplayLabel = dateLabel,
                nextLessonPreview = nextLesson?.let {
                    "${it.date.formattedCompletedGerman()}, ${it.startTimeFormatted} Uhr"
                },
                onDueDateChange = { applyDueDateMode(it) },
                timeDisplayLabel = timeLabel,
                onPickTimeClick = { subView = CreateTaskSubView.PICK_TIME },
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
                detailsFocusRequester = detailsFocusRequester,
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
    }
}

@Composable
fun WearCreateTaskScreenContent(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedSubject: String,
    onPickSubjectClick: () -> Unit,
    selectedDueDate: WearDueDateOption,
    dateDisplayLabel: String,
    nextLessonPreview: String?,
    onDueDateChange: (WearDueDateOption) -> Unit,
    timeDisplayLabel: String,
    onPickTimeClick: () -> Unit,
    isStarred: Boolean,
    onStarredChange: (Boolean) -> Unit,
    listTitle: String,
    hasMultipleLists: Boolean,
    onCycleList: () -> Unit,
    isSaving: Boolean,
    statusMessage: String?,
    focusRequester: FocusRequester = remember { FocusRequester() },
    detailsFocusRequester: FocusRequester = remember { FocusRequester() },
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
                        fontFamily = GoogleSansRounded,
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
                        fontFamily = GoogleSansRounded,
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
                                fontFamily = GoogleSansRounded,
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
                                fontFamily = GoogleSansRounded,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { detailsFocusRequester.requestFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

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

            // Details / Beschreibung
            item(key = "details_input") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(
                            width = 1.dp,
                            color = if (description.isNotBlank()) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { detailsFocusRequester.requestFocus() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (description.isEmpty()) {
                        Text(
                            text = "Details hinzufügen…",
                            fontFamily = GoogleSansRounded,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        maxLines = 3,
                        textStyle = TextStyle(
                            fontFamily = GoogleSansRounded,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(detailsFocusRequester)
                    )
                }
            }

            // Fach auswählen
            item(key = "subject_picker") {
                val subjectColor = if (selectedSubject.isNotBlank()) {
                    SubjectColor.getColor(
                        subject = selectedSubject,
                        isDark = true,
                        fallback = MaterialTheme.colorScheme.primary
                    )
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                FilledTonalButton(
                    onClick = onPickSubjectClick,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(subjectColor)
                        )
                    },
                    label = {
                        Text(
                            text = selectedSubject.ifBlank { "Fach wählen" },
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (selectedSubject.isBlank()) "Optional" else "Fach • Tippen zum Ändern",
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            // Fälligkeitsdatum
            item(key = "due_date_label") {
                Text(
                    text = "Fälligkeit",
                    fontFamily = GoogleSansRounded,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Schnellauswahl: Ohne / Heute / Morgen
            item(key = "due_date_quick") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(WearDueDateOption.NONE, WearDueDateOption.TODAY, WearDueDateOption.TOMORROW).forEach { option ->
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
                                .defaultMinSize(minHeight = 46.dp)
                        ) {
                            Text(
                                text = option.label,
                                fontFamily = GoogleSansRounded,
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

            // Option: "Bis zur nächsten Stunde"
            item(key = "next_lesson_option") {
                val isSelected = selectedDueDate == WearDueDateOption.NEXT_LESSON
                FilledTonalButton(
                    onClick = { onDueDateChange(WearDueDateOption.NEXT_LESSON) },
                    enabled = nextLessonPreview != null,
                    colors = if (isSelected) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Bis zur nächsten Stunde",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = nextLessonPreview ?: "Keine nächste Stunde im Plan",
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            // Eigenes Datum wählen
            item(key = "custom_date_picker") {
                FilledTonalButton(
                    onClick = { onDueDateChange(WearDueDateOption.CUSTOM) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar_event),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Datum wählen",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = dateDisplayLabel,
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            // Uhrzeit wählen
            item(key = "time_picker") {
                FilledTonalButton(
                    onClick = onPickTimeClick,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Uhrzeit",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = timeDisplayLabel,
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
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
                        fontFamily = GoogleSansRounded,
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
                                fontFamily = GoogleSansRounded,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = listTitle,
                                fontFamily = GoogleSansRounded,
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
                        fontFamily = GoogleSansRounded,
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

@Composable
private fun WearSubjectPickerView(
    subjects: List<String>,
    selectedSubject: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onBack,
                buttonSize = EdgeButtonSize.Small
            ) {
                Text("Zurück", fontFamily = GoogleSansRounded, fontSize = 12.sp)
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
            item(key = "sub_header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Text("Fach wählen", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium)
                }
            }

            item(key = "no_subject") {
                FilledTonalButton(
                    onClick = { onSelect("") },
                    colors = if (selectedSubject.isBlank()) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    Text("Kein Fach", fontFamily = GoogleSansRounded)
                }
            }

            items(items = subjects, key = { it }) { subject ->
                val isSelected = subject.equals(selectedSubject, ignoreCase = true)
                val color = SubjectColor.getColor(subject, isDark = true, fallback = MaterialTheme.colorScheme.primary)
                FilledTonalButton(
                    onClick = { onSelect(subject) },
                    colors = if (isSelected) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    },
                    label = {
                        Text(
                            text = subject,
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearCreateTaskScreenPreview() {
    WearAppTheme(useDynamicColors = false) {
        WearCreateTaskScreenContent(
            title = "Bio Protokoll abgeben",
            onTitleChange = {},
            description = "Seite 42 Aufgaben 1-3",
            onDescriptionChange = {},
            selectedSubject = "Biologie",
            onPickSubjectClick = {},
            selectedDueDate = WearDueDateOption.NEXT_LESSON,
            dateDisplayLabel = "28.11.2026 • 09:45",
            nextLessonPreview = "28.11.2026, 09:45 Uhr",
            onDueDateChange = {},
            timeDisplayLabel = "09:45 Uhr",
            onPickTimeClick = {},
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
