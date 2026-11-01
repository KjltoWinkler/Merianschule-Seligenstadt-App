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
        authToken = null
        currentUser = null
        PlatformStorage.remove(KEY_AUTH_TOKEN)
        PlatformStorage.remove(KEY_CURRENT_USER)
    }

    private val client = HttpClient(CIO)

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
            saveSession(auth.token, auth.user)
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
            saveSession(auth.token, auth.user)
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error("Anmeldung fehlgeschlagen ($error)")
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
            saveSession(auth.token, auth.user)
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error(error)
        }
    }

    suspend fun update2Fa(enabled: Boolean): Result<Boolean> = runCatching {
        val token = authToken ?: error("Nicht angemeldet")
        val response = client.post("$serverUrl/api/auth/2fa") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(Update2FaRequest(enabled)))
        }
        val success = response.status == HttpStatusCode.OK
        if (success) {
            currentUser?.let { user ->
                val updated = user.copy(isTwoFactorEnabled = enabled)
                currentUser = updated
                PlatformStorage.setString(KEY_CURRENT_USER, json.encodeToString(updated))
            }
        }
        success
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

    suspend fun syncTasks(tasks: List<TaskItem>, lists: List<TaskList>): Result<SyncTasksResponse> = runCatching {
        val token = authToken ?: error("Für die Synchronisation ist eine Anmeldung erforderlich.")
        val request = SyncTasksRequest(tasks = tasks, lists = lists)

        val response = client.post("$serverUrl/api/tasks/sync") {
            header("Authorization", "Bearer $token")
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
