package de.kjltowinkler.msapp

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.onboarding.OnboardingOneTimePasswordScreen
import de.kjltowinkler.msapp.ui.onboarding.OnboardingUsernameScreen
import de.kjltowinkler.msapp.ui.onboarding.OnboardingWelcomeScreen

object AppRoutes {
    const val WELCOME = "welcome"
    const val USERNAME = "username"
    const val ONE_TIME_PASSWORD = "one_time_password"
    const val NEW_PASSWORD = "new_password"
    const val TWO_FACTOR_AUTH = "two_factor_auth"
    const val TWO_FACTOR_CONNECT = "two_factor_connect"
    const val TWO_FACTOR_CONFIRM = "two_factor_confirm"
    const val READY = "ready"
    
    // Regulärer Login für Accounts mit eigenem Passwort
    const val LOGIN_PASSWORD = "login_password"
    const val LOGIN_READY = "login_ready"

    // Hauptseite
    const val HOME = "home"

    // Einstellungen
    const val SETTINGS = "settings"
    const val SETTINGS_SCHOOL_WAY = "settings_school_way"
}

/**
 * Zustand des Accounts für die Navigation:
 * - hasCustomPassword: Hat der Account bereits ein eigenes Passwort oder noch das Einmalpasswort der Schule?
 * - isTwoFactorEnabled: Ist 2FA bereits eingerichtet?
 */
data class MockAccountState(
    val username: String = "Sebastian Wagner",
    val email: String = "sebastian.wagner@mssof.onmicrosoft.com",
    val className: String = "9aR",
    val schoolType: String = "Realschule",
    val classTeacher: String = "Hr. Thomas",
    val avatarType: String = "initials",
    val avatarColorHex: Long = 0xFF395A3CL,
    val hasCustomPassword: Boolean = false,
    val isTwoFactorEnabled: Boolean = false
)

fun createMockAccount(
    enteredUsername: String,
    hasCustomPassword: Boolean,
    isTwoFactorEnabled: Boolean
): MockAccountState {
    val clean = enteredUsername.trim()
    val lower = clean.lowercase()
    return when {
        lower.contains("anna") || lower.contains("becker") -> {
            MockAccountState(
                username = if (clean.isNotBlank()) clean else "Anna Becker",
                email = "anna.becker@mssof.onmicrosoft.com",
                className = "7bG",
                schoolType = "Gymnasium",
                classTeacher = "Fr. Schneider",
                avatarType = "initials",
                avatarColorHex = 0xFF6B4FA2L,
                hasCustomPassword = hasCustomPassword,
                isTwoFactorEnabled = isTwoFactorEnabled
            )
        }
        lower.contains("leon") || lower.contains("schmidt") -> {
            MockAccountState(
                username = if (clean.isNotBlank()) clean else "Leon Schmidt",
                email = "leon.schmidt@mssof.onmicrosoft.com",
                className = "10aH",
                schoolType = "Hauptschule",
                classTeacher = "Hr. Weber",
                avatarType = "mascot_clean",
                avatarColorHex = 0xFF007A78L,
                hasCustomPassword = hasCustomPassword,
                isTwoFactorEnabled = isTwoFactorEnabled
            )
        }
        lower.contains("thomas") || lower.contains("lehrer") -> {
            MockAccountState(
                username = if (clean.isNotBlank()) clean else "Hr. Thomas",
                email = "thomas@merianschule.de",
                className = "9aR (Leitung)",
                schoolType = "Realschule",
                classTeacher = "Hr. Thomas (Selbst)",
                avatarType = "app_icon",
                avatarColorHex = 0xFF395A3CL,
                hasCustomPassword = hasCustomPassword,
                isTwoFactorEnabled = isTwoFactorEnabled
            )
        }
        lower.contains("wagner") || lower.contains("sebastian") -> {
            MockAccountState(
                username = if (clean.isNotBlank()) clean else "Sebastian Wagner",
                email = "sebastian.wagner@mssof.onmicrosoft.com",
                className = "9aR",
                schoolType = "Realschule",
                classTeacher = "Hr. Thomas",
                avatarType = "initials",
                avatarColorHex = 0xFF395A3CL,
                hasCustomPassword = hasCustomPassword,
                isTwoFactorEnabled = isTwoFactorEnabled
            )
        }
        else -> {
            val name = if (clean.isNotBlank()) clean else "Sebastian Wagner"
            val emailPrefix = name.lowercase().replace("\\s+".toRegex(), ".")
            MockAccountState(
                username = name,
                email = "$emailPrefix@mssof.onmicrosoft.com",
                className = "8bR",
                schoolType = "Realschule",
                classTeacher = "Fr. Hoffmann",
                avatarType = "initials",
                avatarColorHex = 0xFF1B65A4L,
                hasCustomPassword = hasCustomPassword,
                isTwoFactorEnabled = isTwoFactorEnabled
            )
        }
    }
}

