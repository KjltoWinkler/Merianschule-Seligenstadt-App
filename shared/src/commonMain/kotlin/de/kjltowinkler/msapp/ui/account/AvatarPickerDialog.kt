package de.kjltowinkler.msapp.ui.account

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
import androidx.compose.ui.window.Dialog
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Dialog zur Auswahl und Anpassung des Profilbilds.
 * Ermöglicht die Wahl zwischen Vorlagen (Foto, Maskottchen, Schullogo)
 * oder Initialen mit individueller Farbwahl.
 */
@Composable
fun AvatarPickerDialog(
    accountName: String,
    currentAvatarType: String,
    currentAvatarColorHex: Long,
    onDismissRequest: () -> Unit,
    onAvatarSelected: (avatarType: String, avatarColorHex: Long) -> Unit,
    useMaterialYou: Boolean = true
) {
    var selectedType by remember { mutableStateOf(if (currentAvatarType == "photo") "initials" else currentAvatarType) }
    var selectedColorHex by remember { mutableStateOf(currentAvatarColorHex) }

    val dialogBg = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF202820)
    }

    val primaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF8CD88C)
    }

    val onSurfaceColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFDFE9DE)
    }

    val onSurfaceVariantColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5ADA2)
    }

    val presetAvatars = listOf(
        "mascot_clean" to "Maskottchen 1",
        "mascot_char" to "Maskottchen 2",
        "app_icon" to "Schullogo"
    )

    val colorOptions = listOf(
        0xFF395A3CL to "Grün",
        0xFF1B65A4L to "Blau",
        0xFF007A78L to "Teal",
        0xFF6B4FA2L to "Lila",
        0xFFBF584DL to "Koralle",
        0xFF4A5568L to "Schiefer"
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profilbild auswählen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Große Live-Vorschau
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(3.dp, primaryColor, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AccountAvatar(
                        accountName = accountName,
                        avatarType = selectedType,
                        avatarColorHex = selectedColorHex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 1. Vorlagen ---
                Text(
                    text = "Vorlagen",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariantColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    presetAvatars.forEach { (type, _) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, primaryColor, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            AccountAvatar(
                                accountName = accountName,
                                avatarType = type,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Initialen mit Farben ---
                Text(
                    text = "Buchstabe & Farben",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariantColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { (colorHex, _) ->
                        val isInitialsSelected = selectedType == "initials" && selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable {
                                    selectedType = "initials"
                                    selectedColorHex = colorHex
                                }
                                .then(
                                    if (isInitialsSelected) Modifier.border(2.5.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isInitialsSelected) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_check),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Aktionen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "Abbrechen",
                            color = onSurfaceVariantColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAvatarSelected(selectedType, selectedColorHex)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Speichern",
                            fontWeight = FontWeight.Bold,
                            color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E)
                        )
                    }
                }
            }
        }
    }
}
