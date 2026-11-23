package de.kjltowinkler.msapp.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import de.kjltowinkler.msapp.wear.presentation.screens.FailureConfirmationScreen
import de.kjltowinkler.msapp.wear.presentation.screens.OnboardingPairingScreen
import de.kjltowinkler.msapp.wear.presentation.screens.OnboardingPromptScreen
import de.kjltowinkler.msapp.wear.presentation.screens.OpenOnPhoneScreen
import de.kjltowinkler.msapp.wear.presentation.screens.SuccessConfirmationScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearHomeScreen
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.delay

enum class WearAppState {
    PROMPT,          // Screen 1: onboarding#1.png
    CHECK_PHONE,     // Screen 2: onboarding#2.png
    PAIRING_WAIT,    // Screen 3: onboarding#3.png
    SUCCESS,         // Screen 4: onboarding#erfolgreich.png
    FAILURE,         // Screen 5: onboarding#fehlgeschlagen.png
    HOME             // HomeScreen
}

class MainActivity : ComponentActivity() {
    private var currentScreenState = mutableStateOf(WearAppState.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        de.kjltowinkler.msapp.util.AppContextProvider.init(this)
        de.kjltowinkler.msapp.data.backend.BackendApiClient.loadSavedSession()
        updateScreenFromIntent(intent)
        setContent {
            WearAppTheme {
                WearApp(
                    appState = currentScreenState.value,
                    onStateChange = { currentScreenState.value = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        updateScreenFromIntent(intent)
    }

    private fun updateScreenFromIntent(intent: android.content.Intent?) {
        val screenName = intent?.getStringExtra("screen") ?: return
        try {
            currentScreenState.value = WearAppState.valueOf(screenName)
        } catch (_: Exception) {}
    }
}

@Composable
fun WearApp(
    appState: WearAppState,
    onStateChange: (WearAppState) -> Unit
) {
    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {
            // TimeText auf dem Homescreen anzeigen
            if (appState == WearAppState.HOME) {
                TimeText()
            }
        }
    ) {
        val contentModifier = if (appState != WearAppState.HOME) {
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tap wechselt manuell durch die Onboarding-Screens
                    onStateChange(
                        when (appState) {
                            WearAppState.PROMPT -> WearAppState.CHECK_PHONE
                            WearAppState.CHECK_PHONE -> WearAppState.PAIRING_WAIT
                            WearAppState.PAIRING_WAIT -> WearAppState.SUCCESS
                            WearAppState.SUCCESS -> WearAppState.FAILURE
                            WearAppState.FAILURE -> WearAppState.HOME
                            WearAppState.HOME -> WearAppState.PROMPT
                        }
                    )
                }
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = contentModifier) {
            when (appState) {
                WearAppState.PROMPT -> {
                    OnboardingPromptScreen(
                        onConfirm = {
                            onStateChange(WearAppState.CHECK_PHONE)
                        }
                    )
                }

                WearAppState.CHECK_PHONE -> {
                    OpenOnPhoneScreen()
                }

                WearAppState.PAIRING_WAIT -> {
                    OnboardingPairingScreen()
                }

                WearAppState.SUCCESS -> {
                    SuccessConfirmationScreen()
                }

                WearAppState.FAILURE -> {
                    FailureConfirmationScreen()
                }

                WearAppState.HOME -> {
                    WearHomeScreen(
                        onAccountClick = {
                            // Bei Klick auf Account z. B. Onboarding/Koppeln aufrufen
                            onStateChange(WearAppState.PROMPT)
                        },
                        onTimetableClick = { /* Später: Navigation Stundenplan */ },
                        onSubstitutionClick = { /* Später: Navigation Vertretungsplan */ },
                        onTasksClick = { /* Später: Navigation Aufgaben */ },
                        onSettingsClick = { /* Später: Navigation Einstellungen */ }
                    )
                }
            }
        }
    }
}
