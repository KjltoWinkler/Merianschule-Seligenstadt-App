package de.kjltowinkler.msapp.data.schulportal

import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters

/**
 * Client für das Schulportal Hessen (LANIS).
 * Führt die 3-stufige SSO-Authentifizierung durch und lädt den aktuellen Vertretungsplan.
 */
object SchulportalClient {

    private const val DEFAULT_SCHOOL_ID = "4322"
    private const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // Speichert Cookies sitzungsübergreifend
    private val cookieJar = mutableMapOf<String, String>()

    // Gespeicherte Anmeldedaten für automatisches Neuladen
    var savedUsername: String = ""
    var savedPassword: String = ""
    var savedSchoolId: String = DEFAULT_SCHOOL_ID

    // Zuletzt vom Schulportal extrahierter Aktualisierungszeitstempel
    var lastUpdated: String? = null

    /**
     * Prüft, ob Anmeldedaten hinterlegt sind.
     */
    val hasCredentials: Boolean
        get() = savedUsername.isNotBlank() && savedPassword.isNotBlank()

    /**
     * Erstellt einen neuen HttpClient ohne automatische Redirects, um Location-Header und Set-Cookie manuell zu steuern.
     */
    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            followRedirects = false
        }
    }

    /**
     * Führt den SSO-Login durch und speichert die Session-Cookies.
     */
    suspend fun login(
        schoolId: String = savedSchoolId,
        username: String = savedUsername,
        password: String = savedPassword
    ): Result<Boolean> {
        val client = createClient()
        try {
            cookieJar.clear()

            val cleanUsername = username.trim()
            val fullUser = if (cleanUsername.startsWith("$schoolId.")) cleanUsername else "$schoolId.$cleanUsername"
            val user2 = if (cleanUsername.startsWith("$schoolId.")) cleanUsername.removePrefix("$schoolId.") else cleanUsername

            // Schritt 1: Login POST an login.schulportal.hessen.de
            val loginUrl = "https://login.schulportal.hessen.de/?i=$schoolId"
            val response1 = client.submitForm(
                url = loginUrl,
                formParameters = parameters {
                    append("url", "")
                    append("timezone", "")
                    append("skin", "sp")
                    append("user", fullUser)
                    append("user2", user2)
                    append("password", password)
                }
            ) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.AcceptLanguage, "de-DE,de;q=0.9,en;q=0.8")
            }

            extractCookies(response1.headers.getAll(HttpHeaders.SetCookie))

            if (response1.status != HttpStatusCode.Found && response1.status != HttpStatusCode.MovedPermanently && response1.status != HttpStatusCode.SeeOther) {
                val errorBody = response1.bodyAsText()
                val message = if (errorBody.contains("authErrorLocktime")) {
                    "Temporäre Anmeldesperre aktiv. Bitte 30 Sekunden warten."
                } else if (errorBody.contains("falsch", ignoreCase = true) || errorBody.contains("ungültig", ignoreCase = true)) {
                    "Benutzername oder Passwort ist falsch."
                } else {
                    "Anmeldung fehlgeschlagen (HTTP ${response1.status.value})."
                }
                return Result.failure(Exception(message))
            }

            // Schritt 2: SSO-Handshake über connect.schulportal.hessen.de
            val response2 = client.get("https://connect.schulportal.hessen.de") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Cookie, formatCookieHeader())
            }

            extractCookies(response2.headers.getAll(HttpHeaders.SetCookie))

            val targetUrl = response2.headers[HttpHeaders.Location]
                ?: "https://start.schulportal.hessen.de/"

            // Schritt 3: Session im Hauptportal aktivieren
            val response3 = client.get(targetUrl) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Cookie, formatCookieHeader())
            }

            extractCookies(response3.headers.getAll(HttpHeaders.SetCookie))

            // Gespeicherte Daten sichern
            savedSchoolId = schoolId
            savedUsername = username
            savedPassword = password

            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            client.close()
        }
    }

    /**
     * Ruft den aktuellen Vertretungsplan ab und parst ihn in eine Liste von [SubstitutionEntry].
     */
    suspend fun fetchSubstitutionPlan(): Result<List<SubstitutionEntry>> {
        val client = createClient()
        try {
            // Falls noch keine Session vorliegt, zuerst einloggen
            if (cookieJar.isEmpty()) {
                if (!hasCredentials) {
                    return Result.failure(Exception("Keine Anmeldedaten hinterlegt."))
                }
                val loginResult = login()
                if (loginResult.isFailure) {
                    return Result.failure(loginResult.exceptionOrNull() ?: Exception("Login fehlgeschlagen"))
                }
            }

            val vplanUrl = "https://start.schulportal.hessen.de/vertretungsplan.php"
            val response = client.get(vplanUrl) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Cookie, formatCookieHeader())
            }

            extractCookies(response.headers.getAll(HttpHeaders.SetCookie))

            // Falls Session abgelaufen war (z. B. Redirect auf Login), einmal neu anmelden
            if (response.status == HttpStatusCode.Found || response.status == HttpStatusCode.MovedPermanently) {
                val relogin = login()
                if (relogin.isFailure) {
                    return Result.failure(relogin.exceptionOrNull() ?: Exception("Erneute Anmeldung fehlgeschlagen"))
                }
                val retryResponse = client.get(vplanUrl) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header(HttpHeaders.Cookie, formatCookieHeader())
                }
                val html = retryResponse.bodyAsText()
                lastUpdated = SchulportalHtmlParser.extractLastUpdated(html)
                val entries = SchulportalHtmlParser.parseHtml(html)
                return Result.success(entries)
            }

            val html = response.bodyAsText()
            lastUpdated = SchulportalHtmlParser.extractLastUpdated(html)
            val entries = SchulportalHtmlParser.parseHtml(html)
            return Result.success(entries)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            client.close()
        }
    }

    private fun extractCookies(setCookieHeaders: List<String>?) {
        setCookieHeaders?.forEach { cookieHeader ->
            val cookiePart = cookieHeader.split(";").firstOrNull()?.trim() ?: return@forEach
            val equalIdx = cookiePart.indexOf('=')
            if (equalIdx > 0) {
                val name = cookiePart.substring(0, equalIdx).trim()
                val value = cookiePart.substring(equalIdx + 1).trim()
                cookieJar[name] = value
            }
        }
    }

    private fun formatCookieHeader(): String {
        return cookieJar.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
}
