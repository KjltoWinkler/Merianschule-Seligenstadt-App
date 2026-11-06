package de.kjltowinkler.msapp.util

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object PlatformNetworkMonitor {
    private val _isOnline = MutableStateFlow(runCatching { window.navigator.onLine }.getOrDefault(true))
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        runCatching {
            window.addEventListener("online") { _isOnline.value = true }
            window.addEventListener("offline") { _isOnline.value = false }
        }
    }
}
