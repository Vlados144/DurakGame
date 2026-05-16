package com.example.durakgame.network

import com.example.durakgame.engine.model.GameConfig
import kotlinx.coroutines.flow.Flow

interface GameNetworkManager {
    fun startHost(config: GameConfig, playerName: String): String  // возвращает код игры
    fun connectToGame(hostAddress: String, playerName: String)
    fun disconnect()
    fun sendAction(action: GameMessage)
    fun observeMessages(): Flow<GameMessage>
    fun getGameCode(): String
    fun getMyPlayerId(): String
}