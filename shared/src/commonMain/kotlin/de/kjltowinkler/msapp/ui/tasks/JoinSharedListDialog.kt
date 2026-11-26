package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.AppSnackbarManager
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun JoinSharedListDialog(
    onDismissRequest: () -> Unit,
    onListJoined: (String) -> Unit = {}
) {
    var linkInput by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_link),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Geteilter Liste beitreten",
                    fontWeight = FontWeight.Normal,
                    fontFamily = getQuicksandFontFamily(),
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Füge hier den Freigabe-Link (z. B. veloschulplaner.de/share/...) oder den Freigabe-Code ein:",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = linkInput,
                    onValueChange = {
                        linkInput = it
                        errorMessage = null
                    },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("veloschulplaner.de/share/...", fontSize = 13.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
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
                    val clean = linkInput.trim()
                    if (clean.isBlank()) {
                        errorMessage = "Bitte gib einen Link oder Code ein."
                        return@Button
                    }
                    isJoining = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = TasksRepository.joinSharedList(clean)
                        isJoining = false
                        if (result.isSuccess) {
                            val res = result.getOrNull()
                            if (res != null && res.success) {
                                AppSnackbarManager.showMessage(res.message)
                                onListJoined(res.list?.id ?: "")
                                onDismissRequest()
                            } else {
                                errorMessage = res?.message ?: "Beitreten fehlgeschlagen."
                            }
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Fehler beim Beitreten."
                        }
                    }
                },
                enabled = !isJoining && linkInput.isNotBlank(),
                modifier = Modifier.handCursor()
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onPrimary
                    )
                } else {
                    Text("Beitreten")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.handCursor()
            ) {
                Text("Abbrechen")
            }
        }
    )
}
