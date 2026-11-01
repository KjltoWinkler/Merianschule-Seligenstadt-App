package de.kjltowinkler.msapp.data.backend

import de.kjltowinkler.msapp.data.auth.*
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.sync.*
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TaskList
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Multiplatform Backend API Client für die Verbindung mit dem Ktor-Server.
 */
object BackendApiClient {

    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_CURRENT_USER = "current_user"

    var serverUrl: String = "https://api.veloschulplaner.de"
        set(value) {
            field = value.trimEnd('/')
        }


    var authToken: String? = null
    var currentUser: UserProfileDto? = null

    val isLoggedIn: Boolean
        get() = !authToken.isNullOrBlank()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    init {
        loadSavedSession()
    }

    fun loadSavedSession() {
        runCatching {
            val token = PlatformStorage.getString(KEY_AUTH_TOKEN)
            val userJson = PlatformStorage.getString(KEY_CURRENT_USER)
            if (!token.isNullOrBlank()) {
                authToken = token
            }
            if (!userJson.isNullOrBlank()) {
                currentUser = json.decodeFromString<UserProfileDto>(userJson)
            }
        }
    }

    fun saveSession(token: String, user: UserProfileDto) {
        authToken = token
        currentUser = user
        PlatformStorage.setString(KEY_AUTH_TOKEN, token)
        PlatformStorage.setString(KEY_CURRENT_USER, json.encodeToString(user))
    }

    fun clearSession() {
        stopRealtimeTaskUpdates()
        authToken = null
        currentUser = null
        PlatformStorage.remove(KEY_AUTH_TOKEN)
        PlatformStorage.remove(KEY_CURRENT_USER)
    }

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    val clientInstanceId: String = "inst_" + (100000..999999).random()

    private val clientScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var realTimeJob: Job? = null

