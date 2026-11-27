package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
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
 * Konten-Screen für Wear OS (Pixel Watch) im Stil der Google-Kontenauswahl:
 * - Kopfzeile "Konten"
 * - Sektion "Aktiv" mit hervorgehobener Konto-Pille (Profilbild, Name, E-Mail)
 * - Sektion "Wechseln zu" mit "Neues Konto"-Pille zur Smartphone-Kopplung
 */
@Composable
fun WearAccountsScreen(
    onActiveAccountClick: () -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val currentUser = BackendApiClient.currentUser
    val isLoggedIn = BackendApiClient.isLoggedIn && currentUser != null

    val userName = currentUser?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.username?.takeIf { it.isNotBlank() }
        ?: "Sebastian Wagner"
    val userEmail = currentUser?.email?.takeIf { it.isNotBlank() }
        ?: "sw11082911@gmail.com"
    val avatarUrl = currentUser?.avatarUrl
    val avatarColorHex = currentUser?.avatarColorHex ?: 0xFF395A3CL

    WearAccountsScreenContent(
        isLoggedIn = isLoggedIn,
        userName = userName,
        userEmail = userEmail,
        avatarUrl = avatarUrl,
        avatarColorHex = avatarColorHex,
        onActiveAccountClick = onActiveAccountClick,
        onAddAccountClick = onAddAccountClick,
        onLogoutClick = onLogoutClick
    )
}

@Composable
fun WearAccountsScreenContent(
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    avatarUrl: String?,
    avatarColorHex: Long,
    onActiveAccountClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var showLogoutDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        scrollState = scrollState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Kopfzeile: "Konten"
            item(key = "header") {
                ListHeader(
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec)
                ) {
                    Text(
                        text = "Konten",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isLoggedIn) {
                // Sektion: "Aktiv"
                item(key = "sub_active") {
                    Text(
                        text = "Aktiv",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .transformedHeight(this, transformationSpec)
                    )
                }

                // Aktives Konto Pille
                item(key = "active_account") {
                    FilledTonalButton(
                        onClick = onActiveAccountClick,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = rememberAvatarBitmap(avatarUrl)
                            val initials = remember(userName) { getInitialsForName(userName) }
                            val effectiveColor = if (avatarColorHex != 0L) {
                                avatarColorHex
                            } else {
                                getAvatarColorForName(userName)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(effectiveColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Profilbild von $userName",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
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
                                    text = userName,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = userEmail,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Sektion: "Wechseln zu"
            item(key = "sub_switch") {
                Text(
                    text = if (isLoggedIn) "Wechseln zu" else "Anmelden",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                        .transformedHeight(this, transformationSpec)
                )
            }

            // "Neues Konto"-Pille
            item(key = "new_account") {
                FilledTonalButton(
                    onClick = onAddAccountClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 58.dp)
                        .transformedHeight(this, transformationSpec)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_switch_account_device),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Neues Konto",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isLoggedIn) {
                item(key = "logout_button") {
                    FilledTonalButton(
                        onClick = { showLogoutDialog = true },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logout),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Abmelden",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    WearLogoutConfirmDialog(
        visible = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        onConfirm = {
            showLogoutDialog = false
            onLogoutClick()
        }
    )
}

@Composable
internal fun WearLogoutConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_logout),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Abmelden?",
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Dein Konto wird auf dieser Uhr abgemeldet.",
                fontFamily = GoogleSansRounded,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(onClick = onConfirm)
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
        }
    )
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun WearAccountsScreenPreview() {
    WearAppTheme(useDynamicColors = false) {
        WearAccountsScreenContent(
            isLoggedIn = true,
            userName = "Sebastian Wagner",
            userEmail = "sw11082911@gmail.com",
            avatarUrl = null,
            avatarColorHex = 0xFF395A3CL,
            onActiveAccountClick = {},
            onAddAccountClick = {}
        )
    }
}
