package de.kjltowinkler.msapp.util

import kotlinx.coroutines.flow.StateFlow

/**
 * Plattformübergreifender Monitor für die Netzwerk-Konnektivität.
 * Ermöglicht die reaktive Überwachung des Online-/Offline-Status
 * und automatische Re-Synchronisation bei Reconnects.
 */
expect object PlatformNetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
