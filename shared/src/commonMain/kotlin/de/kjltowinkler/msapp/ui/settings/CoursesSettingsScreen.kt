package de.kjltowinkler.msapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.courses.CourseRepository
import de.kjltowinkler.msapp.data.courses.UserCourse
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.PredictiveBackContainer
import de.kjltowinkler.msapp.ui.components.handCursor
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesSettingsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    useMaterialYou: Boolean = true
) {
    val courses = CourseRepository.courses
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<UserCourse?>(null) }

    val screenBackground = MaterialTheme.colorScheme.background
    val cardBackground = MaterialTheme.colorScheme.surfaceContainer
    val backButtonBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    // Ermittle den aktuellen Zustand für die Standard-Kategorien
    val currentRelEthCourse = courses.firstOrNull {
        it.subject.contains("Ethik", ignoreCase = true) || it.subject.contains("Religion", ignoreCase = true)
    }
    val currentWpuCourse = courses.firstOrNull {
        it.subject.contains("WPU", ignoreCase = true)
    }
    val currentLanguageCourse = courses.firstOrNull {
        it.subject.equals("Französisch", ignoreCase = true) || it.subject.equals("Latein", ignoreCase = true)
    }
    val otherCourses = courses.filter {
        it != currentRelEthCourse && it != currentWpuCourse && it != currentLanguageCourse
    }

    PredictiveBackContainer(
        enabled = showAddDialog || editingCourse != null,
        onBack = {
            showAddDialog = false
            editingCourse = null
        }
    ) {}

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backButtonBg)
                        .handCursor()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "Zurück",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Meine Kurse",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = getQuicksandFontFamily(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- INFO CARD ---
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_check),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Trage hier deine Kurse ein (z. B. Ethik oder Religion, WPU, 2. Fremdsprache). Vertretungen für fremde Kurse werden im Vertretungsplan automatisch ausgeblendet.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BEREICH 1: Religion & Ethik ---
            SectionHeader(title = "Religion & Ethik", color = primaryColor)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welchen Kurs besuchst du?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedSubject = currentRelEthCourse?.subject ?: "Ethik"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Ethik", "Ev. Religion", "Kath. Religion").forEach { subj ->
                            val isSelected = selectedSubject.equals(subj, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val teacher = currentRelEthCourse?.teacher ?: ""
                                    CourseRepository.addCourse(
                                        UserCourse(
                                            id = "course_rel_eth",
                                            subject = subj,
                                            teacher = teacher,
                                            name = "$subj${if (teacher.isNotBlank()) " ($teacher)" else ""}"
                                        )
                                    )
                                },
                                label = { Text(subj, fontSize = 12.sp) },
                                modifier = Modifier.handCursor()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var teacherText by remember(currentRelEthCourse) {
                        mutableStateOf(currentRelEthCourse?.teacher ?: "")
                    }

                    OutlinedTextField(
                        value = teacherText,
                        onValueChange = { newTeacher ->
                            teacherText = newTeacher
                            CourseRepository.addCourse(
                                UserCourse(
                                    id = "course_rel_eth",
                                    subject = selectedSubject,
                                    teacher = newTeacher.trim(),
                                    name = "$selectedSubject${if (newTeacher.isNotBlank()) " ($newTeacher)" else ""}"
                                )
                            )
                        },
                        label = { Text("Lehrkraft-Kürzel (z. B. Web, Car, Sr)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BEREICH 2: Wahlpflichtunterricht (WPU) ---
            SectionHeader(title = "Wahlpflichtunterricht (WPU)", color = primaryColor)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var wpuNameText by remember(currentWpuCourse) {
                        mutableStateOf(currentWpuCourse?.name?.takeIf { it != "WPU" } ?: "")
                    }
                    var wpuTeacherText by remember(currentWpuCourse) {
                        mutableStateOf(currentWpuCourse?.teacher ?: "")
                    }

                    OutlinedTextField(
                        value = wpuNameText,
                        onValueChange = { newName ->
                            wpuNameText = newName
                            CourseRepository.addCourse(
                                UserCourse(
                                    id = "course_wpu",
                                    subject = "WPU",
                                    teacher = wpuTeacherText.trim(),
                                    name = if (newName.isNotBlank()) newName.trim() else "WPU"
                                )
                            )
                        },
                        label = { Text("Kursname (z. B. WPU Informatik, WPU Kunst)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = wpuTeacherText,
                        onValueChange = { newTeacher ->
                            wpuTeacherText = newTeacher
                            CourseRepository.addCourse(
                                UserCourse(
                                    id = "course_wpu",
                                    subject = "WPU",
                                    teacher = newTeacher.trim(),
                                    name = if (wpuNameText.isNotBlank()) wpuNameText.trim() else "WPU"
                                )
                            )
                        },
                        label = { Text("Lehrkraft-Kürzel (z. B. Mül, Tho)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BEREICH 3: 2. Fremdsprache ---
            SectionHeader(title = "2. Fremdsprache", color = primaryColor)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val selectedLang = currentLanguageCourse?.subject ?: "Französisch"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Französisch", "Latein", "Spanisch", "Keine").forEach { lang ->
                            val isSelected = if (lang == "Keine") currentLanguageCourse == null else selectedLang.equals(lang, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (lang == "Keine") {
                                        CourseRepository.removeCourse("course_fs2")
                                    } else {
                                        val teacher = currentLanguageCourse?.teacher ?: ""
                                        CourseRepository.addCourse(
                                            UserCourse(
                                                id = "course_fs2",
                                                subject = lang,
                                                teacher = teacher,
                                                name = lang
                                            )
                                        )
                                    }
                                },
                                label = { Text(lang, fontSize = 12.sp) },
                                modifier = Modifier.handCursor()
                            )
                        }
                    }

                    if (currentLanguageCourse != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        var langTeacherText by remember(currentLanguageCourse) {
                            mutableStateOf(currentLanguageCourse.teacher)
                        }

                        OutlinedTextField(
                            value = langTeacherText,
                            onValueChange = { newTeacher ->
                                langTeacherText = newTeacher
                                CourseRepository.addCourse(
                                    UserCourse(
                                        id = "course_fs2",
                                        subject = selectedLang,
                                        teacher = newTeacher.trim(),
                                        name = selectedLang
                                    )
                                )
                            },
                            label = { Text("Lehrkraft-Kürzel (z. B. Hol, Bü)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BEREICH 4: Weitere Kurse & Fächer ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(title = "Weitere Kurse", color = primaryColor)

                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.handCursor()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Hinzufügen",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Hinzufügen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (otherCourses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Keine weiteren Kurse angelegt.",
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        otherCourses.forEachIndexed { index, course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.displayLabel(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                    if (course.room.isNotBlank()) {
                                        Text(
                                            text = "Raum: ${course.room}",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { editingCourse = course },
                                        modifier = Modifier.size(36.dp).handCursor()
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_star_filled),
                                            contentDescription = "Bearbeiten",
                                            tint = textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { CourseRepository.removeCourse(course.id) },
                                        modifier = Modifier.size(36.dp).handCursor()
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_close),
                                            contentDescription = "Löschen",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (index < otherCourses.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog: Neuen Kurs hinzufügen oder bearbeiten
    val courseToEdit = editingCourse
    if (showAddDialog || courseToEdit != null) {
        CourseEditDialog(
            initialCourse = courseToEdit,
            onDismiss = {
                showAddDialog = false
                editingCourse = null
            },
            onSave = { savedCourse ->
                if (courseToEdit != null) {
                    CourseRepository.updateCourse(savedCourse)
                } else {
                    CourseRepository.addCourse(savedCourse)
                }
                showAddDialog = false
                editingCourse = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        ),
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun CourseEditDialog(
    initialCourse: UserCourse?,
    onDismiss: () -> Unit,
    onSave: (UserCourse) -> Unit
) {
    var subject by remember { mutableStateOf(initialCourse?.subject ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var room by remember { mutableStateOf(initialCourse?.room ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialCourse != null) "Kurs bearbeiten" else "Neuen Kurs anlegen")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Fach (z. B. Sport, Kunst, Musik)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Lehrkraft-Kürzel (z. B. Tho, Af)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kursname / Gruppe (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Raum (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        val id = initialCourse?.id ?: "course_custom_${de.kjltowinkler.msapp.data.task.getCurrentEpochMillis()}"
                        onSave(
                            UserCourse(
                                id = id,
                                subject = subject.trim(),
                                teacher = teacher.trim(),
                                name = if (name.isNotBlank()) name.trim() else subject.trim(),
                                room = room.trim(),
                                isCustom = true
                            )
                        )
                    }
                },
                enabled = subject.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
