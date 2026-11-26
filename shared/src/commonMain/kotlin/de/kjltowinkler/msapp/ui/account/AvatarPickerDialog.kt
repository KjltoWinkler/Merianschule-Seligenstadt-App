package de.kjltowinkler.msapp.ui.account

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.kjltowinkler.msapp.data.auth.UpdateProfileRequest
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.util.rememberPlatformImagePicker
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * Dialog zur Auswahl und Anpassung des Profilbilds.
 * Unterstützt ein eigenes sicheres Profilbild (mit Google Fotos Zuschneide-Screen),
 * Vorlagen (Maskottchen, Schullogo) oder Initialen mit Farbwahl.
 */
@Composable
fun AvatarPickerDialog(
    accountName: String,
    currentAvatarType: String,
    currentAvatarColorHex: Long,
    currentAvatarUrl: String? = null,
    onDismissRequest: () -> Unit,
    onAvatarSelected: (avatarType: String, avatarColorHex: Long, avatarUrl: String?) -> Unit,
    useMaterialYou: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(currentAvatarType) }
    var selectedColorHex by remember { mutableStateOf(currentAvatarColorHex) }
    var selectedAvatarUrl by remember { mutableStateOf(currentAvatarUrl) }

    var selectedLocalBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedLocalBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var selectedMimeType by remember { mutableStateOf("image/jpeg") }

    var showCropScreen by remember { mutableStateOf(false) }
    var pendingCropBytes by remember { mutableStateOf<ByteArray?>(null) }

    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickImage = rememberPlatformImagePicker { bytes, mimeType ->
        try {
            pendingCropBytes = bytes
            selectedMimeType = mimeType
            showCropScreen = true
            errorMessage = null
        } catch (t: Throwable) {
            errorMessage = "Bild konnte nicht geöffnet werden: ${t.message}"
        }
    }

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
        "app_icon" to "App-Logo"
    )

    val colorOptions = listOf(
        0xFF395A3CL to "Grün",
        0xFF1B65A4L to "Blau",
        0xFF007A78L to "Teal",
        0xFF6B4FA2L to "Lila",
        0xFFBF584DL to "Koralle",
        0xFF4A5568L to "Schiefer"
    )

    // --- GOOGLE FOTOS STYLE CROP SCREEN ---
    if (showCropScreen && pendingCropBytes != null) {
        AvatarCropScreen(
            imageBytes = pendingCropBytes!!,
            onDismissRequest = {
                showCropScreen = false
                pendingCropBytes = null
            },
            onCropConfirmed = { croppedBytes ->
                try {
                    val bmp = croppedBytes.decodeToImageBitmap()
                    selectedLocalBytes = croppedBytes
                    selectedLocalBitmap = bmp
                    selectedMimeType = "image/jpeg"
                    selectedType = "custom"
                    showCropScreen = false
                    pendingCropBytes = null
                    errorMessage = null
                } catch (t: Throwable) {
                    errorMessage = "Fehler beim Vorbereiten des Bildes: ${t.message}"
                    showCropScreen = false
                }
            },
            useMaterialYou = useMaterialYou
        )
    }

    Dialog(onDismissRequest = {
        if (!isUploading && !showCropScreen) onDismissRequest()
    }) {
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
                        fontWeight = FontWeight.Normal,
                        color = onSurfaceColor
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Große Live-Vorschau
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(3.dp, primaryColor, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedType == "custom" && selectedLocalBitmap != null) {
                        Image(
                            bitmap = selectedLocalBitmap!!,
                            contentDescription = "Profilbild Vorschau",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        AccountAvatar(
                            accountName = accountName,
                            avatarType = selectedType,
                            avatarColorHex = selectedColorHex,
                            avatarUrl = if (selectedType == "custom") selectedAvatarUrl else null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 1. Eigenes Bild hochladen ---
                Text(
                    text = "Eigenes Foto",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariantColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { pickImage() },
                        enabled = !isUploading,
                        modifier = Modifier
                            .weight(1f)
                            .handCursor(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_edit),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedType == "custom" && (selectedLocalBitmap != null || selectedAvatarUrl != null)) "Anderes Foto" else "Foto wählen",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedType == "custom" && selectedLocalBytes != null) {
                        IconButton(
                            onClick = {
                                pendingCropBytes = selectedLocalBytes
                                showCropScreen = true
                            },
                            enabled = !isUploading,
                            modifier = Modifier.handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_crop),
                                contentDescription = "Zuschneiden",
                                tint = primaryColor
                            )
                        }
                    }

                    if (selectedType == "custom" || !selectedAvatarUrl.isNullOrBlank() || selectedLocalBitmap != null) {
                        OutlinedButton(
                            onClick = {
                                selectedType = "initials"
                                selectedLocalBytes = null
                                selectedLocalBitmap = null
                                selectedAvatarUrl = null
                            },
                            enabled = !isUploading,
                            modifier = Modifier.handCursor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete),
                                contentDescription = "Foto entfernen",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Vorlagen ---
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
                                .clickable(enabled = !isUploading) {
                                    selectedType = type
                                    selectedLocalBytes = null
                                    selectedLocalBitmap = null
                                }
                                .handCursor(),
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

                // --- 3. Initialen mit Farben ---
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
                                .clickable(enabled = !isUploading) {
                                    selectedType = "initials"
                                    selectedColorHex = colorHex
                                    selectedLocalBytes = null
                                    selectedLocalBitmap = null
                                }
                                .handCursor()
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
                    TextButton(
                        onClick = onDismissRequest,
                        enabled = !isUploading,
                        modifier = Modifier.handCursor()
                    ) {
                        Text(
                            text = "Abbrechen",
                            color = onSurfaceVariantColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isUploading) return@Button

                            if (selectedType == "custom" && selectedLocalBytes != null) {
                                if (BackendApiClient.isLoggedIn) {
                                    isUploading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = BackendApiClient.uploadAvatar(
                                            bytes = selectedLocalBytes!!,
                                            mimeType = selectedMimeType
                                        )
                                        isUploading = false
                                        result.onSuccess { updatedUser ->
                                            selectedLocalBitmap?.let { bmp ->
                                                updatedUser.avatarUrl?.let { url ->
                                                    AvatarImageLoader.setCache(url, bmp)
                                                }
                                            }
                                            onAvatarSelected("custom", selectedColorHex, updatedUser.avatarUrl)
                                            onDismissRequest()
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Upload fehlgeschlagen"
                                        }
                                    }
                                } else {
                                    selectedLocalBitmap?.let { bmp ->
                                        val mockUrl = "mock://avatar/local"
                                        AvatarImageLoader.setCache(mockUrl, bmp)
                                        onAvatarSelected("custom", selectedColorHex, mockUrl)
                                    } ?: run {
                                        onAvatarSelected("custom", selectedColorHex, null)
                                    }
                                    onDismissRequest()
                                }
                            } else if (currentAvatarType == "custom" && selectedType != "custom") {
                                if (BackendApiClient.isLoggedIn) {
                                    isUploading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        BackendApiClient.deleteAvatar()
                                        BackendApiClient.updateProfile(
                                            UpdateProfileRequest(
                                                avatarType = selectedType,
                                                avatarColorHex = selectedColorHex,
                                                avatarUrl = null
                                            )
                                        )
                                        isUploading = false
                                        onAvatarSelected(selectedType, selectedColorHex, null)
                                        onDismissRequest()
                                    }
                                } else {
                                    onAvatarSelected(selectedType, selectedColorHex, null)
                                    onDismissRequest()
                                }
                            } else {
                                onAvatarSelected(selectedType, selectedColorHex, selectedAvatarUrl)
                                onDismissRequest()
                            }
                        },
                        enabled = !isUploading,
                        modifier = Modifier.handCursor(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wird gespeichert...",
                                fontWeight = FontWeight.Bold,
                                color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E)
                            )
                        } else {
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
}
