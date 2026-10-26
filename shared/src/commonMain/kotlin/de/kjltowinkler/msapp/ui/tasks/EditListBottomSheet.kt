package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * BottomSheet zum Verwalten der Liste "Meine Aufgaben"
 * entsprechend Figma-Design: `tasks#editlistesheet.png`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListBottomSheet(
    listTitle: String = "Meine Aufgaben",
    onDismissRequest: () -> Unit,
    onRenameList: (String) -> Unit = {},
    onMarkAllCompleted: () -> Unit = {},
    onMarkAllUncompleted: () -> Unit = {},
    onMarkAllStarred: () -> Unit = {},
    onUnstarAll: () -> Unit = {},
    onDeleteCompletedTasks: () -> Unit = {},
    onDeleteAllTasks: () -> Unit = {},
    onDeleteList: (() -> Unit)? = null,
    areAllCompleted: Boolean = false,
    areAllStarred: Boolean = false,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteCompletedConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteListConfirmDialog by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme

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

    val topBrickShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
    val middleBrickShape = RoundedCornerShape(3.dp)
    val bottomBrickShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    val singleBrickShape = RoundedCornerShape(20.dp)

    // Dialog: Liste umbenennen
    if (showRenameDialog) {
        var textFieldValue by remember(listTitle) {
            mutableStateOf(
                TextFieldValue(
                    text = listTitle,
                    selection = TextRange(0, listTitle.length)
                )
            )
        }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            delay(50)
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = "Liste umbenennen",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = textFieldValue.text.trim()
                        if (trimmed.isNotBlank()) {
                            onRenameList(trimmed)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Speichern", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Abbrechen", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // Dialog: Alle erledigten Aufgaben löschen
    if (showDeleteCompletedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedConfirmDialog = false },
            title = {
                Text(
                    text = "Erledigte Aufgaben löschen?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Möchtest du alle erledigten Aufgaben aus dieser Liste entfernen?",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteCompletedConfirmDialog = false
                        onDeleteCompletedTasks()
                        onDismissRequest()
                    }
                ) {
                    Text("Löschen", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedConfirmDialog = false }) {
                    Text("Abbrechen", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // Dialog: Alle Aufgaben löschen
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = {
                Text(
                    text = "Alle Aufgaben löschen?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Möchtest du wirklich alle Aufgaben aus dieser Liste unwiderruflich löschen?",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirmDialog = false
                        onDeleteAllTasks()
                        onDismissRequest()
                    }
                ) {
                    Text("Löschen", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Abbrechen", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // Dialog: Liste löschen
    if (showDeleteListConfirmDialog && onDeleteList != null) {
        AlertDialog(
            onDismissRequest = { showDeleteListConfirmDialog = false },
            title = {
                Text(
                    text = "Liste löschen?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Möchtest du die Liste „$listTitle“ und alle darin enthaltenen Aufgaben wirklich unwiderruflich löschen?",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteListConfirmDialog = false
                        onDeleteList()
                        onDismissRequest()
                    }
                ) {
                    Text("Löschen", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteListConfirmDialog = false }) {
                    Text("Abbrechen", color = colorScheme.onSurfaceVariant)
                }
            },
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
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

            // Titel des Sheets
            Text(
                text = listTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. BLOCK: Liste umbenennen ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(singleBrickShape)
                    .background(brickBackgroundColor)
                    .clickable { showRenameDialog = true }
                    .defaultMinSize(minHeight = 54.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = null,
                    tint = actionIconTint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Liste umbenennen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. BLOCK: Status- & Markierungs-Aktionen ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Aktion 1: Erledigt / Unerledigt markieren (oberer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(topBrickShape)
                        .background(brickBackgroundColor)
                        .clickable {
                            if (areAllCompleted) {
                                onMarkAllUncompleted()
                            } else {
                                onMarkAllCompleted()
                            }
                            onDismissRequest()
                        }
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (areAllCompleted) Res.drawable.ic_event_busy else Res.drawable.ic_check
                        ),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (areAllCompleted) "Alle als unerledigt markieren" else "Alle als erledigt markieren",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }

                // Aktion 2: Alle mit Stern markieren / Markierungen entfernen (unterer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bottomBrickShape)
                        .background(brickBackgroundColor)
                        .clickable {
                            if (areAllStarred) {
                                onUnstarAll()
                            } else {
                                onMarkAllStarred()
                            }
                            onDismissRequest()
                        }
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (areAllStarred) Res.drawable.ic_star_filled else Res.drawable.ic_star_outline
                        ),
                        contentDescription = null,
                        tint = if (areAllStarred) colorScheme.primary else actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (areAllStarred) "Alle Markierungen entfernen" else "Alle mit Stern markieren",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 3. BLOCK: Lösch- & Aufräum-Aktionen ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Aktion 1: Erledigte Aufgaben löschen (oberer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(topBrickShape)
                        .background(brickBackgroundColor)
                        .clickable { showDeleteCompletedConfirmDialog = true }
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_event_busy),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Erledigte Aufgaben löschen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }

                // Aktion 2: Alle Aufgaben löschen (mittlerer Brick wenn onDeleteList != null, sonst unterer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(if (onDeleteList != null) middleBrickShape else bottomBrickShape)
                        .background(brickBackgroundColor)
                        .clickable { showDeleteAllConfirmDialog = true }
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
                        text = "Alle Aufgaben löschen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = destructiveIconTint
                    )
                }

                // Aktion 3: Liste löschen (nur bei benutzerdefinierten Listen, unterer Brick)
                if (onDeleteList != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bottomBrickShape)
                            .background(brickBackgroundColor)
                            .clickable { showDeleteListConfirmDialog = true }
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
                            text = "Liste löschen",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = destructiveIconTint
                        )
                    }
                }
            }
        }
    }
}
