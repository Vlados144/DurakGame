package com.example.durakgame.engine.model

data class Card(
    val suit: Suit,
    val rank: Rank,
    val id: String = "${rank.value}_${suit.name}"
) {
    fun beats(other: Card, trumpSuit: Suit): Boolean {
        if (this.suit == other.suit) {
            return this.rank.value > other.rank.value
        }
        return this.suit == trumpSuit
    }

    fun isTrump(trumpSuit: Suit): Boolean {
        return this.suit == trumpSuit
    }

    override fun toString(): String {
        return "${rank.displayName}${suit.symbol}"
    }
}