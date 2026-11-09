package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.getCurrentEpochMillis
import de.kjltowinkler.msapp.theme.getGoogleSansFlexFontFamily
import de.kjltowinkler.msapp.ui.components.PredictiveBackContainer
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.ui.components.textCursor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import de.kjltowinkler.msapp.data.task.TasksRepository

/**
 * Bottom-Sheet / Bottom-Bar "Neue Aufgaben" exakt nach Figma: tasks#neu+detail.png
 * 
 * Wird direkt im Window-Layout gerendert (ohne Dialog-Window-Latenz),
 * damit es sich über imePadding() perfekt synchron mit der Tastatur hoch- und herunterschiebt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskBottomSheet(
    initialIsStarred: Boolean = false,
    initialIsSchoolTask: Boolean = false,
    initialSubject: String = "",
    targetListId: String = "my_tasks",
    onDismissRequest: () -> Unit,
    onSaveTask: (TaskItem) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedSubject by remember(initialSubject) { mutableStateOf(initialSubject) }
    var isStarred by remember(initialIsStarred) { mutableStateOf(initialIsStarred) }

    var selectedDate by remember { mutableStateOf<SimpleDate?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSubjectPicker by remember { mutableStateOf(false) }

    var showDescription by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val detailsFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(showDescription) {
        if (showDescription) {
            detailsFocusRequester.requestFocus()
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    PredictiveBackContainer(
        enabled = !showDatePicker && !showTimePicker && !showSubjectPicker,
        onBack = {
            keyboardController?.hide()
            onDismissRequest()
        },
        maxScaleReduction = 0.05f,
        modifier = Modifier.fillMaxSize()
    ) { backProgress ->
        // Abdunkelungs-Hintergrund (Scrim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ((1f - backProgress) * 0.5f).coerceAtLeast(0f)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    keyboardController?.hide()
                    onDismissRequest()
                }
        )

        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomInset = maxOf(imeBottom, navBottom)

        // Das Menu selbst dockt unten an und reagiert simultan auf die Tastatur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset),
            contentAlignment = Alignment.BottomCenter
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Verhindert Klicks durchs Menü auf den Scrim */ },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 16.dp)
            ) {
                // --- 1. Titel-Eingabezeile ("Neue Aufgaben") ---
                val titleTextStyle = TextStyle(
                    fontFamily = getGoogleSansFlexFontFamily(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                BasicTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .textCursor(),
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (taskTitle.isEmpty()) {
                                Text(
                                    text = "Neue Aufgaben",
                                    style = titleTextStyle.copy(
                                        color = colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = if (showDescription) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (showDescription) {
                                detailsFocusRequester.requestFocus()
                            }
                        }
                    )
                )

                // --- 2. Details-Eingabezeile ("Details hinzufügen") - erscheint nur bei Klick auf das Details/Sort-Icon ---
                AnimatedVisibility(visible = showDescription) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        val detailsTextStyle = TextStyle(
                            fontFamily = getGoogleSansFlexFontFamily(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorScheme.onSurface
                        )
                        BasicTextField(
                            value = taskDescription,
                            onValueChange = { taskDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(detailsFocusRequester)
                                .textCursor(),
                            textStyle = detailsTextStyle,
                            cursorBrush = SolidColor(colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (taskDescription.isEmpty()) {
                                        Text(
                                            text = "Details hinzufügen",
                                            style = detailsTextStyle.copy(
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }

                // Anzeige von gewähltem Fach / Datum / Uhrzeit Chips (falls gesetzt)
                if (selectedSubject.isNotBlank() || selectedDate != null || (selectedHour != null && selectedMinute != null)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        if (selectedSubject.isNotBlank()) {
                            NewTaskChip(
                                text = selectedSubject,
                                onClick = { showSubjectPicker = true },
                                onRemove = { selectedSubject = "" },
                                contentDescription = "Fach entfernen"
                            )
                        }

                        selectedDate?.let { date ->
                            NewTaskChip(
                                text = date.formattedCompletedGerman(),
                                onClick = { showDatePicker = true },
                                onRemove = { selectedDate = null },
                                contentDescription = "Datum entfernen"
                            )
                        }

                        if (selectedHour != null && selectedMinute != null) {
                            NewTaskChip(
                                text = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')} Uhr",
                                onClick = { showTimePicker = true },
                                onRemove = {
                                    selectedHour = null
                                    selectedMinute = null
                                },
                                contentDescription = "Uhrzeit entfernen"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 3. ICONS-LEISTE & SPEICHERN BUTTON ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Linke Seite: Icons [Details/Sort], [Datum], [Uhrzeit], [Stern]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Icon 1: Details / Subject (3 waagerechte Striche - blendet "Details hinzufügen" ein/aus)
                        IconButton(
                            onClick = {
                                showDescription = !showDescription
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_subject),
                                contentDescription = "Details hinzufügen",
                                tint = if (showDescription) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Icon 2: Datum auswählen (Kalender)
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_calendar_event),
                                contentDescription = "Datum auswählen",
                                tint = if (selectedDate != null) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Icon 3: Uhrzeit auswählen (Uhr / History)
                        IconButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_access_time),
                                contentDescription = "Uhrzeit auswählen",
                                tint = if (selectedHour != null) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Icon 4: Fach auswählen (9 Punkte / 3x3 Grid / Apps)
                        IconButton(
                            onClick = { showSubjectPicker = true },
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_apps),
                                contentDescription = "Fach auswählen",
                                tint = if (selectedSubject.isNotBlank()) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Icon 5: Stern / Markieren
                        IconButton(
                            onClick = { isStarred = !isStarred },
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isStarred) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                                ),
                                contentDescription = "Als wichtig markieren",
                                tint = if (isStarred) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Rechte Seite: "Speichern" Button
                    TextButton(
                        onClick = {
                            val trimmedTitle = taskTitle.trim()
                            if (trimmedTitle.isNotBlank()) {
                                val dueTimeString = if (selectedHour != null && selectedMinute != null) {
                                    "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
                                } else ""
                                val dueDateString = selectedDate?.formattedCompletedGerman() ?: ""

                                val newTask = TaskItem(
                                    id = getCurrentEpochMillis().toString(),
                                    title = trimmedTitle,
                                    description = taskDescription.trim(),
                                    subject = selectedSubject,
                                    dueDate = dueDateString,
                                    dueTime = dueTimeString,
                                    isStarred = isStarred,
                                    isSchoolTask = initialIsSchoolTask,
                                    listId = if (initialIsSchoolTask) "my_tasks" else targetListId
                                )
                                onSaveTask(newTask)
                                keyboardController?.hide()
                                onDismissRequest()
                            }
                        },
                        enabled = taskTitle.isNotBlank(),
                        modifier = Modifier.handCursor(taskTitle.isNotBlank())
                    ) {
                        Text(
                            text = "Speichern",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (taskTitle.isNotBlank()) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
    }

    // --- SUBJECT PICKER DIALOG (tasks#neu+fach.png) ---
    if (showSubjectPicker) {
        TaskSubjectPickerDialog(
            selectedSubject = selectedSubject,
            onSubjectSelected = { selectedSubject = it },
            onDismissRequest = { showSubjectPicker = false }
        )
    }

    // --- DATE PICKER DIALOG ---
    if (showDatePicker) {
        TaskDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismissRequest = { showDatePicker = false }
        )
    }

    // --- TIME PICKER DIALOG ---
    if (showTimePicker) {
        TaskTimePickerDialog(
            initialHour = selectedHour ?: 7,
            initialMinute = selectedMinute ?: 0,
            onTimeSelected = { h, m ->
                selectedHour = h
                selectedMinute = m
            },
            onDismissRequest = { showTimePicker = false }
        )
    }
}

/**
 * Kompakter, gerahmter Chip für Fach, Zeit und Datum beim Erstellen einer Aufgabe.
 * Entspricht dem Design mit 8.dp abgerundeten Ecken, feinem Rahmen, Primärfarb-Akzent
 * und integriertem Schließen-Icon.
 */
@Composable
private fun NewTaskChip(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onClick() }
                    .padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 6.dp)
                    .handCursor()
            ) {
                Text(
                    text = text,
                    fontFamily = getGoogleSansFlexFontFamily(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 14.dp)
                    ) { onRemove() }
                    .padding(start = 4.dp, top = 6.dp, end = 10.dp, bottom = 6.dp)
                    .handCursor(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = contentDescription,
                    tint = colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
