package com.example.durakgame.engine.model

data class Player(
    val id: String,
    val name: String,
    val hand: MutableList<Card> = mutableListOf(),
    val isHost: Boolean = false
) {
    val cardCount: Int
        get() = hand.size

    val hasCards: Boolean
        get() = hand.isNotEmpty()

    fun removeCard(card: Card): Boolean {
        return hand.remove(card)
    }

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
    }

    fun sortHand(trumpSuit: Suit) {
        hand.sortWith(compareBy({ it.suit != trumpSuit }, { it.suit.ordinal }, { it.rank.value }))
    }
}