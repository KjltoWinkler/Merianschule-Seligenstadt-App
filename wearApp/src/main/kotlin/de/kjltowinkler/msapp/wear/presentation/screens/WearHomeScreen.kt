package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.ui.account.getAvatarColorForName
import de.kjltowinkler.msapp.ui.account.getInitialsForName
import de.kjltowinkler.msapp.ui.account.rememberAvatarBitmap
import de.kjltowinkler.msapp.wear.R
import de.kjltowinkler.msapp.wear.presentation.theme.GoogleSansRounded
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme

/**
 * Start-Screen der Wear OS App (Merianschule Seligenstadt / Notes+).
 *
 * Zeigt eine flüssig transformierende Liste für runde Pixel-Watch-Displays mit Optionen:
 * 1. Account (Profilbild/Initialen, Name, E-Mail)
 * 2. Stundenplan (ic_nav_schedule)
 * 3. Vertretungsplan (ic_nav_event_note)
 * 4. Aufgaben (ic_nav_checklist_rtl)
 * 5. Trennungsstrich
 * 6. Einstellungen (ic_settings)
 */
@Composable
fun WearHomeScreen(
    onAccountClick: () -> Unit = {},
    onTimetableClick: () -> Unit = {},
    onSubstitutionClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    // Aktuelle Nutzerdaten aus BackendApiClient oder Fallback
    val currentUser = BackendApiClient.currentUser
    val userName = currentUser?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.username?.takeIf { it.isNotBlank() }
        ?: "Sebastian Wagner"
    val userEmail = currentUser?.email?.takeIf { it.isNotBlank() }
        ?: "sw11082911@gmail.com"
    val avatarUrl = currentUser?.avatarUrl
    val avatarColorHex = currentUser?.avatarColorHex ?: 0xFF395A3CL

    ScreenScaffold(
        scrollState = scrollState
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val listContentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            // Zusätzlicher Abstand oben, damit der volle Account-Button unter TimeText nicht von der runden Display-Kante abgeschnitten wird
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding()
        )

        TransformingLazyColumn(
            state = scrollState,
            contentPadding = listContentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Kopfzeile: Startseite
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Text(
                        text = "Startseite",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1. Account Option
            item {
                WearAccountButton(
                    name = userName,
                    email = userEmail,
                    avatarUrl = avatarUrl,
                    avatarColorHex = avatarColorHex,
                    onClick = onAccountClick,
                    transformationScope = this,
                    transformationSpec = transformationSpec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Stundenplan Option
            item {
                WearMenuButton(
                    label = "Stundenplan",
                    iconRes = R.drawable.ic_nav_schedule,
                    onClick = onTimetableClick,
                    transformationScope = this,
                    transformationSpec = transformationSpec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Vertretungsplan Option
            item {
                WearMenuButton(
                    label = "Vertretungsplan",
                    iconRes = R.drawable.ic_nav_event_note,
                    onClick = onSubstitutionClick,
                    transformationScope = this,
                    transformationSpec = transformationSpec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Aufgaben Option
            item {
                WearMenuButton(
                    label = "Aufgaben",
                    iconRes = R.drawable.ic_nav_checklist_rtl,
                    onClick = onTasksClick,
                    transformationScope = this,
                    transformationSpec = transformationSpec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Trennungsstrich
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(1.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // 6. Einstellungen Option
            item {
                WearMenuButton(
                    label = "Einstellungen",
                    iconRes = R.drawable.ic_settings,
                    onClick = onSettingsClick,
                    transformationScope = this,
                    transformationSpec = transformationSpec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Puffer unten für perfekte Rundung der Pixel Watch
            item {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/**
 * Account-Button im Wear OS Material 3 Pill-Design mit rundem Avatar,
 * Haupttitel (Name) und Untertitel (E-Mail) in Google Sans Rounded.
 */
@Composable
fun WearAccountButton(
    name: String,
    email: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    avatarColorHex: Long = 0xFF395A3CL,
    transformationScope: TransformingLazyColumnItemScope? = null,
    transformationSpec: TransformationSpec? = null
) {
    val buttonModifier = if (transformationScope != null && transformationSpec != null) {
        modifier.transformedHeight(transformationScope, transformationSpec)
    } else {
        modifier
    }
    val transformation = if (transformationScope != null && transformationSpec != null) {
        with(transformationScope) {
            SurfaceTransformation(transformationSpec)
        }
    } else null

    FilledTonalButton(
        onClick = onClick,
        modifier = buttonModifier,
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = rememberAvatarBitmap(avatarUrl)
            val initials = remember(name) { getInitialsForName(name) }
            val effectiveColor = if (avatarColorHex != 0L) avatarColorHex else getAvatarColorForName(name)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(effectiveColor)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Profilbild von $name",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = email,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Menü-Button für Wear OS Material 3 mit linkem App-Icon und Label in Google Sans Rounded.
 */
@Composable
fun WearMenuButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    transformationScope: TransformingLazyColumnItemScope? = null,
    transformationSpec: TransformationSpec? = null
) {
    val buttonModifier = if (transformationScope != null && transformationSpec != null) {
        modifier.transformedHeight(transformationScope, transformationSpec)
    } else {
        modifier
    }
    val transformation = if (transformationScope != null && transformationSpec != null) {
        with(transformationScope) {
            SurfaceTransformation(transformationSpec)
        }
    } else null

    FilledTonalButton(
        onClick = onClick,
        modifier = buttonModifier,
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearHomeScreenPreview() {
    WearAppTheme {
        WearHomeScreen()
    }
}
