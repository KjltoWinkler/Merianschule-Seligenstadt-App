package de.kjltowinkler.msapp.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingUsernameScreen(
    initialUsername: String = "",
    textFieldState: TextFieldState = rememberTextFieldState(initialUsername),
    onBackClicked: () -> Unit = {},
    onNextClicked: (String) -> Unit = {},
    onAccountInfoClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {}
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var showAccountInfoDialog by remember { mutableStateOf(false) }
    var showForgotInfoDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }.collect {
            if (isError) isError = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (maxWidth >= 840.dp) {
            DesktopUsernameScreen(
                initialUsername = initialUsername,
                textFieldState = textFieldState,
                onNextClicked = onNextClicked,
                onAccountInfoClicked = onAccountInfoClicked,
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

            // 2. Headline "Anmelden"
            Text(
                text = "Anmelden",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Subtitel
            Text(
                text = "Melde dich mit dem Benutzernamen an, den du von deiner Schule erhalten hast.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Link "Weitere Informationen zu deinem Konto"
            Text(
                text = "Weitere Informationen zu deinem Konto",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textDecoration = TextDecoration.Underline
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        showAccountInfoDialog = true
                        onAccountInfoClicked()
                    }
            )

            Spacer(modifier = Modifier.height(36.dp))

            val customErrorColor = Color(0xFFFF7164)

            // 5. OutlinedTextField (SimpleOutlinedTextField Variante)
            OutlinedTextField(
                state = textFieldState,
                label = { Text(if (isError) "Nutzername oder Schüler-ID*" else "Nutzername oder Schüler-ID") },
                lineLimits = TextFieldLineLimits.SingleLine,
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
                            text = "Bitte gib deinen Nutzernamen oder deine Schüler-ID ein",
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                onKeyboardAction = {
                    val username = textFieldState.text.toString().trim()
                    if (username.isNotBlank()) {
                        keyboardController?.hide()
                        onNextClicked(username)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (isError) error("Bitte gib deinen Nutzernamen oder deine Schüler-ID ein")
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Link "Nutzername oder Schüler-ID vergessen?"
            Text(
                text = "Nutzername oder Schüler-ID vergessen?",
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

            // Puffer nach unten
            Spacer(modifier = Modifier.weight(1f))

            // 7. "Weiter" Button
            Button(
                onClick = {
                    val username = textFieldState.text.toString().trim()
                    if (username.isNotBlank()) {
                        onNextClicked(username)
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

        // Dialog: Weitere Informationen zum Konto
        if (showAccountInfoDialog) {
            AlertDialog(
                onDismissRequest = { showAccountInfoDialog = false },
                title = {
                    Text(
                        text = "Informationen zum Konto",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Dein Benutzername bzw. deine Schüler-ID wurde dir von deiner Schule ausgehändigt. Falls du diese Zugangsdaten noch nicht erhalten hast, wende dich bitte an dein Schulsekretariat oder deine Klassenleitung.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAccountInfoDialog = false }) {
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

        // Dialog: Zugangsdaten vergessen
        if (showForgotInfoDialog) {
            AlertDialog(
                onDismissRequest = { showForgotInfoDialog = false },
                title = {
                    Text(
                        text = "Zugangsdaten vergessen?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Wenn du deinen Benutzernamen oder deine Schüler-ID vergessen hast, kann dir deine Schule die Zugangsdaten erneut mitteilen oder dein Konto zurücksetzen.",
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
private fun OnboardingUsernameScreenPreview() {
    AppTheme(darkTheme = true) {
        OnboardingUsernameScreen()
    }
}