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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Multiplatform Backend API Client für die Verbindung mit dem Ktor-Server.
 */
object BackendApiClient {

    var serverUrl: String = "http://localhost:8080"
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
            authToken = auth.token
            currentUser = auth.user
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error("Registrierung fehlgeschlagen ($error)")
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
            authToken = auth.token
            currentUser = auth.user
            auth
        } else {
            val error = runCatching { json.decodeFromString<SimpleApiResponse>(body).message }.getOrDefault(body)
            error("Anmeldung fehlgeschlagen ($error)")
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
            user
        } else {
            error("Profil-Update fehlgeschlagen")
        }
    }

    fun logout() {
        authToken = null
        currentUser = null
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
