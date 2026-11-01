package de.kjltowinkler.msapp

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.kjltowinkler.msapp.data.auth.LoginRequest
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.components.LocalPredictiveBackEnabled
import de.kjltowinkler.msapp.ui.onboarding.OnboardingOneTimePasswordScreen
import de.kjltowinkler.msapp.ui.onboarding.OnboardingUsernameScreen
import de.kjltowinkler.msapp.ui.onboarding.OnboardingWelcomeScreen
import kotlinx.coroutines.launch

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
    const val SETTINGS_COURSES = "settings_courses"
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
                email = if (clean.contains("@")) clean else "sebastian.wagner@mssof.onmicrosoft.com",
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
            val email = if (clean.contains("@")) clean else {
                val emailPrefix = name.lowercase().replace("\\s+".toRegex(), ".")
                "$emailPrefix@mssof.onmicrosoft.com"
            }
            MockAccountState(
                username = name,
                email = email,
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
    var isPredictiveBackEnabled by remember { mutableStateOf(true) }

    AppTheme(darkTheme = isDarkTheme, dynamicColor = isDynamicColor) {
        CompositionLocalProvider(
            LocalPredictiveBackEnabled provides isPredictiveBackEnabled
        ) {
            val navController = rememberNavController()

            PlatformNavigationBackHandler(navController)

        val coroutineScope = rememberCoroutineScope()
        var loginError by remember { mutableStateOf<String?>(null) }
        var isLoggingIn by remember { mutableStateOf(false) }

        // Aktueller Account-Status
        var currentAccount by remember {
            mutableStateOf(
                MockAccountState()
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isDesktop = getPlatform().name.startsWith("Desktop") || maxWidth >= 840.dp
            val initialRoute = remember {
                if (isDesktop) AppRoutes.USERNAME else AppRoutes.WELCOME
            }

            // NavHost aus Navigation Compose (>= 2.8.0) unterstützt nativ Predictive Back Gesten:
            // 1. Auf dem Root-Screen ist kein vorheriger Screen auf dem Backstack vorhanden,
            //    sodass Android nativ die System-Animation (Back-to-Home) ausführt.
            // 2. Auf Folgeseiten fängt NavHost die Wischgeste ab und trackt
            //    die Geste mit popExitTransition & popEnterTransition in Echtzeit.
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .platformEdgeBackGesture(),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 5 },
                        animationSpec = tween(durationMillis = 300)
                    ) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                },
                popEnterTransition = {
                    // Predictive Back: Der zurückgekehrte Screen vergrößert sich subtil aus dem Hintergrund
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 5 },
                        animationSpec = tween(durationMillis = 300)
                    ) + (if (isPredictiveBackEnabled) {
                        scaleIn(
                            initialScale = 0.92f,
                            transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f),
                            animationSpec = tween(durationMillis = 300)
                        )
                    } else EnterTransition.None) + fadeIn(animationSpec = tween(durationMillis = 300))
                },
                popExitTransition = {
                    // Predictive Back: Der aktuelle Screen gleitet zur Seite mit Skalierung und Verblassen
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + (if (isPredictiveBackEnabled) {
                        scaleOut(
                            targetScale = 0.88f,
                            transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f),
                            animationSpec = tween(durationMillis = 300)
                        )
                    } else ExitTransition.None) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
            ) {
                composable(AppRoutes.WELCOME) {
                    if (isDesktop) {
                        OnboardingUsernameScreen(
                            onBackClicked = {},
                            onNextClicked = { enteredUsername ->
                                val hasOtp = enteredUsername.contains("otp", ignoreCase = true) ||
                                            enteredUsername.contains("einmal", ignoreCase = true)
                                val has2Fa = enteredUsername.contains("2fa", ignoreCase = true)

                                currentAccount = createMockAccount(
                                    enteredUsername = enteredUsername,
                                    hasCustomPassword = !hasOtp,
                                    isTwoFactorEnabled = has2Fa
                                )
                                loginError = null

                                if (hasOtp) {
                                    navController.navigate(AppRoutes.ONE_TIME_PASSWORD)
                                } else {
                                    navController.navigate(AppRoutes.LOGIN_PASSWORD)
                                }
                            }
                        )
                    } else {
                        OnboardingWelcomeScreen(
                            onNextClicked = {
                                navController.navigate(AppRoutes.USERNAME)
                            },
                            onAccessibilityClicked = {
                                // Schnelleinstellungen für Barrierefreiheit
                            }
                        )
                    }
                }
                composable(AppRoutes.USERNAME) {
                    OnboardingUsernameScreen(
                        onBackClicked = {
                            if (!isDesktop) {
                                navController.popBackStack()
                            }
                        },
                        onNextClicked = { enteredUsername ->
                            val hasOtp = enteredUsername.contains("otp", ignoreCase = true) ||
                                        enteredUsername.contains("einmal", ignoreCase = true)
                            val has2Fa = enteredUsername.contains("2fa", ignoreCase = true)

                            currentAccount = createMockAccount(
                                enteredUsername = enteredUsername,
                                hasCustomPassword = !hasOtp,
                                isTwoFactorEnabled = has2Fa
                            )
                            loginError = null

                            if (hasOtp) {
                                navController.navigate(AppRoutes.ONE_TIME_PASSWORD)
                            } else {
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
                            popUpTo(initialRoute) { inclusive = true }
                        }
                    }
                )
            }

            // --- PFAD B: REGULÄRER LOGIN (Bestehender Account mit eigenem Passwort) ---
            composable(AppRoutes.LOGIN_PASSWORD) {
                de.kjltowinkler.msapp.ui.login.LoginPasswordScreen(
                    accountName = currentAccount.username,
                    isLoading = isLoggingIn,
                    errorMessage = loginError,
                    onBackClicked = {
                        loginError = null
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        loginError = null
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onNextClicked = { password ->
                        loginError = null
                        isLoggingIn = true

                        val identifier = if (currentAccount.email.isNotBlank() && currentAccount.email.contains("@")) {
                            currentAccount.email
                        } else {
                            currentAccount.username
                        }

                        coroutineScope.launch {
                            val result = BackendApiClient.login(LoginRequest(usernameOrEmail = identifier, password = password))
                                .recoverCatching {
                                    // Fallback: Wenn Login mit E-Mail fehlschlug, versuchen wir es mit dem Benutzernamen
                                    if (identifier != currentAccount.username && currentAccount.username.isNotBlank()) {
                                        BackendApiClient.login(LoginRequest(usernameOrEmail = currentAccount.username, password = password)).getOrThrow()
                                    } else {
                                        throw it
                                    }
                                }

                            result.onSuccess { auth ->
                                isLoggingIn = false
                                loginError = null

                                currentAccount = MockAccountState(
                                    username = auth.user.fullName.ifBlank { auth.user.username },
                                    email = auth.user.email,
                                    className = auth.user.className,
                                    schoolType = auth.user.schoolType,
                                    classTeacher = auth.user.classTeacher,
                                    avatarType = auth.user.avatarType,
                                    avatarColorHex = auth.user.avatarColorHex,
                                    hasCustomPassword = true,
                                    isTwoFactorEnabled = currentAccount.isTwoFactorEnabled
                                )

                                // Kurse synchronisieren / laden
                                BackendApiClient.fetchUserCourses().onSuccess { courses ->
                                    de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(courses)
                                }

                                de.kjltowinkler.msapp.data.schulportal.SchulportalClient.savedSchoolId = "4322"
                                de.kjltowinkler.msapp.data.schulportal.SchulportalClient.savedUsername = auth.user.username
                                de.kjltowinkler.msapp.data.schulportal.SchulportalClient.savedPassword = password

                                if (currentAccount.isTwoFactorEnabled) {
                                    navController.navigate(AppRoutes.LOGIN_READY)
                                } else {
                                    navController.navigate(AppRoutes.HOME) {
                                        popUpTo(initialRoute) { inclusive = true }
                                    }
                                }
                            }.onFailure { err ->
                                isLoggingIn = false
                                val msg = err.message ?: "Anmeldung fehlgeschlagen."
                                loginError = when {
                                    msg.contains("ConnectException") || msg.contains("UnresolvedAddressException") || msg.contains("Failed to connect") -> {
                                        "Verbindung zum Server fehlgeschlagen. Bitte prüfe deine Internetverbindung."
                                    }
                                    msg.contains("401") || msg.contains("falsch", ignoreCase = true) || msg.contains("ungültig", ignoreCase = true) -> {
                                        "Benutzername oder Passwort ist falsch."
                                    }
                                    else -> {
                                        msg.removePrefix("java.lang.IllegalStateException: ")
                                           .removePrefix("IllegalStateException: ")
                                    }
                                }
                            }
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
                            popUpTo(initialRoute) { inclusive = true }
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
                        BackendApiClient.logout()
                        de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(emptyList())
                        navController.navigate(initialRoute) {
                            popUpTo(0) { inclusive = true }
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
                    predictiveBackEnabled = isPredictiveBackEnabled,
                    onPredictiveBackChanged = { isPredictiveBackEnabled = it },
                    onSchoolWayClicked = {
                        navController.navigate(AppRoutes.SETTINGS_SCHOOL_WAY)
                    },
                    onCoursesClicked = {
                        navController.navigate(AppRoutes.SETTINGS_COURSES)
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

            // --- EINSTELLUNGEN: MEINE KURSE ---
            composable(AppRoutes.SETTINGS_COURSES) {
                de.kjltowinkler.msapp.ui.settings.CoursesSettingsScreen(
                    onBackClicked = {
                        navController.popBackStack()
                    },
                    useMaterialYou = isDynamicColor
                )
            }
        }
    }
}
}
}