package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import de.kjltowinkler.msapp.PlatformPredictiveBackHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Vollbild-Screen "Neue Liste erstellen" exakt nach Figma: `tasks#neueliste.png`
 * und Benutzer-Screenshot:
 * - Echtes Vollbild (ohne Dialog-Window), nahtlos edge-to-edge ohne schwarze Balken oben/unten
 * - Oben links: Schließen-Button "✕" (`ic_close`)
 * - Direkt daneben: Titel "Neue Liste erstellen" (Fettschrift, 20.sp)
 * - Oben rechts: "Fertig"-Aktion (deaktiviert bei leerer Eingabe, akzentuiert bei getipptem Text)
 * - Eingabefeld: "Listentitel eingeben" mit abgerundeten Ecken (4.dp) und hellgrünem Fokus-Rahmen
 * - Bildschirmtastatur wird automatisch fokussiert und geöffnet
 */
@Composable
fun NewListScreen(
    onDismissRequest: () -> Unit,
    onSaveList: (String) -> Unit,
    useMaterialYou: Boolean = true
) {
    var listTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorScheme = MaterialTheme.colorScheme

    val screenBg = if (useMaterialYou) colorScheme.background else Color(0xFF0C0F0B)
    val textPrimary = if (useMaterialYou) colorScheme.onBackground else Color.White
    val textSecondary = if (useMaterialYou) colorScheme.onSurfaceVariant else Color(0xFF8E958C)
    val accentColor = if (useMaterialYou) colorScheme.primary else Color(0xFFABD0AA)
    val disabledColor = if (useMaterialYou) colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else Color(0xFF5E655C)
    val borderColor = if (useMaterialYou) colorScheme.outlineVariant else Color(0xFF424940)

    val isValid = listTitle.trim().isNotEmpty()

    LaunchedEffect(Unit) {
        delay(60)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    var backProgress by remember { mutableFloatStateOf(0f) }

    PlatformPredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            progressFlow.collect { event ->
                backProgress = event.progress
            }
            onDismissRequest()
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    val scale = 1f - (backProgress * 0.1f)
    val cornerRadius = (backProgress * 28f).dp
    val offsetX = (backProgress * 48f).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX.toPx()
                clip = backProgress > 0f
                shape = RoundedCornerShape(cornerRadius)
            }
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 10.dp)
        ) {
            // --- HEADER: [✕] Neue Liste erstellen       [Fertig] ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Schließen",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Neue Liste erstellen",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        if (isValid) {
                            onSaveList(listTitle.trim())
                            onDismissRequest()
                        }
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = accentColor,
                        disabledContentColor = disabledColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Fertig",
                        fontSize = 16.sp,
                        fontWeight = if (isValid) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- EINGABEFELD: "Listentitel eingeben" ---
            OutlinedTextField(
                value = listTitle,
                onValueChange = { listTitle = it },
                placeholder = {
                    Text(
                        text = "Listentitel eingeben",
                        color = textSecondary,
                        fontSize = 16.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = accentColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isValid) {
                            onSaveList(listTitle.trim())
                            onDismissRequest()
                        }
                    }
                )
            )
        }
    }
}
