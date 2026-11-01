package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.sync.SyncTasksRequest
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TaskList
import de.kjltowinkler.msapp.server.repository.TaskRepository
import de.kjltowinkler.msapp.server.security.JwtService
import de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
        if (userId.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Ungültiges oder fehlendes Token"))
            return@webSocket
        }

        TaskWebSocketManager.register(userId, this)
        try {
            send(Frame.Text("""{"event":"connected","userId":"$userId"}"""))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.contains("ping", ignoreCase = true)) {
                        send(Frame.Text("""{"event":"pong"}"""))
                    }
                }
            }
        } finally {
            TaskWebSocketManager.unregister(userId, this)
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
}
