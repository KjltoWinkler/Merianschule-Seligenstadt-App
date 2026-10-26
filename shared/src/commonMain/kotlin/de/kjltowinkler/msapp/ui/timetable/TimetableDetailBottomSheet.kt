package de.kjltowinkler.msapp.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Informations-BottomSheet für Details zu einer ausgewählten Unterrichtsstunde.
 * Gestaltet im einheitlichen Brick-Design der App (analog zu SubstitutionDetailBottomSheet
 * und TaskEditBottomSheet) mit voller Material-You-Unterstützung und Figma-Fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableDetailBottomSheet(
    lesson: TimetableLesson,
    onDismissRequest: () -> Unit,
    onTeacherClick: (String) -> Unit,
    onAddTaskForSubject: (String) -> Unit,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
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

    val topBrickShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
    val middleBrickShape = RoundedCornerShape(3.dp)
    val bottomBrickShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    val singleBrickShape = RoundedCornerShape(20.dp)

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
            // --- HEADER: Fach & Schließen-Button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lesson.subject,
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
                        tint = closeIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // --- META-INFOS (Stunde & Zeit) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_nav_schedule),
                        contentDescription = null,
                        tint = metaIconTint,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "${lesson.periodNumber}. Stunde (${lesson.timeRange})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor
                    )
                }
            }

            // --- INFO-BLOCK GRUPPE (Raum, Lehrkraft, Pause als Bricks) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Raum (oberer Brick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(topBrickShape)
                        .background(brickBackgroundColor)
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_location_on),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (lesson.room.startsWith("Raum", ignoreCase = true)) lesson.room else "Raum ${lesson.room}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                }

                // 2. Lehrkraft (mittlerer bzw. unterer Brick)
                val teacherShape = if (lesson.breakLabel != null) middleBrickShape else bottomBrickShape
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(teacherShape)
                        .background(brickBackgroundColor)
                        .clickable {
                            onDismissRequest()
                            onTeacherClick(lesson.teacherCode)
                        }
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_person),
                            contentDescription = null,
                            tint = actionIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        val teacherText = if (lesson.teacherName.isNotBlank()) {
                            "${lesson.teacherName} (${lesson.teacherCode})"
                        } else {
                            lesson.teacherCode
                        }
                        Text(
                            text = teacherText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                    }

                    Icon(
                        painter = painterResource(Res.drawable.ic_chevron_right),
                        contentDescription = "Details",
                        tint = textSecondaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 3. Pause im Anschluss (unterer Brick, falls vorhanden)
                if (lesson.breakLabel != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bottomBrickShape)
                            .background(brickBackgroundColor)
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_access_time),
                            contentDescription = null,
                            tint = actionIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = lesson.breakLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- AKTIONEN-GRUPPE: Aufgabe für dieses Fach anlegen (als Brick) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(singleBrickShape)
                    .background(brickBackgroundColor)
                    .clickable {
                        onDismissRequest()
                        onAddTaskForSubject(lesson.subject)
                    }
                    .defaultMinSize(minHeight = 54.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_add_task),
                    contentDescription = null,
                    tint = actionIconTint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Aufgabe für ${lesson.subject} anlegen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimaryColor
                )
            }
        }
    }
}
