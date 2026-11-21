package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
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
import de.kjltowinkler.msapp.data.task.SharedListInvitation
import de.kjltowinkler.msapp.data.task.TaskMemberRole
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.ui.components.AppSnackbarManager
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun SharedListInvitationDialog(
    invitation: SharedListInvitation,
    onDismissRequest: () -> Unit,
    onAccepted: (String) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val roleLabel = when (invitation.role) {
        TaskMemberRole.EDITOR -> "Mitarbeiter (kann bearbeiten & abhaken)"
        TaskMemberRole.COMMENTER -> "Kommentator (kann ansehen & kommentieren)"
        else -> "Betrachter (nur Lesezugriff)"
    }

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) onDismissRequest()
        },
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_groups),
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Einladung zu geteilter Liste",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "von ${invitation.ownerName}",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = invitation.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = roleLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary
                        )
                    }
                }

                if (invitation.previewTasks.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Vorschau der Aufgaben:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant
                        )

                        invitation.previewTasks.forEach { task ->
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
                    }
                }

                Text(
                    text = "Möchtest du diese Einladung annehmen und der Liste beitreten?",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessing = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = TasksRepository.acceptInvitation(invitation)
                        isProcessing = false
                        if (result.isSuccess) {
                            AppSnackbarManager.showMessage("Liste '${invitation.title}' erfolgreich beigetreten")
                            onAccepted(invitation.listId)
                            onDismissRequest()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Fehler beim Annehmen der Einladung."
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.handCursor()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onPrimary
                    )
                } else {
                    Text("Annehmen")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    isProcessing = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = TasksRepository.declineInvitation(invitation)
                        isProcessing = false
                        if (result.isSuccess) {
                            AppSnackbarManager.showMessage("Einladung zu '${invitation.title}' abgelehnt")
                            onDismissRequest()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Fehler beim Ablehnen der Einladung."
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.handCursor()
            ) {
                Text("Ablehnen", color = colorScheme.error)
            }
        }
    )
}
