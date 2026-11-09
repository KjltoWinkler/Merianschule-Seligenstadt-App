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
