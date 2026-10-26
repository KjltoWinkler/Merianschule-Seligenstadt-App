package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.*
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import de.kjltowinkler.msapp.ui.components.MerianQrCode
import de.kjltowinkler.msapp.ui.components.MerianQrCodeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun DesktopTwoFactorConnectScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    totpUri: String = "otpauth://totp/Merianschule:$accountName?secret=ABCD%20EFGH%201234%205678&issuer=Merianschule",
    backupCode: String = "ABCD EFGH 1234 5678",
    onNextClicked: () -> Unit = {},
    onChangeAccountClicked: () -> Unit = {},
    onHelpClicked: () -> Unit = {}
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var codeCopied by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    DesktopScreenScaffold(
        artworkVariant = DesktopArtworkVariant.TWO_FACTOR_CONNECT,
        topSpacing = 85.dp
    ) {
        // 1. Titel
        Text(
            text = "Authenticator-App\nverknüpfen",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold
            ),
            color = DesktopTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Account Chip
        MerianAccountBadge(
            accountName = accountName,
            avatarPainter = avatarPainter,
            onClick = onChangeAccountClicked
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Beschreibung & Link "Hilfe"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installiere die App „Google Authenticator“ aus dem Play Store oder App Store. Öffne sie und scanne diesen QR-Code. ",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = DesktopTextSecondary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hilfe",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textDecoration = TextDecoration.Underline
            ),
            color = DesktopLink,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    showHelpDialog = true
                    onHelpClicked()
                }
        )

        Spacer(modifier = Modifier.weight(1f))

        // 4. QR-Code
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MerianQrCode(
                content = totpUri,
                modifier = Modifier.size(240.dp),
                style = MerianQrCodeStyle(
                    darkColor = Color.Black,
                    lightColor = Color.White,
                    cornerRadius = 16.dp,
                    quietZonePadding = 14.dp,
                    roundPixels = true
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Code-Text mit Kopier-Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    clipboardManager.setText(AnnotatedString(backupCode))
                    codeCopied = true
                    coroutineScope.launch {
                        delay(2500)
                        codeCopied = false
                    }
                }
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = if (codeCopied) "Code kopiert!" else "Code: $backupCode",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                color = if (codeCopied) DesktopAccentBlue else DesktopTextSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(Res.drawable.ic_content_copy),
                contentDescription = "Code kopieren",
                tint = if (codeCopied) DesktopAccentBlue else DesktopTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 6. Weiter Button
        Button(
            onClick = onNextClicked,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = DesktopAccentBlue,
                contentColor = DesktopOnAccentBlue
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .semantics { contentDescription = "Weiter" }
        ) {
            Text(
                text = "Weiter",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = DesktopOnAccentBlue
            )
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = DesktopCardBackground,
            titleContentColor = DesktopTextPrimary,
            textContentColor = DesktopTextSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Hilfe zu Authenticator-Apps", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Du kannst jede TOTP-kompatible Authenticator-App wie Google Authenticator, Microsoft Authenticator oder 2FAS verwenden. Scanne den QR-Code oder gib den Text-Code manuell ein.",
                    color = DesktopTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Verstanden", color = DesktopAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Preview
@Composable
private fun DesktopTwoFactorConnectScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopTwoFactorConnectScreen()
        }
    }
}
