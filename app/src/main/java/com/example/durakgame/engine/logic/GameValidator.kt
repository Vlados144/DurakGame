package com.example.durakgame.engine.logic

import com.example.durakgame.engine.model.Card
import com.example.durakgame.engine.model.GameConfig
import com.example.durakgame.engine.model.GameState

object GameValidator {

    fun canAttack(gameState: GameState, playerId: String, card: Card): Boolean {
        if (gameState.phase != GameState.GamePhase.PLAYING) return false

        val player = gameState.players.find { it.id == playerId } ?: return false

        // Защищающийся не может атаковать
        if (player.id == gameState.currentDefender.id) return false

        // Карта должна быть у игрока
        if (!player.hand.contains(card)) return false

        // Первая карта — любая
        if (gameState.tableCards.isEmpty()) return true

        // Подкидывание — только того же достоинства
        val tableRanks = gameState.tableCards.flatMap { slot ->
            listOfNotNull(slot.attackingCard.rank, slot.defendingCard?.rank)
        }.toSet()

        if (card.rank !in tableRanks) return false

        // Лимит карт на столе — 6
        if (gameState.tableCards.size >= 6) return false

        // Защищающийся должен иметь возможность отбить
        val unresolvedCount = gameState.tableCards.count { !it.isDefended } + 1
        if (unresolvedCount > gameState.currentDefender.hand.size) return false

        return true
    }

    fun canDefend(
        gameState: GameState,
        playerId: String,
        defendingCard: Card,
        attackingCard: Card
    ): Boolean {
        if (gameState.phase != GameState.GamePhase.PLAYING) return false

        val player = gameState.players.find { it.id == playerId } ?: return false
        if (player.id != gameState.currentDefender.id) return false

        if (!player.hand.contains(defendingCard)) return false

        val tableSlot = gameState.tableCards.find { it.attackingCard == attackingCard } ?: return false
        if (tableSlot.isDefended) return false

        return defendingCard.beats(attackingCard, gameState.trumpSuit)
    }

    fun canTransfer(
        gameState: GameState,
        playerId: String,
        card: Card
    ): Boolean {
        // Перевод пока не поддерживается
        return false
    }

    fun canDeclareTake(gameState: GameState, playerId: String): Boolean {
        if (gameState.phase != GameState.GamePhase.PLAYING) return false
        val player = gameState.players.find { it.id == playerId } ?: return false
        return player.id == gameState.currentDefender.id
    }

    fun canPassAddingCards(gameState: GameState, playerId: String): Boolean {
        if (gameState.phase != GameState.GamePhase.PLAYING) return false
        val player = gameState.players.find { it.id == playerId } ?: return false
        if (player.id == gameState.currentDefender.id) return false
        return playerId !in gameState.playersConfirmedEnd
    }

    fun canEndTurn(gameState: GameState, playerId: String): Boolean {
        if (gameState.phase != GameState.GamePhase.PLAYING) return false
        if (gameState.tableCards.isEmpty()) return false
        val player = gameState.players.find { it.id == playerId } ?: return false
        if (player.id == gameState.currentDefender.id) return false
        return playerId !in gameState.playersConfirmedEnd
    }

    fun canEndRound(gameState: GameState): Boolean {
        if (gameState.phase != GameState.GamePhase.PLAYING) return false
        val nonDefenders = gameState.players.filter {
            it.id != gameState.currentDefender.id && it.hasCards
        }
        return nonDefenders.all { it.id in gameState.playersConfirmedEnd }
    }
}