package de.kjltowinkler.msapp.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.data.wear.PlatformWearPairing
import de.kjltowinkler.msapp.data.wear.WearPairingPaths
import de.kjltowinkler.msapp.util.AppContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearPairingListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        AppContextProvider.init(applicationContext)

        if (messageEvent.path == WearPairingPaths.PAIR_SESSION) {
            val payload = PlatformWearPairing.parseSessionPayload(messageEvent.data.decodeToString())
            if (payload != null) {
                BackendApiClient.saveSession(payload.token, payload.user)
                TasksRepository.startRealtimeSync()
                serviceScope.launch {
                    TasksRepository.reloadAndSync()
                    PlatformWearPairing.sendPairAckToPhone(messageEvent.sourceNodeId)
                }
            }
        }

        PlatformWearPairing.handleIncomingMessage(
            path = messageEvent.path,
            data = messageEvent.data,
            sourceNodeId = messageEvent.sourceNodeId
        )
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        AppContextProvider.init(applicationContext)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearPairingPaths.DATA_SESSION) {
                val payloadJson = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("payload") ?: continue
                val payload = PlatformWearPairing.parseSessionPayload(payloadJson) ?: continue

                BackendApiClient.saveSession(payload.token, payload.user)
                TasksRepository.startRealtimeSync()
                serviceScope.launch {
                    TasksRepository.reloadAndSync()
                    PlatformWearPairing.sendPairAckToPhone()
                }
                PlatformWearPairing.handleIncomingMessage(
                    path = WearPairingPaths.PAIR_SESSION,
                    data = payloadJson.encodeToByteArray(),
                    sourceNodeId = ""
                )
            }
        }
    }
}
