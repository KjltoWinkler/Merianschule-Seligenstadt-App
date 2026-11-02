package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.update.AppUpdateInfo
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.security.JwtService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

@Serializable
data class AdminUpdateAppInfoRequest(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String = "/api/app/download/latest.apk",
    val releaseNotes: String = ""
)

@Serializable
data class AdminAppUpdateStatusDto(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val fileSizeBytes: Long,
    val publishedAt: Long,
    val apkExists: Boolean,
    val apkFilePath: String
)

object AppUpdateRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getInfoFile(): File {
        ServerConfig.ensureDataDirectory()
        return File(ServerConfig.dataDir, "app_update_info.json")
    }

    fun getApkFile(): File {
        ServerConfig.ensureDataDirectory()
        val updatesDir = File(ServerConfig.dataDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()
        return File(updatesDir, "latest.apk")
    }

    @Synchronized
    fun getUpdateInfo(): AppUpdateInfo {
        val file = getInfoFile()
        val apk = getApkFile()
        val info = if (file.exists()) {
            runCatching {
                json.decodeFromString<AppUpdateInfo>(file.readText())
            }.getOrElse { defaultInfo() }
        } else {
            val def = defaultInfo()
            saveUpdateInfo(def)
            def
        }

        // If an APK file is on disk, reflect its real file size
        return if (apk.exists()) {
            info.copy(fileSizeBytes = apk.length())
        } else {
            info
        }
    }

    @Synchronized
    fun saveUpdateInfo(info: AppUpdateInfo) {
        val file = getInfoFile()
        file.writeText(json.encodeToString(info))
    }

    private fun defaultInfo() = AppUpdateInfo(
        versionCode = 1,
        versionName = "1.0.0",
        downloadUrl = "/api/app/download/latest.apk",
        releaseNotes = "• Merianschule Seligenstadt App - Initiale Version mit Stundenplan, Vertretungsplan und Aufgaben",
        fileSizeBytes = 0L,
        publishedAt = System.currentTimeMillis()
    )
}

private fun verifyAdminAccess(call: ApplicationCall): Boolean {
    val adminKeyHeader = call.request.header("X-Admin-Key")
    if (adminKeyHeader != null && adminKeyHeader == ServerConfig.adminKey) {
        return true
    }

    val authHeader = call.request.header("Authorization")
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        val token = authHeader.removePrefix("Bearer ").trim()
        return try {
            val decoded = JwtService.verifier.verify(token)
            val role = decoded.getClaim("role").asString()
            role.equals("ADMIN", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    return false
}

fun Route.appUpdateRoutes() {

    route("/api/app") {

        // Öffentlich: App-Version und Update-Informationen abfragen
        get("/version") {
            val info = AppUpdateRepository.getUpdateInfo()
            call.respond(HttpStatusCode.OK, info)
        }

        // Öffentlich: Neueste APK-Datei herunterladen
        get("/download/latest.apk") {
            val apkFile = AppUpdateRepository.getApkFile()
            if (!apkFile.exists()) {
                call.respond(
                    HttpStatusCode.NotFound,
                    SimpleApiResponse(false, "Noch keine APK-Datei auf dem Server hinterlegt. Bitte wende dich an den Administrator.")
                )
                return@get
            }

            val info = AppUpdateRepository.getUpdateInfo()
            val filename = "MerianschuleApp-${info.versionName}.apk"

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, filename).toString()
            )
            call.response.header(HttpHeaders.ContentType, "application/vnd.android.package-archive")
            call.respondFile(apkFile)
        }
    }

    // Admin Endpoints zur Pflege von APK-Dateien und Versionsinformationen
    route("/api/admin/app") {

        get("/info") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                return@get
            }

            val info = AppUpdateRepository.getUpdateInfo()
            val apk = AppUpdateRepository.getApkFile()

            call.respond(HttpStatusCode.OK, AdminAppUpdateStatusDto(
                versionCode = info.versionCode,
                versionName = info.versionName,
                downloadUrl = info.downloadUrl,
                releaseNotes = info.releaseNotes,
                fileSizeBytes = if (apk.exists()) apk.length() else info.fileSizeBytes,
                publishedAt = info.publishedAt,
                apkExists = apk.exists(),
                apkFilePath = apk.absolutePath
            ))
        }

        post("/info") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                return@post
            }

            val req = runCatching { call.receive<AdminUpdateAppInfoRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Versionsdaten."))
                return@post
            }

            if (req.versionName.isBlank() || req.versionCode <= 0) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Versionsnummer oder Build-Code."))
                return@post
            }

            val current = AppUpdateRepository.getUpdateInfo()
            val apk = AppUpdateRepository.getApkFile()

            val updated = current.copy(
                versionCode = req.versionCode,
                versionName = req.versionName.trim(),
                downloadUrl = req.downloadUrl.ifBlank { "/api/app/download/latest.apk" },
                releaseNotes = req.releaseNotes.trim(),
                fileSizeBytes = if (apk.exists()) apk.length() else current.fileSizeBytes,
                publishedAt = System.currentTimeMillis()
            )

            AppUpdateRepository.saveUpdateInfo(updated)
            call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "App-Update-Informationen erfolgreich aktualisiert."))
        }

        post("/upload-apk") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                return@post
            }

            val multipart = call.receiveMultipart()
            var uploadedBytes = 0L
            val apkFile = AppUpdateRepository.getApkFile()

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val originalName = part.originalFileName ?: "update.apk"
                    if (originalName.endsWith(".apk", ignoreCase = true)) {
                        part.streamProvider().use { input ->
                            FileOutputStream(apkFile).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var bytes = input.read(buffer)
                                while (bytes >= 0) {
                                    output.write(buffer, 0, bytes)
                                    uploadedBytes += bytes
                                    bytes = input.read(buffer)
                                }
                            }
                        }
                    }
                }
                part.dispose()
            }

            if (uploadedBytes == 0L) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Keine gültige .apk Datei empfangen."))
                return@post
            }

            val current = AppUpdateRepository.getUpdateInfo()
            val updated = current.copy(
                fileSizeBytes = uploadedBytes,
                publishedAt = System.currentTimeMillis()
            )
            AppUpdateRepository.saveUpdateInfo(updated)

            call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "APK erfolgreich hochgeladen (${uploadedBytes / 1024 / 1024} MB)."))
        }
    }
}
