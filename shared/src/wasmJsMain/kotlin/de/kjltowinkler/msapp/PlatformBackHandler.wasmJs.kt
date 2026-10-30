package de.kjltowinkler.msapp

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.w3c.dom.events.Event

/**
 * Zentraler Back-Dispatcher für Web/Wasm.
 * Verbindet Browser-History (popstate), Android-System-Zurück und In-App-Wischgesten.
 */
object WebBackDispatcher {
    private val callbacks = mutableListOf<() -> Boolean>()
    private var isListenerAttached = false
    private var isHandlingPopState = false

    private val popStateListener: (Event) -> Unit = {
        if (!isHandlingPopState) {
            isHandlingPopState = true
            try {
                var consumed = false
                // Den obersten (zuletzt hinzugefügten) Callback zuerst ausführen
                for (i in callbacks.lastIndex downTo 0) {
                    val cb = callbacks[i]
                    if (cb()) {
                        consumed = true
                        break
                    }
                }
                // Wenn ein Callback konsumiert wurde und weiterhin Callbacks aktiv sind,
                // halten wir einen Dummy-State im Verlauf, damit die nächste Geste wieder funktioniert
                if (consumed && callbacks.isNotEmpty()) {
                    ensureHistoryState()
                }
            } finally {
                isHandlingPopState = false
            }
        }
    }

    fun register(callback: () -> Boolean): () -> Unit {
        callbacks.add(callback)
        attachListenerIfNeeded()
        ensureHistoryState()
        return {
            callbacks.remove(callback)
        }
    }

    /**
     * Löst manuell eine Zurück-Aktion aus (z. B. durch Wischgeste vom Bildschirmrand).
     */
    fun triggerBack(): Boolean {
        for (i in callbacks.lastIndex downTo 0) {
            val cb = callbacks[i]
            if (cb()) {
                return true
            }
        }
        return false
    }

    private fun attachListenerIfNeeded() {
        if (!isListenerAttached) {
            isListenerAttached = true
            window.addEventListener("popstate", popStateListener)
        }
    }

    private fun ensureHistoryState() {
        try {
            window.history.pushState(null, "", window.location.href)
        } catch (_: Throwable) {}
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val unregister = WebBackDispatcher.register {
            currentOnBack()
            true
        }
        onDispose {
            unregister()
        }
    }
}

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<PredictiveBackProgress>) -> Unit
) {
    val scope = rememberCoroutineScope()
    PlatformBackHandler(enabled = enabled) {
        scope.launch {
            onBack(emptyFlow())
        }
    }
}

@Composable
actual fun PlatformNavigationBackHandler(navController: NavHostController) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val canGoBack = navController.previousBackStackEntry != null

    PlatformBackHandler(enabled = canGoBack) {
        navController.popBackStack()
    }
}

/**
 * Erkennt Wischgesten vom linken oder rechten Bildschirmrand (36dp Randbereich)
 * und löst die Zurück-Aktion aus – analog zur nativen Android Back-Gesture.
 */
actual fun Modifier.platformEdgeBackGesture(): Modifier = this.pointerInput(Unit) {
    val edgeThreshold = 36.dp.toPx()
    val triggerDistance = 50.dp.toPx()

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val isLeftEdge = down.position.x <= edgeThreshold
        val isRightEdge = down.position.x >= (size.width - edgeThreshold)

        if (isLeftEdge || isRightEdge) {
            var isTriggered = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val diffX = change.position.x - down.position.x
                val diffY = kotlin.math.abs(change.position.y - down.position.y)

                val shouldTrigger = if (isLeftEdge) {
                    diffX > triggerDistance && diffY < diffX * 0.7f
                } else {
                    diffX < -triggerDistance && diffY < (-diffX) * 0.7f
                }

                if (shouldTrigger && !isTriggered) {
                    isTriggered = true
                    change.consume()
                    WebBackDispatcher.triggerBack()
                    break
                }
            }
        }
    }
}
