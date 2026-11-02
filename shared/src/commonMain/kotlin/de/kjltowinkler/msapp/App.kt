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
import de.kjltowinkler.msapp.data.auth.SetInitialPasswordRequest
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
    val loginUsername: String = "sebastian.wagner",
    val email: String = "sebastian.wagner@mssof.onmicrosoft.com",
    val studentId: String = "#104829",
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
                studentId = "#102341",
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
                studentId = "#105582",
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
                studentId = "#100002",
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
                studentId = "#104829",
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
                studentId = "#104829",
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
        var usernameError by remember { mutableStateOf<String?>(null) }
        var isCheckingUsername by remember { mutableStateOf(false) }
        var oneTimePasswordError by remember { mutableStateOf<String?>(null) }
        var isCheckingOneTimePassword by remember { mutableStateOf(false) }
        var newPasswordError by remember { mutableStateOf<String?>(null) }
        var isSettingNewPassword by remember { mutableStateOf(false) }
        var initialPasswordEntered by remember { mutableStateOf("") }
        var isOnboardingFlow by remember { mutableStateOf(false) }
        var twoFactorTempToken by remember { mutableStateOf<String?>(null) }
        var isLoginTwoFactorVerification by remember { mutableStateOf(false) }
        var twoFactorSetupUri by remember { mutableStateOf<String?>(null) }
        var twoFactorBackupSecret by remember { mutableStateOf<String?>(null) }
        var isLoadingTwoFactorSetup by remember { mutableStateOf(false) }
        var isConfirmingTwoFactor by remember { mutableStateOf(false) }
        var twoFactorConfirmError by remember { mutableStateOf<String?>(null) }
        var lastEnteredPassword by remember { mutableStateOf("") }
        var isSettingUpTwoFactorFromSettings by remember { mutableStateOf(false) }

        // Aktueller Account-Status (wird bei bestehender Sitzung aus dem Speicher geladen)
        var currentAccount by remember {
            mutableStateOf(
                BackendApiClient.currentUser?.let { user ->
                    MockAccountState(
                        username = user.fullName.ifBlank { user.username },
                        loginUsername = user.username,
                        email = user.email,
                        studentId = user.studentId,
                        className = user.className,
                        schoolType = user.schoolType,
                        classTeacher = user.classTeacher,
                        avatarType = user.avatarType,
                        avatarColorHex = user.avatarColorHex,
                        hasCustomPassword = user.hasCustomPassword,
                        isTwoFactorEnabled = user.isTwoFactorEnabled
                    )
                } ?: MockAccountState()
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isDesktop = getPlatform().name.startsWith("Desktop") || maxWidth >= 840.dp
            val initialRoute = remember {
                if (BackendApiClient.isLoggedIn) {
                    if (BackendApiClient.currentUser?.hasCustomPassword == false) {
                        AppRoutes.NEW_PASSWORD
                    } else {
                        AppRoutes.HOME
                    }
                } else if (isDesktop) {
                    AppRoutes.USERNAME
                } else {
                    AppRoutes.WELCOME
                }
            }

            // Automatische Sitzungsüberprüfung beim Start & Update-Check
            LaunchedEffect(Unit) {
                // Prüft auf neue App-Updates und benachrichtigt bei neuer Version
                de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate()

                val pendingNav = de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("pending_navigate_to")
                if (pendingNav != null) {
                    de.kjltowinkler.msapp.data.storage.PlatformStorage.remove("pending_navigate_to")
                    if (pendingNav == "settings" && BackendApiClient.isLoggedIn) {
                        navController.navigate(AppRoutes.SETTINGS)
                    }
                }

                if (BackendApiClient.isLoggedIn) {
                    de.kjltowinkler.msapp.data.task.TasksRepository.syncWithServer()
                    de.kjltowinkler.msapp.data.task.TasksRepository.startRealtimeSync()
                    de.kjltowinkler.msapp.data.task.TaskNotificationManager.checkAndNotify()
                    BackendApiClient.fetchUserCourses().onSuccess { courses ->
                        de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(courses)
                    }
                    BackendApiClient.getProfile().onSuccess { user ->
                        currentAccount = MockAccountState(
                            username = user.fullName.ifBlank { user.username },
                            loginUsername = user.username,
                            email = user.email,
                            studentId = user.studentId,
                            className = user.className,
                            schoolType = user.schoolType,
                            classTeacher = user.classTeacher,
                            avatarType = user.avatarType,
                            avatarColorHex = user.avatarColorHex,
                            hasCustomPassword = user.hasCustomPassword,
                            isTwoFactorEnabled = user.isTwoFactorEnabled
                        )

                        // Falls der Account noch das Einmal-Passwort hat -> zwingend neues Passwort erstellen lassen!
                        if (!user.hasCustomPassword) {
                            isOnboardingFlow = true
                            navController.navigate(AppRoutes.NEW_PASSWORD) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }.onFailure { err ->
                        val msg = err.message ?: ""
                        if (msg.contains("401") || msg.contains("Nicht autorisiert", ignoreCase = true)) {
                            BackendApiClient.logout()
                            de.kjltowinkler.msapp.data.task.TasksRepository.clearAndReset()
                            de.kjltowinkler.msapp.data.schulportal.SchulportalClient.clearCredentials()
                            de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(emptyList())
                            val defaultRoute = if (isDesktop) AppRoutes.USERNAME else AppRoutes.WELCOME
                            navController.navigate(defaultRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }

            // Periodischer Aufgaben-Erinnerungs- und Überfälligkeits-Checker (jede Minute bei aktiver App)
            LaunchedEffect(Unit) {
                while (true) {
                    de.kjltowinkler.msapp.data.task.TaskNotificationManager.checkAndNotify()
                    kotlinx.coroutines.delay(60_000L)
                }
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
                val handleUsernameNext: (String) -> Unit = { enteredUsername ->
                    val clean = enteredUsername.trim()
                    if (clean.isNotBlank()) {
                        val hasOtp = clean.contains("otp", ignoreCase = true) ||
                                    clean.contains("einmal", ignoreCase = true)
                        val has2Fa = clean.contains("2fa", ignoreCase = true)

                        if (hasOtp) {
                            isOnboardingFlow = true
                            currentAccount = createMockAccount(
                                enteredUsername = clean,
                                hasCustomPassword = false,
                                isTwoFactorEnabled = has2Fa
                            )
                            usernameError = null
                            navController.navigate(AppRoutes.ONE_TIME_PASSWORD)
                        } else {
                            isCheckingUsername = true
                            usernameError = null

                            coroutineScope.launch {
                                BackendApiClient.checkAccount(clean)
                                    .onSuccess { response ->
                                        isCheckingUsername = false
                                        if (response.exists) {
                                            usernameError = null
                                            currentAccount = MockAccountState(
                                                username = response.fullName?.ifBlank { response.username } ?: clean,
                                                loginUsername = response.username ?: clean,
                                                email = response.email ?: clean,
                                                studentId = response.studentId ?: "",
                                                className = response.className ?: "9aR",
                                                schoolType = response.schoolType ?: "Realschule",
                                                classTeacher = response.classTeacher ?: "Hr. Thomas",
                                                avatarType = response.avatarType ?: "initials",
                                                avatarColorHex = response.avatarColorHex ?: 0xFF395A3CL,
                                                hasCustomPassword = response.hasCustomPassword,
                                                isTwoFactorEnabled = response.isTwoFactorEnabled
                                            )
                                            loginError = null
                                            oneTimePasswordError = null
                                            newPasswordError = null

                                            if (!response.hasCustomPassword) {
                                                isOnboardingFlow = true
                                                navController.navigate(AppRoutes.ONE_TIME_PASSWORD)
                                            } else {
                                                isOnboardingFlow = false
                                                navController.navigate(AppRoutes.LOGIN_PASSWORD)
                                            }
                                        } else {
                                            usernameError = "Dieser Account existiert nicht. Bitte überprüfe deine Eingabe."
                                        }
                                    }
                                    .onFailure { err ->
                                        isCheckingUsername = false
                                        val msg = err.message ?: ""
                                        usernameError = when {
                                            msg.contains("ConnectException") || msg.contains("UnresolvedAddressException") || msg.contains("Failed to connect") -> {
                                                "Verbindung zum Server fehlgeschlagen. Bitte prüfe deine Internetverbindung."
                                            }
                                            else -> {
                                                "Dieser Account existiert nicht. Bitte überprüfe deine Eingabe."
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }

                composable(AppRoutes.WELCOME) {
                    if (isDesktop) {
                        OnboardingUsernameScreen(
                            isLoading = isCheckingUsername,
                            errorMessage = usernameError,
                            onBackClicked = {
                                usernameError = null
                            },
                            onNextClicked = handleUsernameNext
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
                        isLoading = isCheckingUsername,
                        errorMessage = usernameError,
                        onBackClicked = {
                            usernameError = null
                            if (!isDesktop) {
                                navController.popBackStack()
                            }
                        },
                        onNextClicked = handleUsernameNext
                    )
                }
            
            // --- PFAD A: ONBOARDING / ERSTEINRICHTUNG (Einmalpasswort) ---
            composable(AppRoutes.ONE_TIME_PASSWORD) {
                OnboardingOneTimePasswordScreen(
                    accountName = currentAccount.username,
                    isLoading = isCheckingOneTimePassword,
                    errorMessage = oneTimePasswordError,
                    onBackClicked = {
                        oneTimePasswordError = null
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        oneTimePasswordError = null
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onNextClicked = { oneTimePassword ->
                        oneTimePasswordError = null
                        isCheckingOneTimePassword = true

                        val identifier = when {
                            currentAccount.loginUsername.isNotBlank() -> currentAccount.loginUsername
                            currentAccount.email.isNotBlank() && currentAccount.email.contains("@") -> currentAccount.email
                            else -> currentAccount.username
                        }

                        coroutineScope.launch {
                            val result = BackendApiClient.login(LoginRequest(usernameOrEmail = identifier, password = oneTimePassword))
                                .recoverCatching {
                                    val fallbackIdentifier = if (identifier == currentAccount.loginUsername && currentAccount.email.contains("@")) {
                                        currentAccount.email
                                    } else {
                                        currentAccount.username
                                    }
                                    if (fallbackIdentifier != identifier && fallbackIdentifier.isNotBlank()) {
                                        BackendApiClient.login(LoginRequest(usernameOrEmail = fallbackIdentifier, password = oneTimePassword)).getOrThrow()
                                    } else {
                                        throw it
                                    }
                                }

                            result.onSuccess {
                                isCheckingOneTimePassword = false
                                oneTimePasswordError = null
                                initialPasswordEntered = oneTimePassword
                                navController.navigate(AppRoutes.NEW_PASSWORD)
                            }.onFailure { err ->
                                isCheckingOneTimePassword = false
                                val msg = err.message ?: ""
                                oneTimePasswordError = when {
                                    msg.contains("ConnectException") || msg.contains("UnresolvedAddressException") || msg.contains("Failed to connect") -> {
                                        "Verbindung zum Server fehlgeschlagen. Bitte prüfe deine Internetverbindung."
                                    }
                                    else -> {
                                        "Das Einmal-Passwort ist nicht korrekt."
                                    }
                                }
                            }
                        }
                    }
                )
            }
            composable(AppRoutes.NEW_PASSWORD) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingNewPasswordScreen(
                    accountName = currentAccount.username,
                    isLoading = isSettingNewPassword,
                    errorMessage = newPasswordError,
                    onBackClicked = {
                        newPasswordError = null
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        newPasswordError = null
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onFinishClicked = { newPassword ->
                        newPasswordError = null
                        isSettingNewPassword = true

                        val identifier = when {
                            currentAccount.loginUsername.isNotBlank() -> currentAccount.loginUsername
                            currentAccount.email.isNotBlank() && currentAccount.email.contains("@") -> currentAccount.email
                            else -> currentAccount.username
                        }

                        coroutineScope.launch {
                            val result = BackendApiClient.setInitialPassword(
                                SetInitialPasswordRequest(
                                    usernameOrEmail = identifier,
                                    initialPassword = initialPasswordEntered,
                                    newPassword = newPassword
                                )
                            ).recoverCatching {
                                val fallbackIdentifier = if (identifier == currentAccount.loginUsername && currentAccount.email.contains("@")) {
                                    currentAccount.email
                                } else {
                                    currentAccount.username
                                }
                                if (fallbackIdentifier != identifier && fallbackIdentifier.isNotBlank()) {
                                    BackendApiClient.setInitialPassword(
                                        SetInitialPasswordRequest(
                                            usernameOrEmail = fallbackIdentifier,
                                            initialPassword = initialPasswordEntered,
                                            newPassword = newPassword
                                        )
                                    ).getOrThrow()
                                } else {
                                    throw it
                                }
                            }

                            result.onSuccess { auth ->
                                isSettingNewPassword = false
                                newPasswordError = null

                                currentAccount = MockAccountState(
                                    username = auth.user.fullName.ifBlank { auth.user.username },
                                    loginUsername = auth.user.username,
                                    email = auth.user.email,
                                    className = auth.user.className,
                                    schoolType = auth.user.schoolType,
                                    classTeacher = auth.user.classTeacher,
                                    avatarType = auth.user.avatarType,
                                    avatarColorHex = auth.user.avatarColorHex,
                                    hasCustomPassword = true,
                                    isTwoFactorEnabled = auth.user.isTwoFactorEnabled
                                )

                                // Kurse synchronisieren / laden
                                BackendApiClient.fetchUserCourses().onSuccess { courses ->
                                    de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(courses)
                                }
                                de.kjltowinkler.msapp.data.task.TasksRepository.syncWithServer()
                                de.kjltowinkler.msapp.data.task.TasksRepository.startRealtimeSync()

                                de.kjltowinkler.msapp.data.schulportal.SchulportalClient.saveCredentials("4322", auth.user.username, newPassword)

                                // Bei 2FA prüfen: Hat der Account es an?
                                // ja = weiter zum Fertig Screen, nein = weiter zum Fragen ob 2FA aktiviert werden soll
                                if (auth.user.isTwoFactorEnabled) {
                                    navController.navigate(AppRoutes.READY)
                                } else {
                                    navController.navigate(AppRoutes.TWO_FACTOR_AUTH)
                                }
                            }.onFailure { err ->
                                isSettingNewPassword = false
                                val msg = err.message ?: "Fehler beim Setzen des Passworts."
                                newPasswordError = when {
                                    msg.contains("ConnectException") || msg.contains("UnresolvedAddressException") || msg.contains("Failed to connect") -> {
                                        "Verbindung zum Server fehlgeschlagen. Bitte prüfe deine Internetverbindung."
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
                        // 2FA abgelehnt ("Nein danke") -> weiter zum entsprechenden Ready Screen
                        val targetRoute = if (isOnboardingFlow) AppRoutes.READY else AppRoutes.LOGIN_READY
                        navController.navigate(targetRoute)
                    },
                    onNextClicked = {
                        // 2FA einrichten ("Weiter") -> QR-Code / Connect Screen
                        navController.navigate(AppRoutes.TWO_FACTOR_CONNECT)
                    }
                )
            }
            composable(AppRoutes.TWO_FACTOR_CONNECT) {
                LaunchedEffect(Unit) {
                    if (twoFactorSetupUri.isNullOrBlank()) {
                        isLoadingTwoFactorSetup = true
                        BackendApiClient.setup2Fa().onSuccess { setup ->
                            twoFactorSetupUri = setup.otpauthUri
                            twoFactorBackupSecret = setup.secret
                            isLoadingTwoFactorSetup = false
                        }.onFailure {
                            isLoadingTwoFactorSetup = false
                        }
                    }
                }

                de.kjltowinkler.msapp.ui.onboarding.OnboardingTwoFactorConnectScreen(
                    accountName = currentAccount.username,
                    totpUri = twoFactorSetupUri ?: "",
                    backupCode = twoFactorBackupSecret ?: "",
                    isLoading = isLoadingTwoFactorSetup || twoFactorSetupUri.isNullOrBlank(),
                    showAccountBadge = !isSettingUpTwoFactorFromSettings,
                    onBackClicked = {
                        isSettingUpTwoFactorFromSettings = false
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        isSettingUpTwoFactorFromSettings = false
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onNextClicked = {
                        isLoginTwoFactorVerification = false
                        twoFactorConfirmError = null
                        navController.navigate(AppRoutes.TWO_FACTOR_CONFIRM)
                    }
                )
            }
            composable(AppRoutes.TWO_FACTOR_CONFIRM) {
                de.kjltowinkler.msapp.ui.onboarding.OnboardingTwoFactorConfirmScreen(
                    accountName = currentAccount.username,
                    isLoading = isConfirmingTwoFactor,
                    errorMessage = twoFactorConfirmError,
                    showAccountBadge = !isSettingUpTwoFactorFromSettings,
                    onBackClicked = {
                        twoFactorConfirmError = null
                        navController.popBackStack()
                    },
                    onChangeAccountClicked = {
                        isSettingUpTwoFactorFromSettings = false
                        twoFactorConfirmError = null
                        navController.popBackStack(AppRoutes.USERNAME, inclusive = false)
                    },
                    onFinishClicked = { rawCode ->
                        val cleanCode = rawCode.filter { it.isDigit() }
                        twoFactorConfirmError = null
                        isConfirmingTwoFactor = true

                        if (isLoginTwoFactorVerification) {
                            val tempToken = twoFactorTempToken ?: ""
                            coroutineScope.launch {
                                BackendApiClient.verify2FaLogin(tempToken, cleanCode)
                                    .onSuccess { auth ->
                                        isConfirmingTwoFactor = false
                                        currentAccount = MockAccountState(
                                            username = auth.user.fullName.ifBlank { auth.user.username },
                                            loginUsername = auth.user.username,
                                            email = auth.user.email,
                                            studentId = auth.user.studentId,
                                            className = auth.user.className,
                                            schoolType = auth.user.schoolType,
                                            classTeacher = auth.user.classTeacher,
                                            avatarType = auth.user.avatarType,
                                            avatarColorHex = auth.user.avatarColorHex,
                                            hasCustomPassword = auth.user.hasCustomPassword,
                                            isTwoFactorEnabled = auth.user.isTwoFactorEnabled
                                        )

                                        BackendApiClient.fetchUserCourses().onSuccess { courses ->
                                            de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(courses)
                                        }
                                        de.kjltowinkler.msapp.data.task.TasksRepository.syncWithServer()
                                        de.kjltowinkler.msapp.data.task.TasksRepository.startRealtimeSync()

                                        if (lastEnteredPassword.isNotBlank()) {
                                            de.kjltowinkler.msapp.data.schulportal.SchulportalClient.saveCredentials("4322", auth.user.username, lastEnteredPassword)
                                        }

                                        navController.navigate(AppRoutes.LOGIN_READY)
                                    }
                                    .onFailure { err ->
                                        isConfirmingTwoFactor = false
                                        val msg = err.message ?: ""
                                        twoFactorConfirmError = when {
                                            msg.contains("401") || msg.contains("falsch", ignoreCase = true) || msg.contains("ungültig", ignoreCase = true) -> {
                                                "Der Bestätigungscode ist falsch oder abgelaufen."
                                            }
                                            msg.contains("abgelaufen", ignoreCase = true) -> {
                                                "Die Anmeldesitzung ist abgelaufen. Bitte melde dich erneut an."
                                            }
                                            else -> {
                                                "Bestätigung fehlgeschlagen. Bitte prüfe deinen Code."
                                            }
                                        }
                                    }
                            }
                        } else {
                            coroutineScope.launch {
                                BackendApiClient.confirm2Fa(cleanCode)
                                    .onSuccess {
                                        isConfirmingTwoFactor = false
                                        currentAccount = currentAccount.copy(isTwoFactorEnabled = true)
                                        if (isSettingUpTwoFactorFromSettings) {
                                            isSettingUpTwoFactorFromSettings = false
                                            navController.popBackStack(AppRoutes.SETTINGS, inclusive = false)
                                        } else {
                                            val targetRoute = if (isOnboardingFlow) AppRoutes.READY else AppRoutes.LOGIN_READY
                                            navController.navigate(targetRoute)
                                        }
                                    }
                                    .onFailure { err ->
                                        isConfirmingTwoFactor = false
                                        val msg = err.message ?: ""
                                        twoFactorConfirmError = when {
                                            msg.contains("401") || msg.contains("falsch", ignoreCase = true) || msg.contains("ungültig", ignoreCase = true) -> {
                                                "Der Bestätigungscode ist falsch. Bitte gib den aktuellen 6-stelligen Code ein."
                                            }
                                            else -> {
                                                "Bestätigung fehlgeschlagen. Bitte prüfe deinen Code."
                                            }
                                        }
                                    }
                            }
                        }
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

                        val identifier = when {
                            currentAccount.loginUsername.isNotBlank() -> currentAccount.loginUsername
                            currentAccount.email.isNotBlank() && currentAccount.email.contains("@") -> currentAccount.email
                            else -> currentAccount.username
                        }

                        coroutineScope.launch {
                            val result = BackendApiClient.login(LoginRequest(usernameOrEmail = identifier, password = password))
                                .recoverCatching {
                                    val fallbackIdentifier = if (identifier == currentAccount.loginUsername && currentAccount.email.contains("@")) {
                                        currentAccount.email
                                    } else {
                                        currentAccount.username
                                    }
                                    if (fallbackIdentifier != identifier && fallbackIdentifier.isNotBlank()) {
                                        BackendApiClient.login(LoginRequest(usernameOrEmail = fallbackIdentifier, password = password)).getOrThrow()
                                    } else {
                                        throw it
                                    }
                                }

                            result.onSuccess { auth ->
                                isLoggingIn = false
                                loginError = null
                                lastEnteredPassword = password

                                currentAccount = MockAccountState(
                                    username = auth.user.fullName.ifBlank { auth.user.username },
                                    loginUsername = auth.user.username,
                                    email = auth.user.email,
                                    studentId = auth.user.studentId,
                                    className = auth.user.className,
                                    schoolType = auth.user.schoolType,
                                    classTeacher = auth.user.classTeacher,
                                    avatarType = auth.user.avatarType,
                                    avatarColorHex = auth.user.avatarColorHex,
                                    hasCustomPassword = auth.user.hasCustomPassword,
                                    isTwoFactorEnabled = auth.user.isTwoFactorEnabled
                                )

                                if (auth.requiresTwoFactor && !auth.tempToken.isNullOrBlank()) {
                                    twoFactorTempToken = auth.tempToken
                                    isLoginTwoFactorVerification = true
                                    twoFactorConfirmError = null
                                    navController.navigate(AppRoutes.TWO_FACTOR_CONFIRM)
                                } else {
                                    // Kurse synchronisieren / laden
                                    BackendApiClient.fetchUserCourses().onSuccess { courses ->
                                        de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(courses)
                                    }
                                    de.kjltowinkler.msapp.data.task.TasksRepository.syncWithServer()
                                    de.kjltowinkler.msapp.data.task.TasksRepository.startRealtimeSync()

                                    de.kjltowinkler.msapp.data.schulportal.SchulportalClient.saveCredentials("4322", auth.user.username, password)

                                    // Wenn der Nutzer noch kein eigenes Passwort hat -> zwingend zur Passwort-Erstellung weiterleiten!
                                    if (!auth.user.hasCustomPassword) {
                                        initialPasswordEntered = password
                                        isOnboardingFlow = true
                                        navController.navigate(AppRoutes.NEW_PASSWORD)
                                    } else if (auth.user.isTwoFactorEnabled) {
                                        navController.navigate(AppRoutes.LOGIN_READY)
                                    } else {
                                        navController.navigate(AppRoutes.TWO_FACTOR_AUTH)
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
                    studentId = currentAccount.studentId,
                    avatarType = currentAccount.avatarType,
                    avatarColorHex = currentAccount.avatarColorHex,
                    onAvatarChanged = { type, colorHex ->
                        currentAccount = currentAccount.copy(
                            avatarType = type,
                            avatarColorHex = colorHex
                        )
                        coroutineScope.launch {
                            BackendApiClient.updateProfile(
                                de.kjltowinkler.msapp.data.auth.UpdateProfileRequest(
                                    avatarType = type,
                                    avatarColorHex = colorHex
                                )
                            )
                        }
                    },
                    onSettingsClicked = {
                        navController.navigate(AppRoutes.SETTINGS)
                    },
                    onLogoutClicked = {
                        BackendApiClient.logout()
                        twoFactorTempToken = null
                        twoFactorSetupUri = null
                        twoFactorBackupSecret = null
                        twoFactorConfirmError = null
                        isLoginTwoFactorVerification = false
                        lastEnteredPassword = ""
                        de.kjltowinkler.msapp.data.task.TasksRepository.clearAndReset()
                        de.kjltowinkler.msapp.data.schulportal.SchulportalClient.clearCredentials()
                        de.kjltowinkler.msapp.data.courses.CourseRepository.setCourses(emptyList())
                        val defaultRoute = if (isDesktop) AppRoutes.USERNAME else AppRoutes.WELCOME
                        navController.navigate(defaultRoute) {
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
                    studentId = currentAccount.studentId,
                    accountName = currentAccount.username,
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
                    },
                    isTwoFactorEnabled = currentAccount.isTwoFactorEnabled,
                    onNavigateTo2FaSetup = {
                        isSettingUpTwoFactorFromSettings = true
                        twoFactorSetupUri = null
                        navController.navigate(AppRoutes.TWO_FACTOR_CONNECT)
                    },
                    onDisable2Fa = {
                        coroutineScope.launch {
                            BackendApiClient.disable2Fa().onSuccess {
                                currentAccount = currentAccount.copy(isTwoFactorEnabled = false)
                            }
                        }
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