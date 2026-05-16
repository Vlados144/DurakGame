package com.example.durakgame.network

import com.example.durakgame.engine.model.GameConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
class LocalNetworkManager : GameNetworkManager {
    private val udpDiscovery = UdpDiscovery()
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var isHost = false
    private var myPlayerId = UUID.randomUUID().toString()
    private var gameCode = ""
    private val clientWriters = ConcurrentHashMap<String, PrintWriter>()
    private val _messages = MutableSharedFlow<GameMessage>(replay = 0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun getUdpDiscovery(): UdpDiscovery = udpDiscovery

    override fun startHost(config: GameConfig, playerName: String): String {
        try { serverSocket?.close() } catch (_: Exception) {}
        isHost = true
        gameCode = generateGameCode()
        serverSocket = ServerSocket(55555)

        // UDP
        udpDiscovery.startAnnouncing(gameCode, 55555)

        scope.launch {
            try {
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    handleClient(socket)
                }
            } catch (_: Exception) {}
        }
        return gameCode
    }
    override fun connectToGame(hostAddress: String, playerName: String) {
        isHost = false
        val parts = hostAddress.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 55555

        scope.launch {
            try {
                clientSocket = Socket(host, port)
                writer = PrintWriter(clientSocket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                sendRaw(GameMessage(type = "JOIN", playerId = myPlayerId, playerName = playerName))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    _messages.emit(parseMessage(JSONObject(line)))
                }
            } catch (e: Exception) {
                if (isActive) _messages.emit(GameMessage(type = "ERROR", error = e.message))
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val cWriter = PrintWriter(socket.getOutputStream(), true)
                var clientId = ""
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val msg = parseMessage(JSONObject(line))
                    when (msg.type) {
                        "JOIN" -> {
                            clientId = msg.playerId ?: UUID.randomUUID().toString()
                            clientWriters[clientId] = cWriter
                            cWriter.println(toJson(GameMessage(type = "JOINED", playerId = msg.playerId, gameCode = gameCode)))
                            _messages.emit(msg)
                        }
                        else -> _messages.emit(msg)
                    }
                }
            } catch (_: Exception) {}
            finally {
                // Клиент отключился — отправляем PLAYER_LEFT
                android.util.Log.d("Network", "Клиент отключился")
                _messages.emit(GameMessage(type = "PLAYER_LEFT"))
            }
        }
    }

    override fun sendAction(action: GameMessage) {
        if (isHost) broadcast(action) else sendRaw(action)
    }

    private fun sendRaw(msg: GameMessage) {
        scope.launch {
            try { writer?.println(toJson(msg)) } catch (_: Exception) {}
        }
    }

    private fun broadcast(msg: GameMessage) {
        scope.launch {
            val json = toJson(msg)
            clientWriters.values.forEach { try { it.println(json) } catch (_: Exception) {} }
        }
    }

    override fun observeMessages(): Flow<GameMessage> = _messages.asSharedFlow()
    override fun getGameCode() = gameCode
    override fun getMyPlayerId() = myPlayerId

    override fun disconnect() {
        udpDiscovery.stop()
        scope.cancel()
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        clientWriters.clear()
        _messages.resetReplayCache()
    }

    private fun generateGameCode() = (1..4).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")

    private fun toJson(msg: GameMessage): String {
        return JSONObject().apply {
            put("type", msg.type)
            msg.playerId?.let { put("playerId", it) }
            msg.playerName?.let { put("playerName", it) }
            msg.cardId?.let { put("cardId", it) }
            msg.againstCardId?.let { put("againstCardId", it) }
            msg.gameCode?.let { put("gameCode", it) }
            msg.playersJson?.let { put("playersJson", it) }
            msg.trumpSuit?.let { put("trumpSuit", it) }
            msg.trumpCardId?.let { put("trumpCardId", it) }
            msg.error?.let { put("error", it) }
        }.toString()
    }

    private fun parseMessage(json: JSONObject): GameMessage {
        return GameMessage(
            type = json.getString("type"),
            playerId = json.optString("playerId", null),
            playerName = json.optString("playerName", null),
            cardId = json.optString("cardId", null),
            againstCardId = json.optString("againstCardId", null),
            gameCode = json.optString("gameCode", null),
            playersJson = json.optString("playersJson", null),
            trumpSuit = json.optString("trumpSuit", null),
            trumpCardId = json.optString("trumpCardId", null),
            error = json.optString("error", null)
        )
    }
}