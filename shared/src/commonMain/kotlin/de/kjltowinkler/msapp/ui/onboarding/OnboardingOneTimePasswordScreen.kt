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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import de.kjltowinkler.msapp.ui.desktop.DesktopOneTimePasswordScreen
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Onboarding-Screen: Einmal-Passwort eingeben
 * Gemäß Figma-Design (onboarding#einmalpasswort.png).
 * Wird angezeigt, wenn sich ein Nutzer zum ersten Mal anmeldet.
 * Nach erfolgreicher Eingabe folgt der Screen zur Erstellung eines neuen Passworts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingOneTimePasswordScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    initialPassword: String = "",
    passwordState: TextFieldState = rememberTextFieldState(initialPassword),
    onBackClicked: () -> Unit = {},
    onNextClicked: (String) -> Unit = {},
    onChangeAccountClicked: () -> Unit = onBackClicked,
    onPasswordInfoClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {}
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    var showPasswordInfoDialog by remember { mutableStateOf(false) }
    var showForgotInfoDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Reaktiv auf Änderungen im TextFieldState reagieren und Fehlerstatus zurücksetzen
    LaunchedEffect(Unit) {
        snapshotFlow { passwordState.text }.collect {
            if (isError) isError = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (maxWidth >= 840.dp) {
            DesktopOneTimePasswordScreen(
                accountName = accountName,
                avatarPainter = avatarPainter,
                initialPassword = initialPassword,
                passwordState = passwordState,
                onNextClicked = onNextClicked,
                onChangeAccountClicked = onChangeAccountClicked,
                onPasswordInfoClicked = onPasswordInfoClicked,
                onForgotPasswordClicked = onForgotPasswordClicked
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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Oberer Bereich: Nimmt allen Platz ein, drückt Button nach unten und scrollt falls nötig
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

                // 2. Headline "Einmal-Passwort eingeben"
                Text(
                    text = "Einmal-Passwort\neingeben",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Nutzer-Badge / Account-Chip ("Sebastian Wagner" mit Avatar)
                MerianAccountBadge(
                    accountName = accountName,
                    avatarPainter = avatarPainter,
                    onClick = onChangeAccountClicked
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Beschreibungstext
                Text(
                    text = "Gib das Einmalpasswort ein, das du von deiner Schule erhalten hast.",
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

                Spacer(modifier = Modifier.height(36.dp))

                val customErrorColor = Color(0xFFFF7164)

                // 6. OutlinedSecureTextField mit Sichtbarkeits-Toggle & Error-Status
                OutlinedSecureTextField(
                    state = passwordState,
                    label = { Text(if (isError) "Einmalpasswort*" else "Einmalpasswort") },
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        errorBorderColor = customErrorColor,
                        errorLabelColor = customErrorColor,
                        errorSupportingTextColor = customErrorColor,
                        errorCursorColor = customErrorColor
                    ),
                    supportingText = if (isError) {
                        {
                            Text(
                                text = "Bitte gib dein Einmal-Passwort ein",
                                modifier = Modifier.clearAndSetSemantics {}
                            )
                        }
                    } else null,
                    textObfuscationMode = if (passwordHidden) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        val password = passwordState.text.toString().trim()
                        if (password.isNotBlank()) {
                            keyboardController?.hide()
                            onNextClicked(password)
                        } else {
                            isError = true
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordHidden = !passwordHidden }) {
                            Icon(
                                painter = painterResource(
                                    if (passwordHidden) Res.drawable.ic_visibility
                                    else Res.drawable.ic_visibility_off
                                ),
                                contentDescription = if (passwordHidden) "Passwort anzeigen" else "Passwort verbergen"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            if (isError) error("Bitte gib dein Einmal-Passwort ein")
                        }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 7. Link "Passwort vergessen?"
                Text(
                    text = "Passwort vergessen?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            showForgotInfoDialog = true
                            onForgotPasswordClicked()
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 8. "Weiter" Button – sitzt fest am unteren Rand bzw. direkt über der Tastatur
            Button(
                onClick = {
                    val password = passwordState.text.toString().trim()
                    if (password.isNotBlank()) {
                        onNextClicked(password)
                    } else {
                        isError = true
                    }
                },
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

        // Dialog: Weitere Informationen zum Einmalpasswort
        if (showPasswordInfoDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordInfoDialog = false },
                title = {
                    Text(
                        text = "Einmal-Passwort",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Ein Einmal-Passwort ist ein vorübergehendes Passwort, das dir von deiner Schule ausgestellt wurde.\n\nNach der Eingabe wirst du direkt aufgefordert, dein persönliches neues Passwort festzulegen.",
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

        // Dialog: Passwort vergessen
        if (showForgotInfoDialog) {
            AlertDialog(
                onDismissRequest = { showForgotInfoDialog = false },
                title = {
                    Text(
                        text = "Passwort vergessen?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Wenn du dein Einmal-Passwort nicht erhalten hast oder es abgelaufen ist, wende dich bitte an das Schulsekretariat oder an deine Klassenleitung, um neue Zugangsdaten zu erhalten.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showForgotInfoDialog = false }) {
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
fun OnboardingOneTimePasswordScreenPreview() {
    AppTheme(darkTheme = true) {
        OnboardingOneTimePasswordScreen(
            accountName = "Sebastian Wagner"
        )
    }
}