package de.kjltowinkler.msapp.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import de.kjltowinkler.msapp.ui.components.MerianQrCode
import de.kjltowinkler.msapp.ui.components.MerianQrCodeStyle
import de.kjltowinkler.msapp.ui.desktop.DesktopTwoFactorConnectScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Onboarding-Screen: Authenticator-App verknüpfen (2FA Connect)
 * Gemäß Figma-Design (onboarding#2FAconnect.png).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingTwoFactorConnectScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    // Standard TOTP-URI oder Secret, lässt sich direkt vom Backend übergeben
    totpUri: String = "otpauth://totp/Merianschule:$accountName?secret=ABCD%20EFGH%201234%205678&issuer=Merianschule",
    backupCode: String = "ABCD EFGH 1234 5678",
    onBackClicked: () -> Unit = {},
    onChangeAccountClicked: () -> Unit = onBackClicked,
    onHelpClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {}
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var codeCopied by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val googleAuthUrl = "https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&hl=de"
    val twoFasUrl = "https://play.google.com/store/apps/details?id=com.twofasapp&hl=de"

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (maxWidth >= 840.dp) {
            DesktopTwoFactorConnectScreen(
                accountName = accountName,
                avatarPainter = avatarPainter,
                totpUri = totpUri,
                backupCode = backupCode,
                onNextClicked = onNextClicked,
                onChangeAccountClicked = onChangeAccountClicked,
                onHelpClicked = onHelpClicked
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Oberer Inhaltsbereich
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Zurück-Button oben links (44x44 dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .size(44.dp)
                            .semantics { contentDescription = "Zurück" }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Zurück",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Headline "Authenticator-App verknüpfen"
                Text(
                    text = "Authenticator-App\nverknüpfen",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Account-Chip ("Sebastian Wagner" mit Avatar)
                MerianAccountBadge(
                    accountName = accountName,
                    avatarPainter = avatarPainter,
                    onClick = onChangeAccountClicked,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Beschreibungstext mit interaktivem "Hilfe"-Link
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Installiere die App „Google Authenticator“ aus dem Play Store oder App Store. Öffne sie und scanne diesen QR-Code. ",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Hilfe",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                showHelpDialog = true
                                onHelpClicked()
                            }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 5. Zentrierter Material 3 QR-Code Generator
                MerianQrCode(
                    content = totpUri,
                    modifier = Modifier
                        .size(250.dp),
                    style = MerianQrCodeStyle(
                        darkColor = Color.Black,
                        lightColor = Color.White,
                        cornerRadius = 16.dp,
                        quietZonePadding = 14.dp,
                        roundPixels = true
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Code-Text mit Kopier-Icon ("Code: ABCD EFGH 1234 5678")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(backupCode))
                            codeCopied = true
                            coroutineScope.launch {
                                delay(2500)
                                codeCopied = false
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (codeCopied) "Code kopiert: $backupCode" else "Code: $backupCode",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (codeCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(Res.drawable.ic_content_copy),
                        contentDescription = "Code in Zwischenablage kopieren",
                        tint = if (codeCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 7. Weiter-Button unten (69 dp hoch)
            Button(
                onClick = onNextClicked,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp)
                    .semantics { contentDescription = "Weiter" }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Weiter",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 28.dp)
                            .size(31.dp)
                    )
                }
            }
        }

        // Hilfe-Dialog: Authenticator-App mit klickbaren Store-Links
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Text(
                        text = "Authenticator-App einrichten",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "So funktioniert's:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "1. Lade dir eine kostenlose Authenticator-App herunter:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Klickbare App-Links
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "• Google Authenticator",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { uriHandler.openUri(googleAuthUrl) }
                            )

                            Text(
                                text = "• 2FAS Authenticator",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { uriHandler.openUri(twoFasUrl) }
                            )
                        }

                        Text(
                            text = "2. Scanne den oben angezeigten QR-Code mit der Kamera oder Sircle-To-Search auf deinem Android und klicke auf öffnen.\n\n" +
                                    "3. Falls das Scannen nicht klappt, kannst du den Textcode kopieren und manuell als Schlüssel eingeben.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text(
                            text = "Verstanden",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}
}
}

@Preview
@Composable
fun OnboardingTwoFactorConnectScreenPreview() {
    AppTheme(darkTheme = true) {
        OnboardingTwoFactorConnectScreen(
            accountName = "Sebastian Wagner"
        )
    }
}
