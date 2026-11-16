package com.battleground.royale3d.network

import com.battleground.royale3d.model.NetPacket
import com.battleground.royale3d.model.WsConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MultiplayerClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    val localPlayerId: String = "player_${UUID.randomUUID().toString().take(6)}"

    private val _connectionState = MutableStateFlow(WsConnectionState.LOCAL_SIMULATION)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _lastPingMs = MutableStateFlow<Int?>(null)
    val lastPingMs: StateFlow<Int?> = _lastPingMs.asStateFlow()

    val remotePeers = ConcurrentHashMap<String, NetPacket>()

    private var webSocket: WebSocket? = null
    private var lastSentAtMs = 0L

    fun connect(url: String) {
        disconnect()
        if (url.isBlank() || (!url.startsWith("ws://") && !url.startsWith("wss://"))) {
            _connectionState.value = WsConnectionState.LOCAL_SIMULATION
            return
        }
        _connectionState.value = WsConnectionState.CONNECTING
        val request = Request.Builder().url(url.trim()).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = WsConnectionState.CONNECTED
                sendPacket(NetPacket(type = "join", playerId = localPlayerId))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val packet = runCatching { json.decodeFromString<NetPacket>(text) }.getOrNull()
                    ?: return
                if (packet.playerId == localPlayerId) {
                    val rtt = (System.currentTimeMillis() - lastSentAtMs).toInt().coerceIn(5, 999)
                    _lastPingMs.value = rtt
                    return
                }
                if (packet.type == "state" || packet.type == "join") {
                    remotePeers[packet.playerId] = packet
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.value = WsConnectionState.DISCONNECTED_FALLBACK
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = WsConnectionState.DISCONNECTED_FALLBACK
            }
        })
    }

    fun sendPlayerState(x: Float, y: Float, z: Float, yaw: Float, weaponId: String, health: Int) {
        val now = System.currentTimeMillis()
        if (now - lastSentAtMs < 80L) return
        lastSentAtMs = now
        sendPacket(
            NetPacket(
                type = "state",
                playerId = localPlayerId,
                x = x,
                y = y,
                z = z,
                yaw = yaw,
                weapon = weaponId,
                health = health
            )
        )
    }

    fun sendFireEvent(x: Float, y: Float, z: Float, yaw: Float, weaponId: String) {
        sendPacket(
            NetPacket(
                type = "fire",
                playerId = localPlayerId,
                x = x,
                y = y,
                z = z,
                yaw = yaw,
                weapon = weaponId
            )
        )
    }

    private fun sendPacket(packet: NetPacket) {
        val ws = webSocket ?: return
        runCatching {
            ws.send(json.encodeToString(packet))
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "closed")
        webSocket = null
        remotePeers.clear()
        _lastPingMs.value = null
        _connectionState.value = WsConnectionState.LOCAL_SIMULATION
    }
}
