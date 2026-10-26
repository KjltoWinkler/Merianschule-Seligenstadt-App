package de.kjltowinkler.msapp.ui.account

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.FigmaBackgroundDark
import de.kjltowinkler.msapp.theme.FigmaButtonSecondaryDark
import de.kjltowinkler.msapp.theme.FigmaLinkGreen
import de.kjltowinkler.msapp.theme.FigmaMenuBackgroundDark
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Account- / Profil-BottomSheet entsprechend dem Entwurf in `account#1.png`.
 * 
 * Beinhaltet:
 * 1. Skalierung, Positionierung & Material You-Theming identisch zu `SubstitutionDetailBottomSheet`.
 * 2. Profilbild-Auswahl (Vorlagen & Initialen mit Farbwahl).
 * 3. E-Mail-Interaktion (öffnet die E-Mail-App mit Entwurf an die hinterlegte Adresse).
 * 4. Dynamische Account-Daten (Klasse, Schulzweig, Klassenleitung).
 * 5. Menü-Aktionen & Abmelden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheet(
    onDismissRequest: () -> Unit,
    accountName: String = "Sebastian Wagner",
    accountEmail: String = "sebastian.wagner@mssof.onmicrosoft.com",
    className: String = "9aR",
    schoolType: String = "Realschule",
    classTeacher: String = "Hr. Thomas",
    avatarType: String = "initials",
    avatarColorHex: Long = 0xFF395A3CL,
    onAvatarChanged: (avatarType: String, avatarColorHex: Long) -> Unit = { _, _ -> },
    onFeedbackClicked: () -> Unit = {},
    onHelpFaqClicked: () -> Unit = {},
    onAboutAppClicked: () -> Unit = {},
    onPrivacyImpressumClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var showAvatarPicker by remember { mutableStateOf(false) }

    // Dynamische Material You-Farben mit Figma-Fallback wie in SubstitutionDetailBottomSheet & TaskEditBottomSheet
    val sheetContainerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        FigmaBackgroundDark // Color(0xFF141A16)
    }

    val brickBackgroundColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        FigmaMenuBackgroundDark // Color(0xFF202820)
    }

    val dragHandleColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        Color(0xFF5E655C)
    }

    val closeButtonBg = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        FigmaButtonSecondaryDark // Color(0xFF222822)
    }

    val closeIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFF9BB89A)
    }

    val textPrimaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFDFE9DE)
    }

    val textSecondaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5ADA2)
    }

    val metaIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFA5ADA2)
    }

    val actionIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFDFE9DE)
    }

    val linkColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        FigmaLinkGreen // Color(0xFFD7FED5)
    }

    val destructiveColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFFDF6A5E)
    }

    val dividerColor = if (useMaterialYou) {
        sheetContainerColor.copy(alpha = 0.8f)
    } else {
        Color(0xFF141A16)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = textPrimaryColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            // Drag-Griff mit konsistenter Positionierung
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
            // --- 1. PROFIL-HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar mit Bearbeiten-Badge
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .clickable { showAvatarPicker = true }
                ) {
                    AccountAvatar(
                        accountName = accountName,
                        avatarType = avatarType,
                        avatarColorHex = avatarColorHex,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Stift-Badge unten rechts
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(sheetContainerColor)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(brickBackgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_edit),
                            contentDescription = "Profilbild bearbeiten",
                            tint = textPrimaryColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name & interaktive E-Mail
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = accountEmail,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                try {
                                    val subject = "Anfrage an $accountName"
                                    val safeSubject = subject.replace(" ", "%20")
                                    uriHandler.openUri("mailto:$accountEmail?subject=$safeSubject")
                                } catch (_: Exception) {
                                    // Fallback falls kein lokaler Mailclient registriert ist
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Schließen-Button (X)
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- 2. SCHUL- UND KLASSENINFORMATIONEN ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Klasse
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_groups),
                        contentDescription = null,
                        tint = metaIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Klasse: $className",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondaryColor
                        )
                    )
                }

                // Schulzweig
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_school),
                        contentDescription = null,
                        tint = metaIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Schulzweig: $schoolType",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondaryColor
                        )
                    )
                }

                // Klassenleitung
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_co_present),
                        contentDescription = null,
                        tint = metaIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Klassenleitung: $classTeacher",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondaryColor
                        )
                    )
                }
            }

            val topBrickShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
            val middleBrickShape = RoundedCornerShape(3.dp)
            val bottomBrickShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

            // --- 3. MENÜ-GRUPPE 1 (Feedback, FAQ, Über die App, Datenschutz) als Bricks ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AccountBrickItem(
                    icon = Res.drawable.ic_feedback,
                    title = "Feedback senden",
                    shape = topBrickShape,
                    backgroundColor = brickBackgroundColor,
                    onClick = onFeedbackClicked,
                    iconTint = actionIconTint,
                    textColor = textPrimaryColor
                )
                AccountBrickItem(
                    icon = Res.drawable.ic_help,
                    title = "Hilfe & FAQ",
                    shape = middleBrickShape,
                    backgroundColor = brickBackgroundColor,
                    onClick = onHelpFaqClicked,
                    iconTint = actionIconTint,
                    textColor = textPrimaryColor
                )
                AccountBrickItem(
                    icon = Res.drawable.ic_info,
                    title = "Über die App",
                    shape = middleBrickShape,
                    backgroundColor = brickBackgroundColor,
                    onClick = onAboutAppClicked,
                    iconTint = actionIconTint,
                    textColor = textPrimaryColor
                )
                AccountBrickItem(
                    icon = Res.drawable.ic_policy,
                    title = "Datenschutz & Impressum",
                    shape = bottomBrickShape,
                    backgroundColor = brickBackgroundColor,
                    onClick = onPrivacyImpressumClicked,
                    iconTint = actionIconTint,
                    textColor = textPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4. MENÜ-GRUPPE 2 (Einstellungen & Abmelden) als Bricks ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AccountBrickItem(
                    icon = Res.drawable.ic_settings,
                    title = "Einstellungen",
                    shape = topBrickShape,
                    backgroundColor = brickBackgroundColor,
                    onClick = onSettingsClicked,
                    iconTint = actionIconTint,
                    textColor = textPrimaryColor
                )
                AccountBrickItem(
                    icon = Res.drawable.ic_logout,
                    title = "Abmelden",
                    shape = bottomBrickShape,
                    backgroundColor = brickBackgroundColor,
                    onClick = onLogoutClicked,
                    iconTint = destructiveColor,
                    textColor = destructiveColor
                )
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            accountName = accountName,
            currentAvatarType = avatarType,
            currentAvatarColorHex = avatarColorHex,
            onDismissRequest = { showAvatarPicker = false },
            onAvatarSelected = { type, colorHex ->
                onAvatarChanged(type, colorHex)
            },
            useMaterialYou = useMaterialYou
        )
    }
}

@Composable
private fun AccountBrickItem(
    icon: DrawableResource,
    title: String,
    shape: Shape,
    backgroundColor: Color,
    onClick: () -> Unit,
    iconTint: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 54.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
