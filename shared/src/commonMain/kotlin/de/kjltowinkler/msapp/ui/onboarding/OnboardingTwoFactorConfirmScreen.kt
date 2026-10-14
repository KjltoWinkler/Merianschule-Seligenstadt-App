package de.kjltowinkler.msapp.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
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
 * Onboarding-Screen: Code-Bestätigung (2FA Bestätigung)
 * Gemäß Figma-Design (onboarding#bestätigen.png).
 * 
 * Ermöglicht die Eingabe eines Codes (z. B. 6-stellig aus Authenticator-App).
 * Man kann flexibel jeden Code eingeben, um ihn zu bestätigen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingTwoFactorConfirmScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = painterResource(Res.drawable.avatar_sample),
    initialCode: String = "",
    codeState: TextFieldState = rememberTextFieldState(initialCode),
    onBackClicked: () -> Unit = {},
    onChangeAccountClicked: () -> Unit = onBackClicked,
    onHelpClicked: () -> Unit = {},
    onFinishClicked: (String) -> Unit = {}
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Fehler zurücksetzen, wenn der Nutzer tippt
    LaunchedEffect(Unit) {
        snapshotFlow { codeState.text }.collect {
            if (isError) {
                isError = false
                errorMessage = null
            }
        }
    }

    fun validateAndSubmit() {
        val code = codeState.text.toString().trim()
        if (code.isBlank()) {
            isError = true
            errorMessage = "Bitte gib deinen Code ein"
        } else {
            // Erstmal alles zulassen, um es bestätigen zu können
            keyboardController?.hide()
            onFinishClicked(code)
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

                // 2. Headline "Code-Bestätigung"
                Text(
                    text = "Code-Bestätigung",
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
                    onClick = onChangeAccountClicked
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Beschreibungstext
                Text(
                    text = "Gib den 6-stelligen Code ein, der dir in deiner Authenticator-App angezeigt wird.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                val customErrorColor = Color(0xFFFF7164)

                // 5. OutlinedTextField ("6-stelligen Code eingeben")
                OutlinedTextField(
                    state = codeState,
                    label = { Text(if (isError) "6-stelligen Code eingeben*" else "6-stelligen Code eingeben") },
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
                                text = errorMessage ?: "Bitte gib deinen Code ein",
                                modifier = Modifier.clearAndSetSemantics {}
                            )
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        validateAndSubmit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            if (isError) error(errorMessage ?: "Bitte gib deinen Code ein")
                        }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Link "Hilfe?"
                Text(
                    text = "Hilfe?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
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

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 7. "Fertigstellen" Button am unteren Rand
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
                    .semantics { contentDescription = "Fertigstellen" }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fertigstellen",
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

        // Hilfe-Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Text(
                        text = "Code-Bestätigung Hilfe",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Öffne die Authenticator-App auf deinem Smartphone. Suche nach dem Eintrag für die Merianschule und gib den aktuell dort angezeigten 6-stelligen Code ein.\n\n" +
                                "Beachte, dass sich der Code alle 30 Sekunden aktualisiert.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Preview
@Composable
fun OnboardingTwoFactorConfirmScreenPreview() {
    AppTheme(darkTheme = true) {
        OnboardingTwoFactorConfirmScreen(
            accountName = "Sebastian Wagner"
        )
    }
}
