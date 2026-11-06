package de.kjltowinkler.msapp.ui.components

import androidx.compose.material3.SnackbarDuration
import de.kjltowinkler.msapp.util.PlatformNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class AppSnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onActionPerformed: (() -> Unit)? = null
)

/**
 * Zentraler Manager für Snackbar-Benachrichtigungen in der App.
 * Überwacht automatisch den Netzwerkstatus und liefert reaktive Offline-Meldungen
 * sowohl bei Verbindungsabbrüchen als auch bei Aktionen, die eine Internetverbindung erfordern.
 */
object AppSnackbarManager {

    private val _messages = MutableSharedFlow<AppSnackbarMessage>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()

    private var networkMonitorStarted = false

    fun showMessage(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onActionPerformed: (() -> Unit)? = null
    ) {
        _messages.tryEmit(
            AppSnackbarMessage(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
                duration = duration,
                onActionPerformed = onActionPerformed
            )
        )
    }

    /**
     * Informiert per Snackbar darüber, dass eine netzwerkabhängige Aktion
     * im aktuellen Offline-Zustand nicht live ausgeführt werden kann.
     */
    fun showOfflineActionMessage(actionDescription: String? = null) {
        val msg = if (actionDescription.isNullOrBlank()) {
            "Keine Internetverbindung • Gespeicherte Daten aktiv"
        } else {
            "Keine Internetverbindung • $actionDescription nicht möglich (Offline-Modus)"
        }
        showMessage(
            message = msg,
            duration = SnackbarDuration.Short
        )
    }

    /**
     * Startet die Überwachung des Netzwerkstatus.
     * Erkennt Verbindungsverlust in Echtzeit und informiert den Nutzer sofort per Snackbar.
     */
    fun bindToNetworkMonitor(scope: CoroutineScope) {
        if (networkMonitorStarted) return
        networkMonitorStarted = true

        scope.launch {
            var previousOnlineState: Boolean? = null
            PlatformNetworkMonitor.isOnline.collect { isOnline ->
                if (previousOnlineState != null) {
                    if (previousOnlineState == true && !isOnline) {
                        showMessage(
                            message = "Keine Internetverbindung mehr • Die App ist jetzt im Offline-Modus",
                            duration = SnackbarDuration.Short
                        )
                    } else if (previousOnlineState == false && isOnline) {
                        showMessage(
                            message = "Verbindung wiederhergestellt • Wieder online",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                previousOnlineState = isOnline
            }
        }
    }
}