    /**
     * Startet die permanente WebSocket-Verbindung für Aufgaben-Echtzeitaktualisierungen.
     * Sobald ein anderes Gerät eine Änderung vornimmt, wird [onUpdate] ausgeführt.
     */
    fun startRealtimeTaskUpdates(onUpdate: suspend () -> Unit) {
        if (!isLoggedIn) return
        realTimeJob?.cancel()
        realTimeJob = clientScope.launch {
            while (isActive && isLoggedIn) {
                try {
                    val token = authToken ?: break
                    val wsScheme = if (serverUrl.startsWith("https://")) "wss://" else "ws://"
                    val cleanHost = serverUrl.removePrefix("https://").removePrefix("http://")
                    val wsUrl = "$wsScheme$cleanHost/api/tasks/ws?token=$token"

                    client.webSocket(wsUrl) {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                if (text.contains("tasks_updated", ignoreCase = true)) {
                                    val isOwnEcho = text.contains("\"origin\":\"$clientInstanceId\"")
                                    if (!isOwnEcho) {
                                        onUpdate()
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Automatische Wiederverbindung bei Verbindungsverlust (z.B. Wechsel WLAN/Mobilfunk)
                }
                delay(3000)
            }
        }
    }

    fun stopRealtimeTaskUpdates() {
        realTimeJob?.cancel()
        realTimeJob = null
    }

    // ==========================================
    // AUTH & ACCOUNT
    // ==========================================

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = runCatching {
        val response = client.post("$serverUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.Created) {
            val auth = json.decodeFromString<AuthResponse>(body)
            if (!auth.token.isNullOrBlank()) {
                saveSession(auth.token, auth.user)
            }
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error("Registrierung fehlgeschlagen ($error)")
        }
    }

    suspend fun checkAccount(usernameOrEmail: String): Result<CheckAccountResponse> = runCatching {
        val response = client.post("$serverUrl/api/auth/check") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CheckAccountRequest(usernameOrEmail.trim())))
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<CheckAccountResponse>(body)
        } else if (response.status == HttpStatusCode.NotFound) {
            val res = runCatching { json.decodeFromString<CheckAccountResponse>(body) }
                .getOrElse { CheckAccountResponse(exists = false, message = "Account nicht gefunden.") }
            res
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error("Überprüfung fehlgeschlagen ($error)")
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        val response = client.post("$serverUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val auth = json.decodeFromString<AuthResponse>(body)
            if (auth.token != null && auth.user != null) {
                saveSession(auth.token, auth.user)
            }
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun setInitialPassword(request: SetInitialPasswordRequest): Result<AuthResponse> = runCatching {
        val response = client.post("$serverUrl/api/auth/set-initial-password") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val auth = json.decodeFromString<AuthResponse>(body)
            if (auth.token != null && auth.user != null) {
                saveSession(auth.token, auth.user)
            }
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun setup2Fa(): Result<TwoFactorSetupResponse> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.post("$serverUrl/api/auth/2fa/setup") {
            header("Authorization", "Bearer $token")
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<TwoFactorSetupResponse>(body)
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun confirm2Fa(code: String): Result<Boolean> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.post("$serverUrl/api/auth/2fa/confirm") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ConfirmTwoFactorRequest(code)))
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            currentUser?.let { user ->
                val updated = user.copy(isTwoFactorEnabled = true)
                currentUser = updated
                PlatformStorage.setString(KEY_CURRENT_USER, json.encodeToString(updated))
            }
            true
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun disable2Fa(): Result<Boolean> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.post("$serverUrl/api/auth/2fa/disable") {
            header("Authorization", "Bearer $token")
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            currentUser?.let { user ->
                val updated = user.copy(isTwoFactorEnabled = false)
                currentUser = updated
                PlatformStorage.setString(KEY_CURRENT_USER, json.encodeToString(updated))
            }
            true
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun verify2FaLogin(tempToken: String, code: String): Result<AuthResponse> = runCatching {
        val response = client.post("$serverUrl/api/auth/2fa/verify-login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TwoFactorLoginRequest(tempToken = tempToken, code = code)))
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val auth = json.decodeFromString<AuthResponse>(body)
            if (auth.token != null && auth.user != null) {
                saveSession(auth.token, auth.user)
            }
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun update2Fa(enabled: Boolean): Result<Boolean> = runCatching {
        if (enabled) {
            error("Bitte confirm2Fa() für die Einrichtung mit Authenticator-Code verwenden.")
        } else {
            disable2Fa().getOrThrow()
        }
    }

    suspend fun getProfile(): Result<UserProfileDto> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.get("$serverUrl/api/auth/me") {
            header("Authorization", "Bearer $token")
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val user = json.decodeFromString<UserProfileDto>(body)
            currentUser = user
            PlatformStorage.setString(KEY_CURRENT_USER, json.encodeToString(user))
            user
        } else {
            error("Profil konnte nicht geladen werden")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<UserProfileDto> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.put("$serverUrl/api/auth/profile") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val user = json.decodeFromString<UserProfileDto>(body)
            currentUser = user
            PlatformStorage.setString(KEY_CURRENT_USER, json.encodeToString(user))
            user
        } else {
            error("Profil-Update fehlgeschlagen")
        }
    }

    fun logout() {
        clearSession()
    }

    // ==========================================
    // KURSE & FÄCHER
    // ==========================================

    suspend fun fetchUserCourses(): Result<List<de.kjltowinkler.msapp.data.courses.UserCourse>> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.get("$serverUrl/api/user/courses") {
            header("Authorization", "Bearer $token")
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<List<de.kjltowinkler.msapp.data.courses.UserCourse>>(body)
        } else {
            error("Kurse konnten nicht geladen werden")
        }
    }

    suspend fun syncUserCourses(courses: List<de.kjltowinkler.msapp.data.courses.UserCourse>): Result<Boolean> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.put("$serverUrl/api/user/courses") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(courses))
        }
        response.status == HttpStatusCode.OK
    }

    // ==========================================
    // AUFGABEN-SYNCHRONISATION
    // ==========================================

    suspend fun syncTasks(
        tasks: List<TaskItem>,
        lists: List<TaskList>,
        deletedTaskIds: List<String> = emptyList(),
        deletedListIds: List<String> = emptyList()
    ): Result<SyncTasksResponse> = runCatching {
        val token = authToken ?: error("Für die Synchronisation ist eine Anmeldung erforderlich.")
        val request = SyncTasksRequest(
            tasks = tasks,
            lists = lists,
            deletedTaskIds = deletedTaskIds,
            deletedListIds = deletedListIds
        )

        val response = client.post("$serverUrl/api/tasks/sync") {
            header("Authorization", "Bearer $token")
            header("X-Client-Instance-Id", clientInstanceId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<SyncTasksResponse>(body)
        } else {
            error("Aufgaben-Sync fehlgeschlagen: $body")
        }
    }

    suspend fun fetchTasks(): Result<List<TaskItem>> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.get("$serverUrl/api/tasks") {
            header("Authorization", "Bearer $token")
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<List<TaskItem>>(body)
        } else {
            error("Aufgaben konnten nicht geladen werden")
        }
    }

    suspend fun fetchTaskLists(): Result<List<TaskList>> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.get("$serverUrl/api/tasks/lists") {
            header("Authorization", "Bearer $token")
        }
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<List<TaskList>>(body)
        } else {
            error("Listen konnten nicht geladen werden")
        }
    }

    var lastUpdatedFromServer: String? = null

    // ==========================================
    // STUNDENPLAN & VERTRETUNGSPLAN
    // ==========================================

    suspend fun fetchSubstitutions(className: String? = null, forceRefresh: Boolean = false): Result<List<SubstitutionEntry>> = runCatching {
        val params = mutableListOf<String>()
        if (!className.isNullOrBlank()) params.add("class=${className.trim()}")
        if (forceRefresh) params.add("forceRefresh=true")

        val queryString = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        val url = "$serverUrl/api/substitutions$queryString"

        val response = client.get(url)
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            val lastUpdated = response.headers["X-Last-Updated"]
            if (!lastUpdated.isNullOrBlank()) {
                lastUpdatedFromServer = lastUpdated
            }
            json.decodeFromString<List<SubstitutionEntry>>(body)
        } else {
            error("Vertretungsplan konnte nicht geladen werden: $body")
        }
    }

    suspend fun fetchTimetable(className: String = "09AR"): Result<ClassTimetableDto> = runCatching {
        val response = client.get("$serverUrl/api/timetable?class=${className.trim()}")
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<ClassTimetableDto>(body)
        } else {
            error("Stundenplan konnte nicht geladen werden: $body")
        }
    }

    // ==========================================
    // ADMIN ENDPOINTS
    // ==========================================

    suspend fun updateSubstitutionsAsAdmin(rawJsonOrEntries: String, adminKey: String): Result<AdminStatusResponse> = runCatching {
        val response = client.post("$serverUrl/api/admin/substitutions") {
            header("X-Admin-Key", adminKey)
            contentType(ContentType.Application.Json)
            setBody(rawJsonOrEntries)
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<AdminStatusResponse>(body)
        } else {
            error("Admin-Update fehlgeschlagen: $body")
        }
    }

    suspend fun updateTimetableAsAdmin(classTimetableJson: String, adminKey: String): Result<AdminStatusResponse> = runCatching {
        val response = client.post("$serverUrl/api/admin/timetable") {
            header("X-Admin-Key", adminKey)
            contentType(ContentType.Application.Json)
            setBody(classTimetableJson)
        }

        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<AdminStatusResponse>(body)
        } else {
            error("Admin-Stundenplan-Update fehlgeschlagen: $body")
        }
    }
}
