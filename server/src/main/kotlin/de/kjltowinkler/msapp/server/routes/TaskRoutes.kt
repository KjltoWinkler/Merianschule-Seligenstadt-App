package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.sync.SyncTasksRequest
import de.kjltowinkler.msapp.data.task.*
import de.kjltowinkler.msapp.server.repository.TaskRepository
import de.kjltowinkler.msapp.server.security.JwtService
import de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText

fun Route.taskRoutes() {
    // Echtzeit-WebSocket-Kanal für sofortige Aufgaben-Synchronisation über alle Geräte des Nutzers
    webSocket("/api/tasks/ws") {
        val token = call.request.queryParameters["token"]
            ?: call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()

        val userId = if (!token.isNullOrBlank()) JwtService.verifyToken(token) else null
        val effectiveUserId = userId ?: "anon_${java.util.UUID.randomUUID()}"

        TaskWebSocketManager.register(effectiveUserId, this)
        try {
            send(Frame.Text("""{"event":"connected","userId":"$effectiveUserId"}"""))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.contains("ping", ignoreCase = true)) {
                        send(Frame.Text("""{"event":"pong"}"""))
                    }
                }
            }
        } finally {
            TaskWebSocketManager.unregister(effectiveUserId, this)
        }
    }

    authenticate("auth-jwt") {
        route("/api/tasks") {

            // Get all tasks of the logged-in user
            get {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val tasks = TaskRepository.getTasks(userId)
                call.respond(HttpStatusCode.OK, tasks)
            }

            // Create or update a single task
            post {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val task = runCatching { call.receive<TaskItem>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Aufgabendaten"))
                }
                val saved = TaskRepository.upsertTask(userId, task)
                val origin = call.request.headers["X-Client-Instance-Id"]
                TaskWebSocketManager.notifyTaskUpdate(userId, "task_created", origin)
                call.respond(HttpStatusCode.Created, saved)
            }

            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val taskId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                val task = runCatching { call.receive<TaskItem>() }.getOrElse {
                    return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Aufgabendaten"))
                }
                val toSave = if (task.id == taskId) task else task.copy(id = taskId)
                val saved = TaskRepository.upsertTask(userId, toSave)
                val origin = call.request.headers["X-Client-Instance-Id"]
                TaskWebSocketManager.notifyTaskUpdate(userId, "task_updated", origin)
                call.respond(HttpStatusCode.OK, saved)
            }

            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val taskId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                val success = TaskRepository.deleteTask(userId, taskId)
                if (success) {
                    val origin = call.request.headers["X-Client-Instance-Id"]
                    TaskWebSocketManager.notifyTaskUpdate(userId, "task_deleted", origin)
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Aufgabe gelöscht"))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Aufgabe nicht gefunden"))
                }
            }

            // Task lists endpoints
            route("/lists") {
                get {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val lists = TaskRepository.getLists(userId)
                    call.respond(HttpStatusCode.OK, lists)
                }

                post {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val list = runCatching { call.receive<TaskList>() }.getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Listendaten"))
                    }
                    val saved = TaskRepository.upsertList(userId, list)
                    val origin = call.request.headers["X-Client-Instance-Id"]
                    TaskWebSocketManager.notifyTaskUpdate(userId, "list_created", origin)
                    call.respond(HttpStatusCode.Created, saved)
                }

                put("/{id}") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val listId = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                    val list = runCatching { call.receive<TaskList>() }.getOrElse {
                        return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Listendaten"))
                    }
                    val toSave = if (list.id == listId) list else list.copy(id = listId)
                    val saved = TaskRepository.upsertList(userId, toSave)
                    val origin = call.request.headers["X-Client-Instance-Id"]
                    TaskWebSocketManager.notifyTaskUpdate(userId, "list_updated", origin)
                    call.respond(HttpStatusCode.OK, saved)
                }

                delete("/{id}") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val listId = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                    val success = TaskRepository.deleteList(userId, listId)
                    if (success) {
                        val origin = call.request.headers["X-Client-Instance-Id"]
                        TaskWebSocketManager.notifyTaskUpdate(userId, "list_deleted", origin)
                        call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Liste gelöscht"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Liste konnte nicht gelöscht werden"))
                    }
                }

                // GET /api/tasks/lists/{id}/share -> Share-Info für Dialog
                get("/{id}/share") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val listId = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                    val shareInfo = TaskRepository.getShareInfo(userId, listId)
                    if (shareInfo != null) {
                        call.respond(HttpStatusCode.OK, shareInfo)
                    } else {
                        call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Liste nicht gefunden oder keine Berechtigung"))
                    }
                }

                // PUT /api/tasks/lists/{id}/share -> Freigabe-Einstellungen aktualisieren
                put("/{id}/share") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val listId = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                    val request = runCatching { call.receive<UpdateShareSettingsRequest>() }.getOrElse {
                        return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Freigabedaten"))
                    }
                    val updateResult = TaskRepository.updateShareSettings(userId, listId, request)
                    if (updateResult != null) {
                        for (uid in updateResult.removedUserIds) {
                            TaskWebSocketManager.notifyTaskUpdate(uid, "list_removed")
                        }
                        for (uid in updateResult.invitedUserIds) {
                            TaskWebSocketManager.notifyTaskUpdate(uid, "list_invitation")
                        }
                        val memberIds = TaskRepository.getSharedListMemberAndOwnerIds(listId)
                        for (uid in memberIds) {
                            TaskWebSocketManager.notifyTaskUpdate(uid, "share_settings_updated")
                        }
                        call.respond(HttpStatusCode.OK, updateResult.updatedInfo)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, SimpleApiResponse(false, "Nur der Eigentümer kann die Freigabe anpassen"))
                    }
                }

                // POST /api/tasks/lists/{id}/leave -> Geteilte Liste als Mitglied verlassen
                post("/{id}/leave") {
                    val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                    val listId = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                    val ownerId = TaskRepository.getSharedListOwnerId(listId)
                    val success = TaskRepository.leaveSharedList(userId, listId)
                    if (success) {
                        TaskWebSocketManager.notifyTaskUpdate(userId, "list_removed")
                        if (ownerId != null) {
                            TaskWebSocketManager.notifyTaskUpdate(ownerId, "member_left")
                        }
                        call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Liste erfolgreich verlassen"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Liste konnte nicht verlassen werden"))
                    }
                }
            }

            // Ausstehende Einladungen abrufen
            get("/invitations") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val invitations = TaskRepository.getPendingInvitations(userId)
                call.respond(HttpStatusCode.OK, invitations)
            }

            // Einladung annehmen
            post("/invitations/{id}/accept") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val listId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                val ownerId = TaskRepository.getSharedListOwnerId(listId)
                val success = TaskRepository.acceptInvitation(userId, listId)
                if (success) {
                    TaskWebSocketManager.notifyTaskUpdate(userId, "list_joined")
                    if (ownerId != null) {
                        TaskWebSocketManager.notifyTaskUpdate(ownerId, "member_joined")
                    }
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Einladung erfolgreich angenommen"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Einladung konnte nicht angenommen werden"))
                }
            }

            // Einladung ablehnen
            post("/invitations/{id}/decline") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val listId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt"))
                val ownerId = TaskRepository.getSharedListOwnerId(listId)
                val success = TaskRepository.declineInvitation(userId, listId)
                if (success) {
                    TaskWebSocketManager.notifyTaskUpdate(userId, "invitation_declined")
                    if (ownerId != null) {
                        TaskWebSocketManager.notifyTaskUpdate(ownerId, "share_settings_updated")
                    }
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Einladung abgelehnt"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Einladung konnte nicht abgelehnt werden"))
                }
            }

            // Geteilter Liste beitreten (als angemeldeter Nutzer)
            post("/share/{token}/join") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val token = call.parameters["token"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Token fehlt"))
                val result = TaskRepository.joinSharedList(token, userId)
                val joinedList = result.list
                if (result.success && joinedList != null) {
                    val memberIds = TaskRepository.getSharedListMemberAndOwnerIds(joinedList.id)
                    for (uid in memberIds) {
                        TaskWebSocketManager.notifyTaskUpdate(uid, "member_joined")
                    }
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.BadRequest, result)
                }
            }

            // Full two-way sync for offline/reconnecting apps
            post("/sync") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert"))
                val request = runCatching { call.receive<SyncTasksRequest>() }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Sync-Anfrage"))
                }
                val syncResult = TaskRepository.sync(userId, request)
                val origin = call.request.headers["X-Client-Instance-Id"]
                TaskWebSocketManager.notifyTaskUpdate(userId, "tasks_synced", origin)
                call.respond(HttpStatusCode.OK, syncResult)
            }
        }
    }

    // Öffentlicher oder authentifizierter API-Zugriff auf geteilte Liste über Token
    get("/api/tasks/share/{token}") {
        val token = call.parameters["token"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Token fehlt"))
        val authHeader = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
        val userId = if (!authHeader.isNullOrBlank()) JwtService.verifyToken(authHeader) else null

        val details = TaskRepository.getSharedListByToken(token, userId)
        if (details != null) {
            call.respond(HttpStatusCode.OK, details)
        } else {
            call.respond(HttpStatusCode.Forbidden, SimpleApiResponse(false, "Zugriff auf diese geteilte Liste verweigert oder Liste existiert nicht."))
        }
    }

    // Öffentliche Web-Vorschau: veloschulplaner.de/share/{token}
    get("/share/{token}") {
        val token = call.parameters["token"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Token fehlt")

        val details = TaskRepository.getSharedListByToken(token, null)
        val info = details?.info
        val tasks = details?.tasks ?: emptyList()
        val title = info?.title ?: "Aufgabenliste"
        val ownerName = info?.ownerName ?: "Unbekannt"
        val privacyLabel = when (info?.privacyMode) {
            TaskPrivacyMode.ANYONE_WITH_LINK -> "Jeder mit dem Link"
            TaskPrivacyMode.SCHOOL_USERS -> "Merianschule / Nutzer"
            TaskPrivacyMode.RESTRICTED -> "Eingeschränkt"
            null -> "Geteilt"
        }

        val tasksHtml = if (tasks.isEmpty()) {
            """<div style="text-align: center; color: #888; padding: 40px 20px;">Keine Aufgaben in dieser Liste</div>"""
        } else {
            tasks.joinToString("") { task ->
                val checked = if (task.isCompleted) "checked" else ""
                val strike = if (task.isCompleted) "text-decoration: line-through; opacity: 0.6;" else ""
                val subBadge = if (task.subject.isNotBlank()) """<span style="font-size: 11px; background: #2E382D; color: #A5D6A7; padding: 2px 8px; border-radius: 6px; margin-left: 8px;">${task.subject}</span>""" else ""
                val due = if (task.dueDate.isNotBlank()) """<div style="font-size: 12px; color: #8E968B; margin-top: 4px;">Fällig: ${task.dueDate} ${task.dueTime}</div>""" else ""
                """
                <div style="display: flex; align-items: flex-start; gap: 12px; padding: 14px 16px; background: #1E251D; border-radius: 12px; margin-bottom: 8px;">
                    <input type="checkbox" $checked disabled style="margin-top: 4px; width: 18px; height: 18px; accent-color: #76A073;" />
                    <div style="flex: 1;">
                        <div style="font-size: 15px; font-weight: 600; color: #E3E8E1; $strike">
                            ${task.title} $subBadge
                        </div>
                        ${if (task.description.isNotBlank()) """<div style="font-size: 13px; color: #B0B8AC; margin-top: 3px;">${task.description}</div>""" else ""}
                        $due
                    </div>
                </div>
                """.trimIndent()
            }
        }

        val html = """
            <!DOCTYPE html>
            <html lang="de">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${title} - Velo.Schulplaner</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
                        background: #111511;
                        color: #E3E8E1;
                        display: flex;
                        justify-content: center;
                        padding: 24px 16px;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 580px;
                        width: 100%;
                    }
                    .card {
                        background: #181F17;
                        border: 1px solid #273326;
                        border-radius: 24px;
                        padding: 24px;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                    }
                    .brand {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        margin-bottom: 20px;
                    }
                    .brand-tag {
                        font-size: 12px;
                        font-weight: 700;
                        letter-spacing: 0.5px;
                        color: #81C784;
                        text-transform: uppercase;
                        background: #203120;
                        padding: 4px 10px;
                        border-radius: 20px;
                    }
                    .title {
                        font-size: 26px;
                        font-weight: 700;
                        color: #FFFFFF;
                        margin-bottom: 6px;
                    }
                    .meta {
                        font-size: 14px;
                        color: #9AA497;
                        margin-bottom: 22px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .privacy-pill {
                        font-size: 12px;
                        padding: 3px 8px;
                        background: #2A3629;
                        border-radius: 8px;
                        color: #C2CDC0;
                    }
                    .actions {
                        display: flex;
                        gap: 10px;
                        margin-top: 24px;
                    }
                    .btn {
                        flex: 1;
                        padding: 12px 18px;
                        border-radius: 12px;
                        font-size: 14px;
                        font-weight: 600;
                        text-align: center;
                        cursor: pointer;
                        text-decoration: none;
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                        border: none;
                        transition: opacity 0.2s;
                    }
                    .btn-primary {
                        background: #4CAF50;
                        color: #0E1F0F;
                    }
                    .btn-secondary {
                        background: #252F24;
                        color: #E3E8E1;
                    }
                    .btn:hover { opacity: 0.9; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="card">
                        <div class="brand">
                            <span class="brand-tag">Velo.Schulplaner</span>
                        </div>
                        <h1 class="title">${title}</h1>
                        <div class="meta">
                            <span>Geteilt von <strong>${ownerName}</strong></span>
                            <span>•</span>
                            <span class="privacy-pill">${privacyLabel}</span>
                        </div>
                        
                        <div style="margin-top: 18px;">
                            ${tasksHtml}
                        </div>

                        <div class="actions">
                            <button class="btn btn-secondary" onclick="navigator.clipboard.writeText(window.location.href); alert('Link kopiert!');">
                                Link kopieren
                            </button>
                            <a class="btn btn-primary" id="openInAppBtn" href="veloschulplaner://share/${token}" onclick="openInApp('${token}'); return true;">
                                In App öffnen
                            </a>
                        </div>
                    </div>
                </div>

                <script>
                    function openInApp(token) {
                        const isAndroid = /Android/i.test(navigator.userAgent);
                        if (isAndroid) {
                            // Android Intent URI als bevorzugter Weg für moderne Mobil-Browser
                            const intentUri = "intent://share/" + token + "#Intent;scheme=veloschulplaner;package=de.kjltowinkler.msapp;end";
                            window.location.href = intentUri;
                        } else {
                            window.location.href = "veloschulplaner://share/" + token;
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        call.respondText(html, ContentType.Text.Html)
    }
}
