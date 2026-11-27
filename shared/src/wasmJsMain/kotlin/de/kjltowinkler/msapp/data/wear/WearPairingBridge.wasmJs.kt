package de.kjltowinkler.msapp.data.wear

import de.kjltowinkler.msapp.data.auth.UserProfileDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

actual object PlatformWearPairing {
    actual val incomingPairSession: SharedFlow<WearPairingSessionPayload> = MutableSharedFlow<WearPairingSessionPayload>().asSharedFlow()
    actual val incomingPairAck: SharedFlow<String> = MutableSharedFlow<String>().asSharedFlow()
    actual val incomingPairRequest: SharedFlow<String> = MutableSharedFlow<String>().asSharedFlow()

    actual suspend fun sendSessionToWatch(token: String, user: UserProfileDto): Result<String> =
        Result.failure(UnsupportedOperationException("Smartwatch-Pairing wird nur auf Android unterstützt."))

    actual suspend fun requestPairingFromPhone(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Smartwatch-Pairing wird nur auf Android unterstützt."))

    actual suspend fun sendPairAckToPhone(sourceNodeId: String?) {}
    actual suspend fun getWatchDevices(): List<PairedWatchInfo> = emptyList()
    actual suspend fun unpairWatch(nodeId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Smartwatch-Pairing wird nur auf Android unterstützt."))
    actual fun startListening() {}
    actual fun stopListening() {}
}
