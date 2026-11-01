package de.kjltowinkler.msapp.server.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

private object AdminWebPanelHolder

private val adminPanelHtml: String by lazy {
    AdminWebPanelHolder::class.java.getResourceAsStream("/admin_panel.html")?.bufferedReader()?.use { it.readText() }
        ?: "<h1>Admin Panel Ressource konnte nicht geladen werden.</h1>"
}

fun Route.adminWebPanel() {
    route("/admin") {
        get {
            call.respondText(adminPanelHtml, ContentType.Text.Html)
        }
    }
}
