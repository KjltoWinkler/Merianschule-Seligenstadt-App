package de.kjltowinkler.msapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface

actual object PlatformNetworkMonitor {
    private val _isOnline = MutableStateFlow(checkConnectivity())
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private fun checkConnectivity(): Boolean {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback && !iface.isVirtual) {
                    return@runCatching true
                }
            }
            false
        }.getOrDefault(true)
    }
}
