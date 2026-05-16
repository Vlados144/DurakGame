package com.example.durakgame.network

data class GameMessage(
    val type: String,
    val playerId: String? = null,
    val playerName: String? = null,
    val cardId: String? = null,
    val againstCardId: String? = null,
    val gameCode: String? = null,
    val playersJson: String? = null,
    val trumpSuit: String? = null,
    val trumpCardId: String? = null,
    val error: String? = null
)

data class PlayerInfo(
    val id: String,
    val name: String,
    val cardCount: Int,
    val isHost: Boolean
)