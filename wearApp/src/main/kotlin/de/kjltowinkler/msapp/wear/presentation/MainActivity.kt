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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.settings.SettingsRepository
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.data.wear.PlatformWearPairing
import de.kjltowinkler.msapp.wear.presentation.screens.FailureConfirmationScreen
import de.kjltowinkler.msapp.wear.presentation.screens.OnboardingPairingScreen
import de.kjltowinkler.msapp.wear.presentation.screens.OnboardingPromptScreen
import de.kjltowinkler.msapp.wear.presentation.screens.OpenOnPhoneScreen
import de.kjltowinkler.msapp.wear.presentation.screens.SuccessConfirmationScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearAccountsScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearCreateTaskScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearHomeScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearSettingsScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearSubstitutionScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearTasksScreen
import de.kjltowinkler.msapp.wear.presentation.screens.WearTimetableScreen
import de.kjltowinkler.msapp.wear.presentation.theme.WearAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private var pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        de.kjltowinkler.msapp.util.AppContextProvider.init(this)
        BackendApiClient.loadSavedSession()
        PlatformWearPairing.startListening()
        if (BackendApiClient.isLoggedIn) {
            TasksRepository.startRealtimeSync()
        }
        updateScreenFromIntent(intent)
        setContent {
            WearAppTheme(useDynamicColors = SettingsRepository.settings.isDynamicColor) {
                WearApp(
                    appState = currentScreenState.value,
                    onStateChange = { currentScreenState.value = it },
                    pendingRoute = pendingRoute.value,
                    onRouteConsumed = { pendingRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        updateScreenFromIntent(intent)
    }

    private fun updateScreenFromIntent(intent: android.content.Intent?) {
        val route = intent?.getStringExtra("route")
        if (!route.isNullOrBlank()) {
            currentScreenState.value = WearAppState.HOME
            pendingRoute.value = route
        }
        val screenName = intent?.getStringExtra("screen") ?: return
        try {
            currentScreenState.value = WearAppState.valueOf(screenName)
        } catch (_: Exception) {}
    }
}

@Composable
fun WearApp(
    appState: WearAppState,
    onStateChange: (WearAppState) -> Unit,
    pendingRoute: String? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    // Eingehende Session vom Smartphone empfangen und automatisch speichern
    LaunchedEffect(Unit) {
        PlatformWearPairing.incomingPairSession.collect { payload ->
            BackendApiClient.saveSession(payload.token, payload.user)
            TasksRepository.startRealtimeSync()
            launch { TasksRepository.reloadAndSync() }
            PlatformWearPairing.sendPairAckToPhone()
            onStateChange(WearAppState.SUCCESS)
        }
    }

    // Timeout für Warten auf Smartphone-Kopplung & Auto-Weiterleitung bei Erfolg
    LaunchedEffect(appState) {
        when (appState) {
            WearAppState.CHECK_PHONE -> {
                delay(1500)
                onStateChange(WearAppState.PAIRING_WAIT)
            }
            WearAppState.PAIRING_WAIT -> {
                delay(45_000)
                onStateChange(WearAppState.FAILURE)
            }
            WearAppState.SUCCESS -> {
                delay(2000)
                onStateChange(WearAppState.HOME)
            }
            else -> Unit
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {
            // TimeText auf dem Homescreen anzeigen
            if (appState == WearAppState.HOME) {
                TimeText()
            }
        }
    ) {
        val contentModifier = if (appState == WearAppState.SUCCESS || appState == WearAppState.FAILURE) {
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onStateChange(
                        when (appState) {
                            WearAppState.SUCCESS -> WearAppState.HOME
                            WearAppState.FAILURE -> WearAppState.PROMPT
                            else -> appState
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
                            coroutineScope.launch {
                                val res = PlatformWearPairing.requestPairingFromPhone()
                                if (res.isFailure) {
                                    onStateChange(WearAppState.FAILURE)
                                }
                            }
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
                    val navController = rememberSwipeDismissableNavController()
                    LaunchedEffect(pendingRoute) {
                        if (!pendingRoute.isNullOrBlank()) {
                            navController.navigate(pendingRoute) {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                            onRouteConsumed()
                        }
                    }
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            WearHomeScreen(
                                onAccountClick = {
                                    navController.navigate("accounts")
                                },
                                onTimetableClick = {
                                    navController.navigate("timetable")
                                },
                                onSubstitutionClick = {
                                    navController.navigate("substitution")
                                },
                                onTasksClick = {
                                    navController.navigate("tasks")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("accounts") {
                            WearAccountsScreen(
                                onActiveAccountClick = {
                                    navController.popBackStack()
                                },
                                onAddAccountClick = {
                                    onStateChange(WearAppState.PROMPT)
                                }
                            )
                        }
                        composable("timetable") {
                            WearTimetableScreen()
                        }
                        composable("substitution") {
                            WearSubstitutionScreen()
                        }
                        composable("settings") {
                            WearSettingsScreen(
                                onPairPhoneClick = {
                                    onStateChange(WearAppState.PROMPT)
                                }
                            )
                        }
                        composable("tasks") {
                            WearTasksScreen(
                                onCreateTaskClick = { listId ->
                                    navController.navigate("create_task/$listId")
                                },
                                onLoginRequiredClick = {
                                    onStateChange(WearAppState.PROMPT)
                                }
                            )
                        }
                        composable("create_task/{listId}") { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: "my_tasks"
                            WearCreateTaskScreen(
                                initialListId = listId,
                                onTaskCreated = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
