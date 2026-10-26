package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.getCurrentEpochMillis
import de.kjltowinkler.msapp.theme.*
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Moderner Desktop-Modal-Dialog für die Erstellung neuer Aufgaben.
 * Ersetzt das mobile Bottom-Sheet auf Desktop-Bildschirmen.
 */
@Composable
fun DesktopNewTaskDialog(
    initialIsStarred: Boolean = false,
    onDismissRequest: () -> Unit,
    onSaveTask: (TaskItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isStarred by remember(initialIsStarred) { mutableStateOf(initialIsStarred) }
    var isSchoolTask by remember { mutableStateOf(true) }
    var selectedDateText by remember { mutableStateOf("Heute") }

    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

    val submitTask: () -> Unit = {
        if (title.isNotBlank()) {
            val newTask = TaskItem(
                id = "task_${getCurrentEpochMillis()}",
                title = title.trim(),
                description = description.trim(),
                isCompleted = false,
                createdDate = "Heute",
                deadlineDate = selectedDateText,
                isStarred = isStarred,
                isSchoolTask = isSchoolTask
            )
            onSaveTask(newTask)
            onDismissRequest()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = DesktopCardBackground, // #0B0B0D
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2F3A)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Neue Aufgabe erstellen",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = DesktopTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Stern-Button (Priorität)
                    IconButton(
                        onClick = { isStarred = !isStarred },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            painter = painterResource(if (isStarred) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline),
                            contentDescription = "Favorit",
                            tint = if (isStarred) Color(0xFFFFC107) else DesktopTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Schließen Icon
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = "Schließen",
                            tint = DesktopTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Titel Eingabefeld
                Text(
                    text = "TITEL DER AUFGABE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = DesktopInputPlaceholder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF16171D))
                        .border(1.dp, Color(0xFF2C2F3A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = "z. B. Mathe Seite 45 Nr. 2",
                            style = TextStyle(fontSize = 14.5.sp, color = DesktopInputPlaceholder)
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = DesktopTextPrimary
                        ),
                        cursorBrush = SolidColor(DesktopAccentBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { submitTask() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Beschreibung Eingabefeld
                Text(
                    text = "BESCHREIBUNG / NOTIZEN (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = DesktopInputPlaceholder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF16171D))
                        .border(1.dp, Color(0xFF2C2F3A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (description.isEmpty()) {
                        Text(
                            text = "Zusätzliche Details, Quellen oder Links eingeben...",
                            style = TextStyle(fontSize = 13.5.sp, color = DesktopInputPlaceholder)
                        )
                    }
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = TextStyle(
                            fontSize = 13.5.sp,
                            color = DesktopTextPrimary
                        ),
                        cursorBrush = SolidColor(DesktopAccentBlue),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schnellauswahl Fälligkeitsdatum & Liste
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Datum Chips
                    val dates = listOf("Heute", "Morgen", "Nächste Woche")
                    dates.forEach { dateLabel ->
                        val isDateSelected = selectedDateText == dateLabel
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isDateSelected) DesktopAccountChipBackground else Color(0xFF1E2129)
                                )
                                .border(
                                    1.dp,
                                    if (isDateSelected) DesktopAccentBlue else Color(0xFF2C2F3A),
                                    RoundedCornerShape(8.dp)
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { selectedDateText = dateLabel }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isDateSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isDateSelected) DesktopAccentBlue else DesktopTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Listen-Typ Toggle (Schulaufgaben / Meine Aufgaben)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2129))
                            .border(1.dp, Color(0xFF2C2F3A), RoundedCornerShape(8.dp))
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { isSchoolTask = !isSchoolTask }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSchoolTask) "Schulaufgaben" else "Meine Aufgaben",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                color = DesktopTextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = "Abbrechen",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                color = DesktopTextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = submitTask,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesktopAccentBlue,
                            contentColor = DesktopOnAccentBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        enabled = title.isNotBlank(),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = "Aufgabe anlegen",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
