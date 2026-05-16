package com.example.durakgame.engine.model

class Deck(internal val cards: MutableList<Card> = mutableListOf()) {
    companion object {
        fun fromCards(cards: List<Card>): Deck {
            val deck = Deck()
            deck.cards.clear()
            deck.cards.addAll(cards)
            return deck
        }
    }

    val size: Int
        get() = cards.size

    val isEmpty: Boolean
        get() = cards.isEmpty()

    val trumpSuit: Suit
        get() = if (cards.isNotEmpty()) cards.last().suit else Suit.SPADES

    val trumpCard: Card
        get() = if (cards.isNotEmpty()) cards.last() else Card(Suit.SPADES, Rank.SIX)

    init {
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                cards.add(Card(suit, rank))
            }
        }
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun draw(count: Int = 1): List<Card> {
        val drawn = mutableListOf<Card>()
        repeat(count) {
            if (cards.isNotEmpty()) {
                drawn.add(cards.removeAt(0))
            }
        }
        return drawn
    }

    fun peekTrump(): Card {
        return if (cards.isNotEmpty()) cards.last() else Card(Suit.SPADES, Rank.SIX)
    }
    fun setTrumpCard(card: Card) {
        // Убираем старый козырь
        if (cards.isNotEmpty()) {
            cards.removeAt(cards.lastIndex)
        }
        // Добавляем новый козырь
        cards.add(card)
    }
    fun getAllCards(): List<Card> {
        return cards.toList()
    }
}