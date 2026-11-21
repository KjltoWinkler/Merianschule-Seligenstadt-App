package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.task.*
import de.kjltowinkler.msapp.ui.components.AppSnackbarManager
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun AcceptSharedListDialog(
    token: String,
    onDismissRequest: () -> Unit,
    onListAccepted: (String) -> Unit
) {
    var isLoadingDetails by remember { mutableStateOf(true) }
    var detailsDto by remember { mutableStateOf<SharedListDetailsDto?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(token) {
        isLoadingDetails = true
        loadError = null
        val result = BackendApiClient.fetchSharedListByToken(token)
        isLoadingDetails = false
        result.onSuccess { details ->
            detailsDto = details
        }.onFailure { err ->
            loadError = err.message ?: "Die geteilte Liste konnte nicht geladen werden oder der Link ist ungültig."
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isJoining) onDismissRequest()
        },
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_link),
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Geteilte Liste",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoadingDetails) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Liste wird geladen...",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else if (loadError != null) {
                    Text(
                        text = loadError ?: "Fehler beim Laden",
                        color = colorScheme.error,
                        fontSize = 14.sp
                    )
                } else if (detailsDto != null) {
                    val info = detailsDto!!.info
                    val tasks = detailsDto!!.tasks
                    val isAlreadyMember = TasksRepository.privateLists.any { it.id == info.listId }

                    // Info-Karte zur Liste
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = info.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_person),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Geteilt von ${info.ownerName}",
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            // Rolle / Berechtigung-Badge
                            val roleText = if (info.linkRole == TaskMemberRole.EDITOR) "Bearbeiter" else "Betrachter"
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = roleText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = "${tasks.size} ${if (tasks.size == 1) "Aufgabe" else "Aufgaben"}",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isAlreadyMember) {
                        Text(
                            text = "Du bist dieser Liste bereits beigetreten.",
                            fontSize = 13.sp,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        // Aufgaben-Vorschau
                        if (tasks.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Vorschau:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurfaceVariant
                                )

                                tasks.take(3).forEach { task ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colorScheme.surfaceContainerLow)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (task.isCompleted) Res.drawable.ic_check_circle else Res.drawable.ic_task_alt
                                            ),
                                            contentDescription = null,
                                            tint = if (task.isCompleted) colorScheme.primary else colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = task.title,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (task.isCompleted) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (tasks.size > 3) {
                                    Text(
                                        text = "+ ${tasks.size - 3} weitere...",
                                        fontSize = 11.sp,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Möchtest du dieser Liste beitreten und sie zu deinen Aufgaben hinzufügen?",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    if (joinError != null) {
                        Text(
                            text = joinError ?: "",
                            fontSize = 12.sp,
                            color = colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (loadError != null) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.handCursor()
                ) {
                    Text("Schließen")
                }
            } else if (detailsDto != null) {
                val info = detailsDto!!.info
                val isAlreadyMember = TasksRepository.privateLists.any { it.id == info.listId }

                if (isAlreadyMember) {
                    Button(
                        onClick = {
                            onListAccepted(info.listId)
                            onDismissRequest()
                        },
                        modifier = Modifier.handCursor()
                    ) {
                        Text("Zur Liste")
                    }
                } else {
                    Button(
                        onClick = {
                            isJoining = true
                            joinError = null
                            coroutineScope.launch {
                                val result = TasksRepository.joinSharedList(token)
                                isJoining = false
                                if (result.isSuccess) {
                                    val res = result.getOrNull()
                                    if (res != null && res.success) {
                                        AppSnackbarManager.showMessage(
                                            res.message.ifBlank { "Liste '${info.title}' erfolgreich beigetreten" }
                                        )
                                        onListAccepted(res.list?.id ?: info.listId)
                                        onDismissRequest()
                                    } else {
                                        joinError = res?.message ?: "Beitreten fehlgeschlagen."
                                    }
                                } else {
                                    joinError = result.exceptionOrNull()?.message ?: "Fehler beim Beitreten."
                                }
                            }
                        },
                        enabled = !isJoining,
                        modifier = Modifier.handCursor()
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.onPrimary
                            )
                        } else {
                            Text("Liste annehmen")
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (loadError == null && detailsDto != null) {
                TextButton(
                    onClick = onDismissRequest,
                    enabled = !isJoining,
                    modifier = Modifier.handCursor()
                ) {
                    Text("Ablehnen")
                }
            }
        }
    )
}
