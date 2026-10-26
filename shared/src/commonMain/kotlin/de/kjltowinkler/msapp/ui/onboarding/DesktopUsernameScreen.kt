package de.kjltowinkler.msapp.ui.onboarding

import de.kjltowinkler.msapp.ui.desktop.DesktopArtworkVariant
import de.kjltowinkler.msapp.ui.desktop.DesktopDecorativeArtwork
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
import de.kjltowinkler.msapp.theme.*
import de.kjltowinkler.msapp.ui.desktop.DesktopDecorativeArtwork
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.bg_desktop_shapes
import org.jetbrains.compose.resources.painterResource

/**
 * Anmelde-Bildschirm für die Desktop- und Webversion gemäß Figma-Design:
 * FigmaDesigns/desktop/vertretungsplan#alle.png
 *
 * Verwendet strikt die im Figma-Design definierten Farben ohne Material You.
 */
@Composable
fun DesktopUsernameScreen(
    initialUsername: String = "",
    textFieldState: TextFieldState = rememberTextFieldState(initialUsername),
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

    DesktopTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopBackground) // #111114
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Linker Bereich: Animierte dekorative Vektorgrafiken aus dem Design
                DesktopDecorativeArtwork(
                    variant = DesktopArtworkVariant.USERNAME,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Rechter Bereich: Anmelde-Panel (docked to right, #0B0B0D)
                Box(
                    modifier = Modifier
                        .widthIn(min = 380.dp, max = 430.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                        .background(DesktopCardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Abstand von oben
                        Spacer(modifier = Modifier.height(130.dp))

                        // 1. Überschrift "Anmelden"
                        Text(
                            text = "Anmelden",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                lineHeight = 44.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = DesktopTextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. Beschreibungstext
                        Text(
                            text = "Melde dich mit dem Benutzernamen an, den du von deiner Schule erhalten hast.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = DesktopTextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Link "Weitere Informationen zu deinem Konto"
                        Text(
                            text = "Weitere Informationen zu deinem Konto",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = DesktopLink, // #A9CAE8
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    showAccountInfoDialog = true
                                    onAccountInfoClicked()
                                }
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        val customErrorColor = Color(0xFFFF7164)

                        // 4. Eingabefeld "Nutzername oder Schüler-ID"
                        OutlinedTextField(
                            state = textFieldState,
                            label = {
                                Text(
                                    text = if (isError) "Nutzername oder Schüler-ID*" else "Nutzername oder Schüler-ID"
                                )
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            isError = isError,
                            shape = RoundedCornerShape(4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesktopAccentBlue,
                                unfocusedBorderColor = DesktopInputBorder, // #79747E
                                focusedLabelColor = DesktopAccentBlue,
                                unfocusedLabelColor = DesktopInputPlaceholder, // #A7A8A4
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. Link "Nutzername oder Schüler-ID vergessen?"
                        Text(
                            text = "Nutzername oder Schüler-ID vergessen?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = DesktopLink, // #A9CAE8
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    showForgotInfoDialog = true
                                    onForgotPasswordClicked()
                                }
                        )

                        // Flexibler Abstand bis zum Button
                        Spacer(modifier = Modifier.weight(1f))

                        // 6. Pill-Button "Weiter"
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
                                containerColor = DesktopAccentBlue, // #A9CAE8
                                contentColor = DesktopOnAccentBlue // #0B0B0D
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

                        // Unterer Abstand
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }

            // Dialog: Weitere Informationen zum Konto
            if (showAccountInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showAccountInfoDialog = false },
                    containerColor = DesktopCardBackground,
                    titleContentColor = DesktopTextPrimary,
                    textContentColor = DesktopTextSecondary,
                    shape = RoundedCornerShape(20.dp),
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
                            color = DesktopTextSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showAccountInfoDialog = false }) {
                            Text(
                                text = "Verstanden",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = DesktopAccentBlue
                            )
                        }
                    }
                )
            }

            // Dialog: Zugangsdaten vergessen
            if (showForgotInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showForgotInfoDialog = false },
                    containerColor = DesktopCardBackground,
                    titleContentColor = DesktopTextPrimary,
                    textContentColor = DesktopTextSecondary,
                    shape = RoundedCornerShape(20.dp),
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
                            color = DesktopTextSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showForgotInfoDialog = false }) {
                            Text(
                                text = "Verstanden",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = DesktopAccentBlue
                            )
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesktopUsernameScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopUsernameScreen()
        }
    }
}
