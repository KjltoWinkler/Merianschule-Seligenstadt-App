package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.*
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import merianschuleseligenstadtapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

@Composable
fun DesktopTwoFactorConfirmScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    initialCode: String = "",
    codeState: TextFieldState = rememberTextFieldState(initialCode),
    onFinishClicked: (String) -> Unit = {},
    onChangeAccountClicked: () -> Unit = {},
    onHelpClicked: () -> Unit = {}
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        snapshotFlow { codeState.text }.collect {
            if (isError) isError = false
        }
    }

    val customErrorColor = Color(0xFFFF7164)

    DesktopScreenScaffold(
        artworkVariant = DesktopArtworkVariant.TWO_FACTOR_CONFIRM
    ) {
        // 1. Titel
        Text(
            text = "Code-Bestätigung",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold
            ),
            color = DesktopTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Account Chip
        MerianAccountBadge(
            accountName = accountName,
            avatarPainter = avatarPainter,
            onClick = onChangeAccountClicked
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Beschreibung
        Text(
            text = "Gib den 6-stelligen Code ein, der dir in deiner Authenticator-App angezeigt wird.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = DesktopTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(36.dp))

        // 4. Eingabefeld 6-stelliger Code
        OutlinedTextField(
            state = codeState,
            label = { Text(if (isError) "6-stelligen Code eingeben*" else "6-stelligen Code eingeben") },
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = isError,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DesktopAccentBlue,
                unfocusedBorderColor = DesktopInputBorder,
                focusedLabelColor = DesktopAccentBlue,
                unfocusedLabelColor = DesktopInputPlaceholder,
                focusedTextColor = DesktopTextPrimary,
                unfocusedTextColor = DesktopTextPrimary,
                cursorColor = DesktopAccentBlue,
                focusedContainerColor = DesktopCardBackground,
                unfocusedContainerColor = DesktopCardBackground,
                errorBorderColor = customErrorColor,
                errorLabelColor = customErrorColor,
                errorSupportingTextColor = customErrorColor,
                errorCursorColor = customErrorColor
            ),
            supportingText = if (isError) {
                {
                    Text(
                        text = "Bitte gib den 6-stelligen Code ein",
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            } else null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = {
                val code = codeState.text.toString().trim()
                if (code.length >= 6) {
                    keyboardController?.hide()
                    onFinishClicked(code)
                } else {
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (isError) error("Bitte gib den 6-stelligen Code ein")
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Link "Hilfe?"
        Text(
            text = "Hilfe?",
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

        // 6. Weiter Button
        Button(
            onClick = {
                val code = codeState.text.toString().trim()
                if (code.length >= 6) {
                    onFinishClicked(code)
                } else {
                    isError = true
                }
            },
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
                Text("Code nicht erhalten?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Vergewissere dich, dass die Uhrzeit auf deinem Smartphone korrekt eingestellt ist (automatische Netzwerkzeit), da TOTP-Codes zeitabhängig alle 30 Sekunden erneuert werden.",
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
private fun DesktopTwoFactorConfirmScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopTwoFactorConfirmScreen()
        }
    }
}
