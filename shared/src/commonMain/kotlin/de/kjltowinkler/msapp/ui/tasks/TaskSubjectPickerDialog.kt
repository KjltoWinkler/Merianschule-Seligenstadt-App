package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.kjltowinkler.msapp.data.courses.CourseRepository
import de.kjltowinkler.msapp.ui.components.handCursor

/**
 * Dialog zur Auswahl des Fachs für eine Aufgabe gemäß Figma tasks#neu+fach.png.
 */
@Composable
fun TaskSubjectPickerDialog(
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val defaultSubjects = listOf(
        "Deutsch",
        "Englisch",
        "Mathematik",
        "Ethik",
        "Biologie",
        "Arbeitslehre"
    )

    val userCourseSubjects = CourseRepository.courses.map { it.subject }.filter { it.isNotBlank() }
    val allSubjects = (defaultSubjects + userCourseSubjects).distinct()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 340.dp)
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Klicks auf der Karte nicht weiterleiten */ },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allSubjects.forEach { subject ->
                        val isSelected = subject.equals(selectedSubject, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isSelected) {
                                        onSubjectSelected("")
                                    } else {
                                        onSubjectSelected(subject)
                                    }
                                    onDismissRequest()
                                }
                                .handCursor()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = subject,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
