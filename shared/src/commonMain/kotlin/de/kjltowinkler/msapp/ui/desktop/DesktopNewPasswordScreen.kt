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
fun DesktopNewPasswordScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    initialPassword: String = "",
    passwordState: TextFieldState = rememberTextFieldState(initialPassword),
    confirmPasswordState: TextFieldState = rememberTextFieldState(""),
    onFinishClicked: (String) -> Unit = {},
    onChangeAccountClicked: () -> Unit = {},
    onPasswordInfoClicked: () -> Unit = {}
) {
    var isLengthError by rememberSaveable { mutableStateOf(false) }
    var isSpecialCharError by rememberSaveable { mutableStateOf(false) }
    var isMismatchError by rememberSaveable { mutableStateOf(false) }
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    var confirmPasswordHidden by rememberSaveable { mutableStateOf(true) }
    var showPasswordInfoDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        snapshotFlow { passwordState.text }.collect {
            if (isLengthError) isLengthError = false
            if (isSpecialCharError) isSpecialCharError = false
            if (isMismatchError) isMismatchError = false
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { confirmPasswordState.text }.collect {
            if (isMismatchError) isMismatchError = false
        }
    }

    fun validateAndProceed() {
        val pwd = passwordState.text.toString()
        val confirm = confirmPasswordState.text.toString()

        if (pwd.length < 8) {
            isLengthError = true
            return
        }
        val hasDigitOrSpecial = pwd.any { it.isDigit() || !it.isLetterOrDigit() }
        if (!hasDigitOrSpecial) {
            isSpecialCharError = true
            return
        }
        if (pwd != confirm) {
            isMismatchError = true
            return
        }

        keyboardController?.hide()
        onFinishClicked(pwd)
    }

    val customErrorColor = Color(0xFFFF7164)

    DesktopScreenScaffold(
        artworkVariant = DesktopArtworkVariant.NEW_PASSWORD,
        topSpacing = 90.dp
    ) {
        // 1. Titel
        Text(
            text = "Neues Passwort\nerstellen",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                lineHeight = 44.sp,
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

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Beschreibung
        Text(
            text = "Wähle ein sicheres Passwort, mit dem du dich künftig anmeldest.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = DesktopTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Link "Weitere Informationen zu deinem Passwort"
        Text(
            text = "Weitere Informationen zu deinem Passwort",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textDecoration = TextDecoration.Underline
            ),
            color = DesktopLink,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    showPasswordInfoDialog = true
                    onPasswordInfoClicked()
                }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 5. Eingabefeld 1: Neues Passwort
        OutlinedSecureTextField(
            state = passwordState,
            textObfuscationMode = if (passwordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
            label = { Text("Neues Passwort") },
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
            isError = isLengthError || isSpecialCharError,
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
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Eingabefeld 2: Passwort bestätigen
        OutlinedSecureTextField(
            state = confirmPasswordState,
            textObfuscationMode = if (confirmPasswordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
            label = { Text("Passwort bestätigen") },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordHidden = !confirmPasswordHidden }) {
                    Icon(
                        painter = painterResource(
                            if (confirmPasswordHidden) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                        ),
                        contentDescription = if (confirmPasswordHidden) "Passwort anzeigen" else "Passwort verbergen",
                        tint = DesktopTextSecondary
                    )
                }
            },
            isError = isMismatchError,
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
            supportingText = if (isMismatchError) {
                {
                    Text(
                        text = "Die Passwörter stimmen nicht überein",
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
            onKeyboardAction = { validateAndProceed() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Passwort-Kriterien
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "• Mindestens 8 Zeichen",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = if (isLengthError) customErrorColor else DesktopTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Mindestens eine Zahl oder ein Sonderzeichen",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = if (isSpecialCharError) customErrorColor else DesktopTextSecondary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 8. Weiter Button
        Button(
            onClick = { validateAndProceed() },
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

    if (showPasswordInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordInfoDialog = false },
            containerColor = DesktopCardBackground,
            titleContentColor = DesktopTextPrimary,
            textContentColor = DesktopTextSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Passwort-Sicherheit", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Wähle ein starkes Passwort, das du nicht für andere Dienste verwendest. Es muss mindestens 8 Zeichen lang sein und Zahlen oder Sonderzeichen enthalten.",
                    color = DesktopTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPasswordInfoDialog = false }) {
                    Text("Verstanden", color = DesktopAccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Preview
@Composable
private fun DesktopNewPasswordScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopNewPasswordScreen()
        }
    }
}
