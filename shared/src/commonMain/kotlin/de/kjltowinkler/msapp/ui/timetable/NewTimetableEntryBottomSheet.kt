package de.kjltowinkler.msapp.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.theme.FigmaPrimaryDark
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Bottom Sheet zum Hinzufügen einer neuen Unterrichtsstunde oder eines Termins.
 * Gestaltet im einheitlichen App-Design mit voller Material-You-Unterstützung und Figma-Fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTimetableEntryBottomSheet(
    initialDay: TimetableDay,
    onDismissRequest: () -> Unit,
    onEntryAdded: (TimetableDay, TimetableLesson) -> Unit,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedDay by remember { mutableStateOf(initialDay) }
    var periodNumber by remember { mutableIntStateOf(1) }
    var subject by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("Raum B208") }
    var teacherCode by remember { mutableStateOf("") }
    var breakSelection by remember { mutableStateOf("5 Min. Wechselpause") }

    val defaultTimes = listOf(
        "07:40 - 08:25",
        "08:30 - 09:15",
        "09:35 - 10:20",
        "10:25 - 11:10",
        "11:30 - 12:15",
        "12:20 - 13:05",
        "13:50 - 14:35",
        "14:35 - 15:20"
    )

    var timeRange by remember(periodNumber) {
        val idx = (periodNumber - 1).coerceIn(0, defaultTimes.lastIndex)
        mutableStateOf(defaultTimes[idx])
    }

    val sheetContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color(0xFF141914)
    }

    val brickBackgroundColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF1E241E)
    }

    val dragHandleColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        Color(0xFF5E655C)
    }

    val closeButtonBg = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color(0xFF222822)
    }

    val closeIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFB0B8AE)
    }

    val textPrimaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFE4EDE0)
    }

    val textSecondaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5AEA3)
    }

    val buttonBg = if (useMaterialYou) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        FigmaPrimaryDark
    }

    val buttonContentColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Color.White
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = textPrimaryColor,
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
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stunde hinzufügen",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(closeButtonBg)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Schließen",
                        modifier = Modifier.size(24.dp),
                        tint = closeIconTint
                    )
                }
            }

            // --- WOCHENTAG AUSWÄHLEN ---
            Text(
                text = "Wochentag",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondaryColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimetableDay.entries.forEach { day ->
                    val isSelected = selectedDay == day
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else brickBackgroundColor
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedDay = day }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.shortName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else textPrimaryColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- FACH ---
            Text(
                text = "Fach",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondaryColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("z. B. Informatik, Biologie", color = textSecondaryColor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = brickBackgroundColor,
                    unfocusedContainerColor = brickBackgroundColor,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textPrimaryColor,
                    unfocusedTextColor = textPrimaryColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- STUNDE & UHRZEIT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stunde",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = periodNumber.toString(),
                        onValueChange = {
                            val num = it.toIntOrNull()
                            if (num != null && num in 1..10) {
                                periodNumber = num
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = brickBackgroundColor,
                            unfocusedContainerColor = brickBackgroundColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor
                        )
                    )
                }

                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        text = "Uhrzeit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = timeRange,
                        onValueChange = { timeRange = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = brickBackgroundColor,
                            unfocusedContainerColor = brickBackgroundColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- RAUM & LEHRER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Raum",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = brickBackgroundColor,
                            unfocusedContainerColor = brickBackgroundColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lehrkraft Kürzel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = teacherCode,
                        onValueChange = { teacherCode = it },
                        placeholder = { Text("z. B. Af, Ky", color = textSecondaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = brickBackgroundColor,
                            unfocusedContainerColor = brickBackgroundColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- SPEICHERN-BUTTON ---
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        val breakMinutes = when {
                            breakSelection.contains("20") -> 20
                            breakSelection.contains("5") -> 5
                            else -> null
                        }
                        val teacherInfo = TimetableRepository.getTeacherInfo(teacherCode.trim())
                        val randomSuffix = kotlin.random.Random.nextInt(1000, 9999)
                        val newLesson = TimetableLesson(
                            id = "custom_${selectedDay.id}_${periodNumber}_$randomSuffix",
                            periodNumber = periodNumber,
                            timeRange = timeRange.trim(),
                            subject = subject.trim(),
                            room = room.trim(),
                            teacherCode = if (teacherCode.isNotBlank()) teacherCode.trim() else "N.N.",
                            teacherName = teacherInfo.fullName,
                            teacherEmail = teacherInfo.email,
                            breakAfterMinutes = breakMinutes,
                            breakLabel = if (breakMinutes != null) "$breakMinutes Min. Wechselpause" else null
                        )
                        TimetableRepository.addLesson(selectedDay, newLesson)
                        onEntryAdded(selectedDay, newLesson)
                        onDismissRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = subject.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonContentColor,
                    disabledContainerColor = buttonBg.copy(alpha = 0.5f),
                    disabledContentColor = buttonContentColor.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Speichern",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
