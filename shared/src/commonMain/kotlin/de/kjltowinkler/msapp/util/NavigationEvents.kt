package de.kjltowinkler.msapp.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NavigationTarget(
    val route: String,
    val highlightId: String? = null
)

object NavigationEvents {
    private val _targetFlow = MutableSharedFlow<NavigationTarget>(extraBufferCapacity = 10)
    val targetFlow = _targetFlow.asSharedFlow()

    private val _routeFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val routeFlow = _routeFlow.asSharedFlow()

    var pendingTarget: NavigationTarget? = null

    private val _shareTokenFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val shareTokenFlow = _shareTokenFlow.asSharedFlow()

    var pendingShareToken: String? = null

    fun emitShareToken(tokenOrUrl: String) {
        val cleanToken = parseShareToken(tokenOrUrl) ?: tokenOrUrl.trim()
        if (cleanToken.isNotBlank()) {
            pendingShareToken = cleanToken
            _shareTokenFlow.tryEmit(cleanToken)
        }
    }

    fun consumePendingShareToken(): String? {
        val t = pendingShareToken
        pendingShareToken = null
        return t
    }

    fun parseShareToken(input: String?): String? {
        if (input == null) return null
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        var s = trimmed
            .substringBefore('?')
            .substringBefore('#')
            .trim()

        val schemeIndex = s.indexOf("://")
        if (schemeIndex != -1) {
            s = s.substring(schemeIndex + 3)
        }
        s = s.trimStart('/')

        s = s.removePrefix("veloschulplaner.de")
            .removePrefix("api.veloschulplaner.de")
        s = s.trimStart('/')

        s = s.removePrefix("share")
        s = s.trim('/')

        return s.ifBlank { null }
    }

    fun emit(route: String, highlightId: String? = null) {
        val target = NavigationTarget(route, highlightId)
        pendingTarget = target
        _targetFlow.tryEmit(target)
        _routeFlow.tryEmit(route)
    }

    fun emitRoute(route: String, highlightId: String? = null) {
        emit(route, highlightId)
    }

    fun consumePendingHighlight(forRoute: String): String? {
        val target = pendingTarget
        if (target != null && target.route == forRoute) {
            val h = target.highlightId
            pendingTarget = null
            return h
        }
        return null
    }
}
