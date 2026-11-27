package de.kjltowinkler.msapp.data.wear

import de.kjltowinkler.msapp.data.auth.UserProfileDto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable

object WearPairingPaths {
    const val REQUEST_PAIR = "/msapp/request_pair"
    const val PAIR_SESSION = "/msapp/pair_session"
    const val PAIR_ACK = "/msapp/pair_ack"
    const val DATA_SESSION = "/msapp/session_data"
    const val UNPAIR = "/msapp/unpair"
}

@Serializable
data class WearPairingSessionPayload(
    val token: String,
    val user: UserProfileDto,
    val timestamp: Long = 0L
)

@Serializable
data class PairedWatchInfo(
    val nodeId: String,
    val displayName: String,
    val isNearby: Boolean = false,
    val isConnected: Boolean = false,
    val isPairedInApp: Boolean = false,
    val lastSyncedAt: Long = 0L
)

expect object PlatformWearPairing {
    val incomingPairSession: SharedFlow<WearPairingSessionPayload>
    val incomingPairAck: SharedFlow<String>
    val incomingPairRequest: SharedFlow<String>

    suspend fun sendSessionToWatch(token: String, user: UserProfileDto): Result<String>
    suspend fun requestPairingFromPhone(): Result<Unit>
    suspend fun sendPairAckToPhone(sourceNodeId: String? = null)
    suspend fun getWatchDevices(): List<PairedWatchInfo>
    suspend fun unpairWatch(nodeId: String): Result<Unit>
    fun startListening()
    fun stopListening()
}