@Composable
@Preview
fun App() {
    var isDarkTheme by remember { mutableStateOf(true) }
    var isDynamicColor by remember { mutableStateOf(true) }

    AppTheme(darkTheme = isDarkTheme, dynamicColor = isDynamicColor) {
        val navController = rememberNavController()

        // Beispiel-Status für Demozwecke (später durch Backend / Admin-Verwaltung ersetzt)
        var currentAccount by androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(
                MockAccountState()
            )
        }

        // NavHost aus Navigation Compose (>= 2.8.0) unterstützt nativ Predictive Back Gesten:
        // 1. Auf dem Root-Screen ("welcome") ist kein vorheriger Screen auf dem Backstack vorhanden,
        //    sodass Android nativ die System-Animation (Back-to-Home) ausführt.
        // 2. Auf Folgeseiten ("username") fängt NavHost die Wischgeste ab und trackt
        //    die Geste mit popExitTransition & popEnterTransition in Echtzeit.
        NavHost(
            navController = navController,
            startDestination = AppRoutes.WELCOME,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            popExitTransition = {
                // Predictive Back Übergangsanimation: Horizontales Ausgleiten mit subtiler Skalierung und Verblassen
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f),
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            }
        ) {
            composable(AppRoutes.WELCOME) {
                OnboardingWelcomeScreen(
                    onNextClicked = {
                        navController.navigate(AppRoutes.USERNAME)
                    },
                    onAccessibilityClicked = {
                        // Schnelleinstellungen für Barrierefreiheit
                    }
                )
            }
            composable(AppRoutes.USERNAME) {
                OnboardingUsernameScreen(
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onNextClicked = { enteredUsername ->
                        // Falls der Nutzername "login" oder "test" enthält, simulieren wir einen bestehenden Account mit eigenem Passwort
                        val hasCustomPwd = enteredUsername.contains("login", ignoreCase = true) ||
                                          enteredUsername.contains("wagner", ignoreCase = true)
                        // Prüfen ob 2FA aktiv ist (z. B. wenn "2fa" im Namen steht)
                        val has2Fa = enteredUsername.contains("2fa", ignoreCase = true)

                        currentAccount = createMockAccount(
                            enteredUsername = enteredUsername,
                            hasCustomPassword = hasCustomPwd,
                            isTwoFactorEnabled = has2Fa
                        )

                        if (!hasCustomPwd) {
                            // Account hat noch kein eigenes Passwort -> Einmal-Passwort Screen
                            navController.navigate(AppRoutes.ONE_TIME_PASSWORD)
                        } else {
                            // Account hat schon ein eigenes Passwort -> Regulärer Login-Passwort Screen
                            navController.navigate(AppRoutes.LOGIN_PASSWORD)
                        }
                    }
                )
            }
            
            // --- PFAD A: ONBOARDING / ERSTEINRICHTUNG (Einmalpasswort) ---
            composable(AppRoutes.ONE_TIME_PASSWORD) {
                OnboardingOneTimePasswordScreen(
                    accountName = currentAccount.username,
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        navController.popBackStack()
                    },
                    onNextClicked = { oneTimePassword ->
                        // Nach Einmalpasswort -> Neues Passwort erstellen
                        navController.navigate(AppRoutes.NEW_PASSWORD)
                    }
                )
            }
            composable(AppRoutes.NEW_PASSWORD) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingNewPasswordScreen(
                    accountName = currentAccount.username,
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onFinishClicked = { newPassword ->
                        // Neues Passwort erstellt. Nun prüfen, ob 2FA aktiv ist:
                        if (!currentAccount.isTwoFactorEnabled) {
                            navController.navigate(AppRoutes.TWO_FACTOR_AUTH)
                        } else {
                            navController.navigate(AppRoutes.READY)
                        }
                    }
                )
            }
            composable(AppRoutes.TWO_FACTOR_AUTH) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingTwoFactorAuthScreen(
                    accountName = currentAccount.username,
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onSkipClicked = {
                        // 2FA abgelehnt ("Nein danke") -> direkt zum Ready Screen
                        navController.navigate(AppRoutes.READY)
                    },
                    onNextClicked = {
                        // 2FA einrichten ("Weiter") -> QR-Code / Connect Screen
                        navController.navigate(AppRoutes.TWO_FACTOR_CONNECT)
                    }
                )
            }
            composable(AppRoutes.TWO_FACTOR_CONNECT) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingTwoFactorConnectScreen(
                    accountName = currentAccount.username,
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onNextClicked = {
                        navController.navigate(AppRoutes.TWO_FACTOR_CONFIRM)
                    }
                )
            }
            composable(AppRoutes.TWO_FACTOR_CONFIRM) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingTwoFactorConfirmScreen(
                    accountName = currentAccount.username,
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onFinishClicked = { code ->
                        currentAccount = currentAccount.copy(isTwoFactorEnabled = true)
                        navController.navigate(AppRoutes.READY)
                    }
                )
            }
            composable(AppRoutes.READY) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingReadyScreen(
                    accountName = currentAccount.username,
                    gradeAndSchoolType = "Klasse ${currentAccount.className} • ${currentAccount.schoolType}",
                    schoolName = "Merianschule-Seligenstadt",
                    onFinishClicked = {
                        // Onboarding vollständig abgeschlossen -> Hauptscreen
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(AppRoutes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            // --- PFAD B: REGULÄRER LOGIN (Bestehender Account mit eigenem Passwort) ---
            composable(AppRoutes.LOGIN_PASSWORD) {
                de.kjltowinkler.msapp.ui.login.LoginPasswordScreen(
                    accountName = currentAccount.username,
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onNextClicked = { password ->
                        de.kjltowinkler.msapp.data.schulportal.SchulportalClient.savedSchoolId = "4322"
                        de.kjltowinkler.msapp.data.schulportal.SchulportalClient.savedUsername = currentAccount.username
                        de.kjltowinkler.msapp.data.schulportal.SchulportalClient.savedPassword = password

                        // Nach Passworteingabe: Prüfen ob 2FA an ist
                        if (currentAccount.isTwoFactorEnabled) {
                            // Wenn 2FA an ist -> Willkommen zurück Screen
                            navController.navigate(AppRoutes.LOGIN_READY)
                        } else {
                            // Wenn 2FA noch nicht an ist -> Nachfragen / 2FA Einrichten anbieten
                            navController.navigate(AppRoutes.TWO_FACTOR_AUTH)
                        }
                    }
                )
            }
            composable(AppRoutes.LOGIN_READY) {
                de.kjltowinkler.msapp.ui.login.LoginReadyScreen(
                    accountName = currentAccount.username,
                    gradeAndSchoolType = "Klasse ${currentAccount.className} • ${currentAccount.schoolType}",
                    schoolName = "Merianschule-Seligenstadt",
                    onNextClicked = {
                        // Weiter zum Hauptscreen
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(AppRoutes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            // --- HAUPTSEITE / DASHBOARD ---
            composable(AppRoutes.HOME) {
                de.kjltowinkler.msapp.ui.home.HomeScreen(
                    accountName = currentAccount.username,
                    accountEmail = currentAccount.email,
                    className = currentAccount.className,
                    schoolType = currentAccount.schoolType,
                    classTeacher = currentAccount.classTeacher,
                    avatarType = currentAccount.avatarType,
                    avatarColorHex = currentAccount.avatarColorHex,
                    onAvatarChanged = { type, colorHex ->
                        currentAccount = currentAccount.copy(
                            avatarType = type,
                            avatarColorHex = colorHex
                        )
                    },
                    onSettingsClicked = {
                        navController.navigate(AppRoutes.SETTINGS)
                    },
                    onLogoutClicked = {
                        navController.navigate(AppRoutes.WELCOME) {
                            popUpTo(AppRoutes.WELCOME) { inclusive = true }
                        }
                    },
                    useMaterialYou = isDynamicColor
                )
            }

            // --- EINSTELLUNGEN ---
            composable(AppRoutes.SETTINGS) {
                de.kjltowinkler.msapp.ui.settings.SettingsScreen(
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    useMaterialYou = isDynamicColor,
                    onMaterialYouChanged = { isDynamicColor = it },
                    initialDarkTheme = isDarkTheme,
                    onDarkThemeChanged = { isDarkTheme = it },
                    onSchoolWayClicked = {
                        navController.navigate(AppRoutes.SETTINGS_SCHOOL_WAY)
                    }
                )
            }

            // --- EINSTELLUNGEN: MEIN SCHULWEG ---
            composable(AppRoutes.SETTINGS_SCHOOL_WAY) {
                de.kjltowinkler.msapp.ui.settings.SchoolWayScreen(
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    useMaterialYou = isDynamicColor
                )
            }
        }
    }
}