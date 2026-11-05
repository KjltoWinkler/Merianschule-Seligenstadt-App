package de.kjltowinkler.msapp.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NavigationEvents {
    private val _routeFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val routeFlow = _routeFlow.asSharedFlow()

    fun emitRoute(route: String) {
        _routeFlow.tryEmit(route)
    }
}
