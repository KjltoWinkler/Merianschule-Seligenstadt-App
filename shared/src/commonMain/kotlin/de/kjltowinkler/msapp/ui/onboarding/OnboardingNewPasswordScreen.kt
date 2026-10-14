package de.kjltowinkler.msapp.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Onboarding-Screen: Neues Passwort erstellen
 * Gemäß Figma-Design (onboarding#neuespasswort.png).
 * Wird genutzt, um das Einmal-Passwort eines Accounts zu einem dauerhaften, vom Nutzer
 * gewählten Passwort zu ändern.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingNewPasswordScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = painterResource(Res.drawable.avatar_sample),
    initialPassword: String = "",
    passwordState: TextFieldState = rememberTextFieldState(initialPassword),
    confirmPasswordState: TextFieldState = rememberTextFieldState(),
    onBackClicked: () -> Unit = {},
    onFinishClicked: (String) -> Unit = {},
    onChangeAccountClicked: () -> Unit = onBackClicked,
    onPasswordInfoClicked: () -> Unit = {}
) {
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    var confirmPasswordHidden by rememberSaveable { mutableStateOf(true) }
    var showPasswordInfoDialog by remember { mutableStateOf(false) }

    // Validierungs-Fehlermeldungen
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Fehler zurücksetzen, wenn der Nutzer tippt
    LaunchedEffect(Unit) {
        snapshotFlow { passwordState.text }.collect {
            if (passwordError != null) passwordError = null
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { confirmPasswordState.text }.collect {
            if (confirmPasswordError != null) confirmPasswordError = null
        }
    }

    fun validateAndSubmit() {
        val pwd = passwordState.text.toString()
        val confirmPwd = confirmPasswordState.text.toString()

        var hasError = false

        if (pwd.length < 8) {
            passwordError = "Mindestens 8 Zeichen erforderlich"
            hasError = true
        } else if (!pwd.any { it.isDigit() || !it.isLetterOrDigit() }) {
            passwordError = "Mindestens eine Zahl oder ein Sonderzeichen erforderlich"
            hasError = true
        } else {
            passwordError = null
        }

        if (confirmPwd.isEmpty()) {
            confirmPasswordError = "Bitte bestätige dein Passwort"
            hasError = true
        } else if (pwd != confirmPwd) {
            confirmPasswordError = "Die Passwörter stimmen nicht überein"
            hasError = true
        } else {
            confirmPasswordError = null
        }

        if (!hasError) {
            keyboardController?.hide()
            onFinishClicked(pwd)
        }
    }

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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Oberer Scroll-Bereich
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Zurück-Button oben links (44x44 dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onBackClicked,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(0.dp),
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

                // 2. Headline "Neues Passwort erstellen"
                Text(
                    text = "Neues Passwort\nerstellen",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Account-Chip ("Sebastian Wagner" mit Profilbild)
                MerianAccountBadge(
                    accountName = accountName,
                    avatarPainter = avatarPainter,
                    onClick = onChangeAccountClicked
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Beschreibungstext
                Text(
                    text = "Wähle ein sicheres Passwort, mit dem du dich künftig anmeldest.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Link "Weitere Informationen zu deinem Passwort"
                Text(
                    text = "Weitere Informationen zu deinem Passwort",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            showPasswordInfoDialog = true
                            onPasswordInfoClicked()
                        }
                )

                Spacer(modifier = Modifier.height(32.dp))

                val customErrorColor = Color(0xFFFF7164)

                // 6. Erstes Passwort-Feld ("Neues Passwort")
                OutlinedSecureTextField(
                    state = passwordState,
                    label = { Text("Neues Passwort") },
                    isError = passwordError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        errorBorderColor = customErrorColor,
                        errorLabelColor = customErrorColor,
                        errorSupportingTextColor = customErrorColor,
                        errorCursorColor = customErrorColor
                    ),
                    supportingText = if (passwordError != null) {
                        {
                            Text(
                                text = passwordError ?: "",
                                modifier = Modifier.clearAndSetSemantics {}
                            )
                        }
                    } else null,
                    textObfuscationMode = if (passwordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = if (passwordHidden) KeyboardType.Password else KeyboardType.PasswordVisible,
                        imeAction = ImeAction.Next
                    ),
                    onKeyboardAction = {
                        focusManager.moveFocus(FocusDirection.Down)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordHidden = !passwordHidden }) {
                            Icon(
                                painter = painterResource(
                                    if (passwordHidden) Res.drawable.ic_visibility
                                    else Res.drawable.ic_visibility_off
                                ),
                                contentDescription = if (passwordHidden) "Neues Passwort anzeigen" else "Neues Passwort verbergen"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            if (passwordError != null) error(passwordError ?: "")
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 7. Zweites Passwort-Feld ("Passwort bestätigen")
                OutlinedSecureTextField(
                    state = confirmPasswordState,
                    label = { Text("Passwort bestätigen") },
                    isError = confirmPasswordError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        errorBorderColor = customErrorColor,
                        errorLabelColor = customErrorColor,
                        errorSupportingTextColor = customErrorColor,
                        errorCursorColor = customErrorColor
                    ),
                    supportingText = if (confirmPasswordError != null) {
                        {
                            Text(
                                text = confirmPasswordError ?: "",
                                modifier = Modifier.clearAndSetSemantics {}
                            )
                        }
                    } else null,
                    textObfuscationMode = if (confirmPasswordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = if (confirmPasswordHidden) KeyboardType.Password else KeyboardType.PasswordVisible,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        validateAndSubmit()
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordHidden = !confirmPasswordHidden }) {
                            Icon(
                                painter = painterResource(
                                    if (confirmPasswordHidden) Res.drawable.ic_visibility
                                    else Res.drawable.ic_visibility_off
                                ),
                                contentDescription = if (confirmPasswordHidden) "Passwort bestätigen anzeigen" else "Passwort bestätigen verbergen"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            if (confirmPasswordError != null) error(confirmPasswordError ?: "")
                        }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 8. Kriterien-Liste gemäß Figma
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Mindestens 8 Zeichen",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "• Mindestens eine Zahl oder ein Sonderzeichen",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 9. "Weiter" Button unten
            Button(
                onClick = { validateAndSubmit() },
                shapes = ButtonDefaults.shapes(),
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

        // Dialog: Informationen zum Passwort
        if (showPasswordInfoDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordInfoDialog = false },
                title = {
                    Text(
                        text = "Passwort-Sicherheit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Dein neues Passwort schützt deinen Schulaccount vor unbefugtem Zugriff.\n\n" +
                                "Bitte achte darauf:\n" +
                                "• Verwende mindestens 8 Zeichen.\n" +
                                "• Verwende mindestens eine Zahl oder ein Sonderzeichen (z. B. !?#*).\n" +
                                "• Teile dein Passwort mit niemandem.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPasswordInfoDialog = false }) {
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

@Preview
@Composable
fun OnboardingNewPasswordScreenPreview() {
    AppTheme(darkTheme = true) {
        OnboardingNewPasswordScreen(
            accountName = "Sebastian Wagner"
        )
    }
}
