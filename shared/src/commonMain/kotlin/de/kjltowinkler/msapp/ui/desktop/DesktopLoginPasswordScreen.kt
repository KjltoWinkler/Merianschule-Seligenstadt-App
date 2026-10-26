package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
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
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun DesktopLoginPasswordScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    initialPassword: String = "",
    passwordState: TextFieldState = rememberTextFieldState(initialPassword),
    onNextClicked: (String) -> Unit = {},
    onChangeAccountClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {}
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    var showForgotInfoDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        snapshotFlow { passwordState.text }.collect {
            if (isError) isError = false
        }
    }

    val customErrorColor = Color(0xFFFF7164)

    DesktopScreenScaffold(
        artworkVariant = DesktopArtworkVariant.LOGIN_PASSWORD
    ) {
        // 1. Titel
        Text(
            text = "Passwort\neingeben",
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
            text = "Gib dein Passwort ein, um dich anzumelden.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = DesktopTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(36.dp))

        // 4. Eingabefeld Passwort
        OutlinedSecureTextField(
            state = passwordState,
            textObfuscationMode = if (passwordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
            label = { Text(if (isError) "Passwort*" else "Passwort") },
            trailingIcon = {
                IconButton(onClick = { passwordHidden = !passwordHidden }) {
                    Icon(
                        painter = painterResource(
                            if (passwordHidden) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                        ),
                        contentDescription = if (passwordHidden) "Passwort anzeigen" else "Passwort verbergen",
                        tint = DesktopTextSecondary
                    )
                }
            },
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
                        text = "Bitte gib dein Passwort ein",
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            } else null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = {
                val pwd = passwordState.text.toString().trim()
                if (pwd.isNotBlank()) {
                    keyboardController?.hide()
                    onNextClicked(pwd)
                } else {
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (isError) error("Bitte gib dein Passwort ein")
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Link "Passwort vergessen?"
        Text(
            text = "Passwort vergessen?",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textDecoration = TextDecoration.Underline
            ),
            color = DesktopLink,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    showForgotInfoDialog = true
                    onForgotPasswordClicked()
                }
        )

        Spacer(modifier = Modifier.weight(1f))

        // 6. Weiter Button
        Button(
            onClick = {
                val pwd = passwordState.text.toString().trim()
                if (pwd.isNotBlank()) {
                    onNextClicked(pwd)
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

    if (showForgotInfoDialog) {
        AlertDialog(
            onDismissRequest = { showForgotInfoDialog = false },
            containerColor = DesktopCardBackground,
            titleContentColor = DesktopTextPrimary,
            textContentColor = DesktopTextSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Passwort vergessen?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Falls du dein Passwort vergessen hast, wende dich bitte an dein Schulsekretariat oder den Administrator, um dein Passwort zurücksetzen zu lassen.",
                    color = DesktopTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotInfoDialog = false }) {
                    Text("Verstanden", color = DesktopAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Preview
@Composable
private fun DesktopLoginPasswordScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopLoginPasswordScreen()
        }
    }
}
