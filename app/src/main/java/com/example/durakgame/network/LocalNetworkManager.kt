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
    
    private var managerJob = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + managerJob)

    fun getUdpDiscovery(): UdpDiscovery = udpDiscovery

    override fun startHost(config: GameConfig, playerName: String): String {
        disconnect()
        isHost = true
        gameCode = generateGameCode()
        
        try {
            serverSocket = ServerSocket(55555)
        } catch (e: Exception) {
            android.util.Log.e("Network", "Failed to start server: ${e.message}")
            return ""
        }

        // UDP
        udpDiscovery.startAnnouncing(gameCode, 55555)

        scope.launch {
            try {
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    handleClient(socket)
                }
            } catch (e: Exception) {
                if (isActive) android.util.Log.e("Network", "Server accept error: ${e.message}")
            }
        }
        return gameCode
    }

    override fun connectToGame(hostAddress: String, playerName: String) {
        disconnect()
        isHost = false
        val parts = hostAddress.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 55555

        scope.launch {
            try {
                delay(200)
                clientSocket = Socket(host, port)
                writer = PrintWriter(clientSocket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                
                sendRaw(GameMessage(type = "JOIN", playerId = myPlayerId, playerName = playerName))
                
                while (isActive) {
                    val line = reader.readLine() ?: break
                    _messages.emit(parseMessage(JSONObject(line)))
                }
            } catch (e: Exception) {
                if (isActive) {
                    android.util.Log.e("Network", "Connect error: ${e.message}")
                    _messages.emit(GameMessage(type = "ERROR", error = e.message))
                }
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
            } catch (e: Exception) {
                if (isActive) android.util.Log.e("Network", "Handle client error: ${e.message}")
            } finally {
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
            clientWriters.values.forEach { 
                try { it.println(json) } catch (_: Exception) {} 
            }
        }
    }

    override fun observeMessages(): Flow<GameMessage> = _messages.asSharedFlow()
    override fun getGameCode() = gameCode
    override fun getMyPlayerId() = myPlayerId

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun disconnect() {
        udpDiscovery.stop()
        
        managerJob.cancel() 
        managerJob = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + managerJob)

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        
        try {
            clientSocket?.close()
        } catch (_: Exception) {}
        clientSocket = null
        
        writer = null
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
            playerId = json.optStringOrNull("playerId"),
            playerName = json.optStringOrNull("playerName"),
            cardId = json.optStringOrNull("cardId"),
            againstCardId = json.optStringOrNull("againstCardId"),
            gameCode = json.optStringOrNull("gameCode"),
            playersJson = json.optStringOrNull("playersJson"),
            trumpSuit = json.optStringOrNull("trumpSuit"),
            trumpCardId = json.optStringOrNull("trumpCardId"),
            error = json.optStringOrNull("error")
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key) else null
    }
}