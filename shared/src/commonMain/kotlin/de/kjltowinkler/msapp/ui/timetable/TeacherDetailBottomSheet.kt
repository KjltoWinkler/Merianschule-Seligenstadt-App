package de.kjltowinkler.msapp.ui.timetable

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.timetable.TeacherInfo
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Bottom Sheet mit Details zur ausgewählten Lehrkraft.
 * Gestaltet im einheitlichen Brick-Design der App (analog zu SubstitutionDetailBottomSheet
 * und AccountBottomSheet) mit voller Material-You-Unterstützung und Figma-Fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDetailBottomSheet(
    teacher: TeacherInfo,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val clipboardManager = LocalClipboardManager.current
    var emailCopied by remember { mutableStateOf(false) }

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

    val actionIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFE4EDE0)
    }

    val avatarContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color(0xFF2F402F)
    }

    val avatarTextColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        Color(0xFFDFE9DE)
    }

    val topBrickShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
    val middleBrickShape = RoundedCornerShape(3.dp)
    val bottomBrickShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

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
            // --- HEADER: Avatar, Name & Schließen-Button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(avatarContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacher.code.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = avatarTextColor
                            )
                        )
                    }

                    Column {
                        Text(
                            text = teacher.fullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kürzel: ${teacher.code}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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

            // --- INFO-BLOCK GRUPPE (Fächer, E-Mail, Sprechzeiten als Bricks) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Unterrichtsfächer (oberer Brick)
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
                        painter = painterResource(Res.drawable.ic_school),
                        contentDescription = null,
                        tint = actionIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = teacher.subjects.joinToString(", "),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                }

                // 2. E-Mail (Schulportal) (mittlerer Brick) mit Kopierfunktion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(middleBrickShape)
                        .background(brickBackgroundColor)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(teacher.email))
                            emailCopied = true
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
                            painter = painterResource(Res.drawable.ic_notes),
                            contentDescription = null,
                            tint = actionIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = teacher.email,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(teacher.email))
                            emailCopied = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (emailCopied) Res.drawable.ic_check else Res.drawable.ic_content_copy
                            ),
                            contentDescription = "E-Mail kopieren",
                            tint = if (emailCopied) actionIconTint else textSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 3. Sprechzeiten (unterer Brick)
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
                        text = teacher.officeHours,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )
                }
            }
        }
    }
}
