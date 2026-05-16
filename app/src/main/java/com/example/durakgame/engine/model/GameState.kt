package com.example.durakgame.engine.model

data class GameState(
    val deck: Deck,
    val players: List<Player>,
    val config: GameConfig,
    val tableCards: List<TableSlot> = listOf(),  // ← было MutableList, стало List
    val currentAttackerIndex: Int = 0,
    val currentDefenderIndex: Int = 1,
    val phase: GamePhase = GamePhase.WAITING_FOR_PLAYERS,
    val roundNumber: Int = 0,
    val discardedPile: Int = 0,
    val playersConfirmedEnd: Set<String> = emptySet(),  // ← тоже заменить MutableSet на Set
    val playerWhoTookCards: String? = null,
    val lastTrumpSuit: Suit = Suit.SPADES
) {
    data class TableSlot(
        val attackingCard: Card,
        val defendingCard: Card? = null
    ) {
        val isDefended: Boolean
            get() = defendingCard != null
    }

    enum class GamePhase {
        WAITING_FOR_PLAYERS,
        PLAYING,
        ROUND_END,
        GAME_OVER
    }

    val currentAttacker: Player
        get() = players[currentAttackerIndex]

    val currentDefender: Player
        get() = players[currentDefenderIndex]

    val trumpSuit: Suit
        get() = if (deck.size > 0) deck.trumpSuit else lastTrumpSuit

    fun getNextPlayerIndex(fromIndex: Int): Int {
        var next = (fromIndex + 1) % players.size
        while (!players[next].hasCards && next != fromIndex) {
            next = (next + 1) % players.size
        }
        return next
    }

    fun getActivePlayers(): List<Player> {
        return players.filter { it.hasCards }
    }
}