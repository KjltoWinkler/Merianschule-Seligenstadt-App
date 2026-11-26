package de.kjltowinkler.msapp.data.wear

import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import de.kjltowinkler.msapp.data.auth.UserProfileDto
import de.kjltowinkler.msapp.util.AppContextProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual object PlatformWearPairing {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _incomingPairSession = MutableSharedFlow<WearPairingSessionPayload>(extraBufferCapacity = 4)
    actual val incomingPairSession: SharedFlow<WearPairingSessionPayload> = _incomingPairSession.asSharedFlow()

    private val _incomingPairAck = MutableSharedFlow<String>(extraBufferCapacity = 4)
    actual val incomingPairAck: SharedFlow<String> = _incomingPairAck.asSharedFlow()

    private val _incomingPairRequest = MutableSharedFlow<String>(extraBufferCapacity = 4)
    actual val incomingPairRequest: SharedFlow<String> = _incomingPairRequest.asSharedFlow()

    private var activeAckDeferred: CompletableDeferred<String>? = null
    private var messageListener: MessageClient.OnMessageReceivedListener? = null

    fun parseSessionPayload(jsonStr: String): WearPairingSessionPayload? =
        runCatching { json.decodeFromString<WearPairingSessionPayload>(jsonStr) }.getOrNull()

    fun handleIncomingMessage(path: String, data: ByteArray, sourceNodeId: String) {
        when (path) {
            WearPairingPaths.PAIR_SESSION -> {
                runCatching {
                    val payload = json.decodeFromString<WearPairingSessionPayload>(data.decodeToString())
                    _incomingPairSession.tryEmit(payload)
                }
            }
            WearPairingPaths.PAIR_ACK -> {
                activeAckDeferred?.complete(sourceNodeId)
                _incomingPairAck.tryEmit(sourceNodeId)
            }
            WearPairingPaths.REQUEST_PAIR -> {
                _incomingPairRequest.tryEmit(sourceNodeId)
            }
        }
    }

    actual fun startListening() {
        val context = runCatching { AppContextProvider.context }.getOrNull() ?: return
        if (messageListener != null) return
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            handleIncomingMessage(event.path, event.data, event.sourceNodeId)
        }
        messageListener = listener
        runCatching {
            Wearable.getMessageClient(context).addListener(listener)
        }
    }

    actual fun stopListening() {
        val context = runCatching { AppContextProvider.context }.getOrNull() ?: return
        val listener = messageListener ?: return
        messageListener = null
        runCatching {
            Wearable.getMessageClient(context).removeListener(listener)
        }
    }

    actual suspend fun sendSessionToWatch(token: String, user: UserProfileDto): Result<String> {
        val context = runCatching { AppContextProvider.context }.getOrElse {
            return Result.failure(IllegalStateException("App-Kontext nicht initialisiert."))
        }

        startListening()

        val nodes = runCatching {
            Wearable.getNodeClient(context).connectedNodes.awaitTask()
        }.getOrElse {
            return Result.failure(IllegalStateException("Wearable-Dienst nicht verfügbar: ${it.message}"))
        }

        if (nodes.isEmpty()) {
            return Result.failure(
                IllegalStateException("Keine Smartwatch verbunden. Stelle sicher, dass deine Uhr per Bluetooth oder WLAN gekoppelt ist.")
            )
        }

        val payload = WearPairingSessionPayload(
            token = token,
            user = user,
            timestamp = System.currentTimeMillis()
        )
        val payloadBytes = json.encodeToString(payload).encodeToByteArray()

        val ackDeferred = CompletableDeferred<String>()
        activeAckDeferred = ackDeferred

        // DataItem als Fallback ablegen, falls Watch erst verzögert aufwacht
        runCatching {
            val putDataReq = PutDataMapRequest.create(WearPairingPaths.DATA_SESSION).apply {
                dataMap.putString("payload", json.encodeToString(payload))
                dataMap.putLong("timestamp", payload.timestamp)
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(putDataReq).awaitTask()
        }

        val messageClient = Wearable.getMessageClient(context)
        var sentCount = 0
        for (node in nodes) {
            val sent = runCatching {
                messageClient.sendMessage(node.id, WearPairingPaths.PAIR_SESSION, payloadBytes).awaitTask()
            }.isSuccess
            if (sent) sentCount++
        }

        if (sentCount == 0) {
            activeAckDeferred = null
            return Result.failure(IllegalStateException("Verbindung zur Smartwatch fehlgeschlagen."))
        }

        val ackNodeId = withTimeoutOrNull(15_000L) {
            ackDeferred.await()
        }
        activeAckDeferred = null

        return if (ackNodeId != null) {
            val displayName = nodes.firstOrNull { it.id == ackNodeId }?.displayName
                ?: nodes.firstOrNull()?.displayName
                ?: "Smartwatch"
            Result.success(displayName)
        } else {
            Result.failure(
                IllegalStateException("Die Smartwatch hat nicht rechtzeitig geantwortet. Bitte öffne die Notes+ App auf deiner Uhr und versuche es erneut.")
            )
        }
    }

    actual suspend fun requestPairingFromPhone(): Result<Unit> {
        val context = runCatching { AppContextProvider.context }.getOrElse {
            return Result.failure(IllegalStateException("App-Kontext nicht initialisiert."))
        }

        startListening()

        val nodes = runCatching {
            Wearable.getNodeClient(context).connectedNodes.awaitTask()
        }.getOrElse {
            return Result.failure(IllegalStateException("Wearable-Dienst nicht verfügbar."))
        }

        if (nodes.isEmpty()) {
            return Result.failure(IllegalStateException("Kein Smartphone verbunden."))
        }

        val messageClient = Wearable.getMessageClient(context)
        var sentAny = false
        for (node in nodes) {
            val ok = runCatching {
                messageClient.sendMessage(node.id, WearPairingPaths.REQUEST_PAIR, ByteArray(0)).awaitTask()
            }.isSuccess
            if (ok) sentAny = true
        }

        return if (sentAny) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Anfrage an das Smartphone fehlgeschlagen."))
        }
    }

    actual suspend fun sendPairAckToPhone(sourceNodeId: String?) {
        val context = runCatching { AppContextProvider.context }.getOrNull() ?: return
        val messageClient = Wearable.getMessageClient(context)
        if (!sourceNodeId.isNullOrBlank()) {
            runCatching {
                messageClient.sendMessage(sourceNodeId, WearPairingPaths.PAIR_ACK, ByteArray(0)).awaitTask()
            }
            return
        }
        val nodes = runCatching {
            Wearable.getNodeClient(context).connectedNodes.awaitTask()
        }.getOrNull() ?: return
        for (node in nodes) {
            scope.launch {
                runCatching {
                    messageClient.sendMessage(node.id, WearPairingPaths.PAIR_ACK, ByteArray(0)).awaitTask()
                }
            }
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { e ->
            if (cont.isActive) cont.resumeWithException(e)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
