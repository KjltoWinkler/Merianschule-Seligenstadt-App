package de.kjltowinkler.msapp.ui.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.wear.PlatformWearPairing
import kotlinx.coroutines.delay

private sealed interface PairingUiState {
    data object Connecting : PairingUiState
    data class Success(val deviceName: String) : PairingUiState
    data class Error(val message: String) : PairingUiState
}

/**
 * Mobile Screen #2 für das Smartwatch-Pairing (Figma: pairwithsmartwatch#pairing.png).
 * Sendet die aktive App-Session per Wearable Data Layer an die verbundene Smartwatch.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PairSmartwatchPairingScreen(
    onFinished: () -> Unit = {},
    onBackClicked: () -> Unit = {}
) {
    var uiState by remember { mutableStateOf<PairingUiState>(PairingUiState.Connecting) }
    var attemptTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(attemptTrigger) {
        uiState = PairingUiState.Connecting
        val token = BackendApiClient.authToken
        val user = BackendApiClient.currentUser
        if (token.isNullOrBlank() || user == null) {
            uiState = PairingUiState.Error(
                "Bitte melde dich zuerst auf deinem Smartphone an, bevor du deine Smartwatch koppelst."
            )
            return@LaunchedEffect
        }

        val result = PlatformWearPairing.sendSessionToWatch(token, user)
        if (result.isSuccess) {
            val deviceName = result.getOrNull().orEmpty().ifBlank { "Smartwatch" }
            uiState = PairingUiState.Success(deviceName)
            delay(1500)
            onFinished()
        } else {
            uiState = PairingUiState.Error(
                result.exceptionOrNull()?.message
                    ?: "Verbindung zur Smartwatch fehlgeschlagen."
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(52.dp))

        when (val state = uiState) {
            PairingUiState.Connecting -> {
                val strokePx = with(LocalDensity.current) { 4.5.dp.toPx() }
                val waveStroke = remember(strokePx) {
                    Stroke(width = strokePx, cap = StrokeCap.Round)
                }

                Text(
                    text = "Wird verbunden...",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    stroke = waveStroke,
                    trackStroke = waveStroke,
                    wavelength = 28.dp
                )
            }

            is PairingUiState.Success -> {
                Text(
                    text = "Verbunden!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${state.deviceName} wurde erfolgreich mit Notes+ gekoppelt.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            is PairingUiState.Error -> {
                Text(
                    text = "Verbindung fehlgeschlagen",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClicked,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = { attemptTrigger++ },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Erneut")
                    }
                }
            }
        }
    }
}
