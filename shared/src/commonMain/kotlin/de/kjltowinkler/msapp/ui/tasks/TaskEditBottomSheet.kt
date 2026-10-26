package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.getTodayDate
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Edit BottomSheet zum Bearbeiten und Einsehen von Aufgabeninformationen
 * exakt nach Figma: `tasks#editsheet.png`.
 *
 * Verhält sich analog zum Informations-BottomSheet aus dem Vertretungsplan
 * (`SubstitutionDetailBottomSheet`) als echtes Material 3 ModalBottomSheet mit Drag-Handle,
 * Swipe-to-Dismiss-Geste und nativer Sheet-Animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditBottomSheet(
    task: TaskItem,
    onDismissRequest: () -> Unit,
    onUpdateTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val coroutineScope = rememberCoroutineScope()

    var title by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description) }
    var isDetailsExpanded by remember(task.id) { mutableStateOf(task.description.isNotBlank()) }

    var deadlineDate by remember(task.id) { mutableStateOf(task.deadlineDate) }
    var dueDate by remember(task.id) { mutableStateOf(task.dueDate) }
    var dueTime by remember(task.id) { mutableStateOf(task.dueTime) }
    var isCompleted by remember(task.id) { mutableStateOf(task.isCompleted) }
    var completedDate by remember(task.id) { mutableStateOf(task.completedDate) }
    var isStarred by remember(task.id) { mutableStateOf(task.isStarred) }
    val createdDate by remember(task.id) {
        mutableStateOf(
            if (task.createdDate.isNotBlank()) task.createdDate else getTodayDate().formattedCompletedGerman()
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // Dynamische Material You-Farben mit Figma-Fallback wie in SubstitutionDetailBottomSheet
    val sheetContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color(0xFF141914)
    }

    val brickBackgroundColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF202820)
    }

    val dragHandleColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        Color(0xFF5E655C)
    }

    val metaIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF9CA599)
    }

    val actionIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFE4EDE0)
    }

    val destructiveIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFFE57373)
    }

    // Brick-Shapes analog zu SubstitutionDetailBottomSheet
    val topBrickShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
    val middleBrickShape = RoundedCornerShape(3.dp)
    val bottomBrickShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    val singleBrickShape = RoundedCornerShape(20.dp)

    // Speichern bei jeder relevanten Änderung
    fun saveChanges(
        newTitle: String = title,
        newDescription: String = description,
        newDeadline: String = deadlineDate,
        newDueDate: String = dueDate,
        newDueTime: String = dueTime,
        newIsCompleted: Boolean = isCompleted,
        newCompletedDate: String = completedDate,
        newIsStarred: Boolean = isStarred
    ) {
        val updated = task.copy(
            title = newTitle.ifBlank { task.title },
            description = newDescription,
            deadlineDate = newDeadline,
            dueDate = newDueDate,
            dueTime = newDueTime,
            isCompleted = newIsCompleted,
            completedDate = newCompletedDate,
            createdDate = createdDate,
            isStarred = newIsStarred
        )
        onUpdateTask(updated)
    }

    ModalBottomSheet(
        onDismissRequest = {
            saveChanges()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            // Drag-Griff analog zum Vertretungsplan-BottomSheet
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(dragHandleColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

                // --- 2. AUFGABENTITEL ---
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        saveChanges(newTitle = it)
                    },
                    textStyle = TextStyle(
                        color = colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. STATUSZEILEN (Erstellt, Erledigt, Fristende) ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Erstellt: Di., 13. Okt.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_task_alt),
                            contentDescription = null,
                            tint = metaIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Erstellt: $createdDate",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    // Erledigt: Mi., 14. Okt. (nur anzeigen wenn erledigt)
                    if (isCompleted) {
                        val displayCompleted = if (completedDate.isNotBlank()) {
                            completedDate
                        } else {
                            getTodayDate().formattedCompletedGerman()
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = metaIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Erledigt: $displayCompleted",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Fristende: Do., 15. Okt. (nur anzeigen wenn Frist gesetzt ist)
                    if (deadlineDate.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_timer),
                                contentDescription = null,
                                tint = metaIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Fristende: $deadlineDate",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 4. OPTIONEN-GRUPPE (Details & Frist als Bricks mit Abstand genau wie im Vertretungsplan) ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Box 1: Details hinzufügen / bearbeiten (oberer Brick)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(topBrickShape)
                            .background(brickBackgroundColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailsExpanded = !isDetailsExpanded }
                                .defaultMinSize(minHeight = 54.dp)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_subject),
                                contentDescription = null,
                                tint = actionIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (description.isNotBlank()) "Details bearbeiten" else "Details hinzufügen",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        }

                        AnimatedVisibility(
                            visible = isDetailsExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            BasicTextField(
                                value = description,
                                onValueChange = {
                                    description = it
                                    saveChanges(newDescription = it)
                                },
                                textStyle = TextStyle(
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                decorationBox = { innerTextField ->
                                    if (description.isEmpty()) {
                                        Text(
                                            text = "Details hier eingeben...",
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Box 2: Datum hinzufügen (mittlerer Brick)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(middleBrickShape)
                            .background(brickBackgroundColor)
                            .clickable { showDatePicker = true }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_today),
                                contentDescription = null,
                                tint = actionIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (dueDate.isNotBlank()) "Datum: $dueDate" else "Datum hinzufügen",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        }

                        if (dueDate.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    dueDate = ""
                                    val newDeadline = if (dueTime.isNotBlank()) "$dueTime Uhr" else ""
                                    deadlineDate = newDeadline
                                    saveChanges(newDueDate = "", newDeadline = newDeadline)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_close),
                                    contentDescription = "Datum entfernen",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Box 3: Zeit hinzufügen (unterer Brick)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bottomBrickShape)
                            .background(brickBackgroundColor)
                            .clickable { showTimePicker = true }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_schedule),
                                contentDescription = null,
                                tint = actionIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (dueTime.isNotBlank()) "Zeit: $dueTime Uhr" else "Zeit hinzufügen",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        }

                        if (dueTime.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    dueTime = ""
                                    val newDeadline = dueDate
                                    deadlineDate = newDeadline
                                    saveChanges(newDueTime = "", newDeadline = newDeadline)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_close),
                                    contentDescription = "Zeit entfernen",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- 5. AKTIONEN-GRUPPE (Aufgabe markieren, Teilen, Als erledigt markieren) ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Aktion 1: Aufgabe markieren / Markierung entfernen (oberer Brick)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(topBrickShape)
                            .background(brickBackgroundColor)
                            .clickable {
                                val newStarred = !isStarred
                                isStarred = newStarred
                                saveChanges(newIsStarred = newStarred)
                            }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isStarred) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                            ),
                            contentDescription = null,
                            tint = if (isStarred) colorScheme.primary else actionIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isStarred) "Markierung entfernen" else "Aufgabe markieren",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }

                    // Aktion 2: Teilen (mittlerer Brick)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(middleBrickShape)
                            .background(brickBackgroundColor)
                            .clickable {
                                // Teilen - Backend-Integration folgt
                            }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_share),
                            contentDescription = null,
                            tint = actionIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Teilen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }

                    // Aktion 3: Als nicht erledigt markieren / Als erledigt markieren (unterer Brick)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bottomBrickShape)
                            .background(brickBackgroundColor)
                            .clickable {
                                val newCompleted = !isCompleted
                                if (newCompleted) {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    } catch (_: Exception) {
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    }
                                }
                                val newCompletedDate = if (newCompleted) getTodayDate().formattedCompletedGerman() else ""
                                isCompleted = newCompleted
                                completedDate = newCompletedDate
                                saveChanges(newIsCompleted = newCompleted, newCompletedDate = newCompletedDate)
                            }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isCompleted) Res.drawable.ic_event_busy else Res.drawable.ic_check
                            ),
                            contentDescription = null,
                            tint = actionIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isCompleted) "Als nicht erledigt markieren" else "Als erledigt markieren",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                }

                // --- 6. BUTTON: Löschen (Rot akzentuiert - nur für eigene Aufgaben, nicht für Schulaufgaben) ---
                if (!task.isSchoolTask) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(singleBrickShape)
                            .background(brickBackgroundColor)
                            .clickable {
                                showDeleteConfirmDialog = true
                            }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_delete),
                            contentDescription = null,
                            tint = destructiveIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Löschen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = destructiveIconTint
                        )
                    }
                }
        }
    }

    // --- BESTÄTIGUNGSDIALOG: Aufgabe löschen ---
    if (showDeleteConfirmDialog && !task.isSchoolTask) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Aufgabe löschen?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (task.title.isNotBlank()) {
                        "Möchtest du „${task.title}“ wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden."
                    } else {
                        "Möchtest du diese Aufgabe wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden."
                    },
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!task.isSchoolTask) {
                                onDeleteTask(task.id)
                            }
                            onDismissRequest()
                        }
                    }
                ) {
                    Text(
                        text = "Löschen",
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text(
                        text = "Abbrechen",
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // --- DIALOGS (Datum & Zeit) ---
    if (showDatePicker) {
        TaskDatePickerDialog(
            selectedDate = null,
            onDateSelected = { date ->
                showDatePicker = false
                if (date != null) {
                    val formatted = date.formattedCompletedGerman()
                    dueDate = formatted
                    val combinedDeadline = buildString {
                        append(formatted)
                        if (dueTime.isNotBlank()) {
                            append(" • $dueTime Uhr")
                        }
                    }
                    deadlineDate = combinedDeadline
                    saveChanges(newDueDate = formatted, newDeadline = combinedDeadline)
                }
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val initialHour = dueTime.split(":").firstOrNull()?.toIntOrNull() ?: 12
        val initialMinute = dueTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0

        TaskTimePickerDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onTimeSelected = { h, m ->
                showTimePicker = false
                val timeStr = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                dueTime = timeStr
                val combinedDeadline = buildString {
                    if (dueDate.isNotBlank()) {
                        append(dueDate)
                        append(" • ")
                    }
                    append("$timeStr Uhr")
                }
                deadlineDate = combinedDeadline
                saveChanges(newDueTime = timeStr, newDeadline = combinedDeadline)
            },
            onDismissRequest = { showTimePicker = false }
        )
    }
}
