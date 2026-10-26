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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * BottomSheet für das Optionen-Menü im Tab "Markiert".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredTasksBottomSheet(
    onDismissRequest: () -> Unit,
    onDeleteAllStarred: () -> Unit,
    onDeleteCompletedStarred: () -> Unit = {},
    onUnstarAll: () -> Unit,
    onMarkAllCompleted: () -> Unit,
    onMarkAllUncompleted: () -> Unit = {},
    areAllCompleted: Boolean = false,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteCompletedConfirmDialog by remember { mutableStateOf(false) }

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

    // Bestätigungs-Dialog: Alle markierten Aufgaben löschen
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = {
                Text(
                    text = "Alle markierten Aufgaben löschen?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Möchtest du wirklich alle markierten privaten Aufgaben unwiderruflich löschen?\n\nHinweis: Markierte Schulaufgaben können nicht gelöscht werden und bleiben erhalten.",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllStarred()
                        showDeleteAllConfirmDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text("Löschen", color = colorScheme.error, fontWeight = FontWeight.Bold)
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

    // Bestätigungs-Dialog: Alle erledigten Aufgaben löschen
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
                    text = "Möchtest du alle erledigten Aufgaben aus deinen privaten Listen unwiderruflich löschen? (Schulaufgaben bleiben geschützt).",
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCompletedStarred()
                        showDeleteCompletedConfirmDialog = false
                        onDismissRequest()
                    }
                ) {
                    Text("Löschen", color = colorScheme.error, fontWeight = FontWeight.Bold)
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
                text = "Markiert Optionen",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. BLOCK: Status & Markierungen ---
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

                // Option: Alle Markierungen entfernen (unterer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bottomBrickShape)
                        .background(brickBackgroundColor)
                        .clickable {
                            onUnstarAll()
                            onDismissRequest()
                        }
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_star_outline),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Alle Markierungen entfernen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. BLOCK: Lösch- & Aufräum-Aktionen ---
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

                // Aktion 2: Alle privaten Aufgaben löschen (unterer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bottomBrickShape)
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
                        text = "Alle privaten Aufgaben löschen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = destructiveIconTint
                    )
                }
            }
        }
    }
}
