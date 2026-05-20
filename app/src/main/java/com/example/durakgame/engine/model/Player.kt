package com.example.durakgame.engine.model

data class Player(
    val id: String,
    val name: String,
    val hand: List<Card> = emptyList(),
    val isHost: Boolean = false,
    val avatarBase64: String? = null
) {
    val cardCount: Int
        get() = hand.size

    val hasCards: Boolean
        get() = hand.isNotEmpty()

    fun removeCard(cardId: String): Player {
        return copy(hand = hand.filter { it.id != cardId })
    }

    fun addCards(newCards: List<Card>): Player {
        return copy(hand = hand + newCards)
    }

    fun sortedHand(trumpSuit: Suit): Player {
        return copy(hand = hand.sortedWith(
            compareBy({ it.suit != trumpSuit }, { it.suit.ordinal }, { it.rank.value })
        ))
    }
}
