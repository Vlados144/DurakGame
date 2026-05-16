package com.example.durakgame.engine.logic

import com.example.durakgame.engine.model.*

class GameEngine(private val config: GameConfig) {

    private var gameState: GameState = GameState(
        deck = Deck(),
        players = emptyList(),
        config = config
    )

    fun getState(): GameState = gameState

    fun addPlayer(name: String, isHost: Boolean = false): Player {
        val player = Player(
            id = generatePlayerId(),
            name = name,
            isHost = isHost
        )
        gameState = gameState.copy(
            players = gameState.players + player
        )
        return player
    }

    fun removePlayer(playerId: String) {
        gameState = gameState.copy(
            players = gameState.players.filter { it.id != playerId }
        )
    }

    fun startGame() {
        val deck = Deck()
        deck.shuffle()

        val playersWithCards = gameState.players.map { player ->
            player.copy(hand = deck.draw(6).toMutableList())
        }

        playersWithCards.forEach { it.sortHand(deck.trumpSuit) }

        val firstAttackerIndex = findFirstAttacker(playersWithCards, deck.trumpSuit)

        gameState = gameState.copy(
            deck = deck,
            players = playersWithCards,
            currentAttackerIndex = firstAttackerIndex,
            currentDefenderIndex = findNextPlayerWithCards(playersWithCards, firstAttackerIndex),
            phase = GameState.GamePhase.PLAYING,
            roundNumber = 1,
            tableCards = mutableListOf(),
            playersConfirmedEnd = mutableSetOf(),
            playerWhoTookCards = null,
            lastTrumpSuit = deck.trumpSuit
        )
    }
    fun setDeck(cards: List<Card>) {
        gameState = gameState.copy(
            deck = Deck.fromCards(cards)
        )
    }

    fun processAction(action: Action): Result<GameState> {
        return when (action) {
            is Action.Attack -> handleAttack(action)
            is Action.Defend -> handleDefend(action)
            is Action.Transfer -> handleTransfer(action)
            is Action.AddCard -> handleAddCard(action)
            is Action.DeclareTake -> handleDeclareTake(action)
            is Action.TakeCards -> handleTakeCards(action)
            is Action.EndTurn -> handleEndTurn(action)
            is Action.PassAdding -> handlePassAdding(action)
        }
    }

    private fun handleAttack(action: Action.Attack): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя атаковать"))
        }

        // Кто может атаковать: атакующий ИЛИ подкидывающие (если атакующий уже сходил)
        val isAttacker = action.playerId == gameState.currentAttacker.id
        val isAdding = gameState.tableCards.isNotEmpty() && action.playerId != gameState.currentDefender.id

        if (!isAttacker && !isAdding) {
            return Result.failure(IllegalStateException("Не ваша очередь атаковать"))
        }

        val player = gameState.players.find { it.id == action.playerId }
            ?: return Result.failure(IllegalStateException("Игрок не найден"))
        val card = player.hand.find { it.id == action.cardId }
            ?: return Result.failure(IllegalStateException("Карта не найдена"))

        // Проверка: первая карта любая, остальные того же достоинства
        if (gameState.tableCards.isNotEmpty()) {
            val tableRanks = gameState.tableCards.flatMap {
                listOfNotNull(it.attackingCard.rank, it.defendingCard?.rank)
            }.toSet()
            if (card.rank !in tableRanks) {
                return Result.failure(IllegalStateException("Можно подкидывать только карты того же достоинства"))
            }
        }

        player.removeCard(card)
        gameState = gameState.copy(
            tableCards = gameState.tableCards + GameState.TableSlot(attackingCard = card)
        )
        return Result.success(gameState)
    }

    private fun handleDefend(action: Action.Defend): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя защищаться"))
        }
        if (action.playerId != gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Только защищающийся может отбиваться"))
        }

        val defender = gameState.currentDefender
        val card = defender.hand.find { it.id == action.cardId }
            ?: return Result.failure(IllegalStateException("Карта не найдена в руке"))

        val slotIndex = gameState.tableCards.indexOfFirst { !it.isDefended }
        if (slotIndex == -1) return Result.failure(IllegalStateException("Нет неотбитых карт"))

        val slot = gameState.tableCards[slotIndex]
        if (!card.beats(slot.attackingCard, gameState.trumpSuit)) {
            return Result.failure(IllegalStateException("Не бьёт"))
        }

        defender.removeCard(card)
        val newTableCards = gameState.tableCards.toMutableList()
        newTableCards[slotIndex] = GameState.TableSlot(slot.attackingCard, card)

        gameState = gameState.copy(tableCards = newTableCards)

        return Result.success(gameState)
    }

    private fun handleTransfer(action: Action.Transfer): Result<GameState> {
        return Result.failure(IllegalStateException("Перевод временно не поддерживается"))
    }

    private fun handleAddCard(action: Action.AddCard): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя подкидывать"))
        }
        if (action.playerId == gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Защищающийся не может подкидывать"))
        }

        val player = gameState.players.find { it.id == action.playerId }
            ?: return Result.failure(IllegalStateException("Игрок не найден"))
        val card = player.hand.find { it.id == action.cardId }
            ?: return Result.failure(IllegalStateException("Карта не найдена"))

        val tableRanks = gameState.tableCards.flatMap {
            listOfNotNull(it.attackingCard.rank, it.defendingCard?.rank)
        }.toSet()
        if (card.rank !in tableRanks) {
            return Result.failure(IllegalStateException("Можно подкидывать только карты с достоинством на столе"))
        }

        player.removeCard(card)
        gameState = gameState.copy(
            tableCards = gameState.tableCards + GameState.TableSlot(attackingCard = card)
        )
        gameState = gameState.copy(playersConfirmedEnd = mutableSetOf())
        return Result.success(gameState)
    }

    private fun handleDeclareTake(action: Action.DeclareTake): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя взять карты"))
        }
        if (action.playerId != gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Только защищающийся может взять"))
        }

        val defender = gameState.currentDefender
        val allCards = gameState.tableCards.flatMap { listOfNotNull(it.attackingCard, it.defendingCard) }
        defender.addCards(allCards)
        defender.sortHand(gameState.trumpSuit)
        gameState = gameState.copy(tableCards = listOf())
        drawCardsAfterRound()

        return nextRound(defenderTookCards = true)
    }

    private fun handleTakeCards(action: Action.TakeCards): Result<GameState> {
        val defender = gameState.currentDefender
        val allCards = gameState.tableCards.flatMap { listOfNotNull(it.attackingCard, it.defendingCard) }
        defender.addCards(allCards)
        defender.sortHand(gameState.trumpSuit)
        gameState = gameState.copy(tableCards = listOf())
        drawCardsAfterRound()
        return nextRound()
    }

    private fun handleEndTurn(action: Action.EndTurn): Result<GameState> {
        android.util.Log.d("GameEngine", "handleEndTurn: playerId=${action.playerId}, phase=${gameState.phase}")
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя завершить ход"))
        }
        if (action.playerId == gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Защищающийся не может нажать Бито"))
        }
        if (gameState.tableCards.isEmpty()) {
            return Result.failure(IllegalStateException("Нет карт на столе"))
        }

        // Проверяем, что все карты отбиты
        if (gameState.tableCards.any { !it.isDefended }) {
            return Result.failure(IllegalStateException("Не все карты отбиты"))
        }

        gameState = gameState.copy(
            playersConfirmedEnd = gameState.playersConfirmedEnd + action.playerId
        )

        if (allNonDefendersConfirmed()) {
            android.util.Log.d("GameEngine", "allNonDefendersConfirmed, вызываю handleRoundEnd")
            return handleRoundEnd()
        }
        return Result.success(gameState)
    }

    private fun allNonDefendersConfirmed(): Boolean {
        val nonDefenders = gameState.players.filter {
            it.id != gameState.currentDefender.id && it.hasCards
        }
        return nonDefenders.all { it.id in gameState.playersConfirmedEnd }
    }

    private fun handlePassAdding(action: Action.PassAdding): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя пасовать"))
        }
        if (action.playerId == gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Защищающийся не пасует"))
        }
        gameState = gameState.copy(
            playersConfirmedEnd = gameState.playersConfirmedEnd + action.playerId
        )
        if (allNonDefendersConfirmed()) {
            return handleRoundEnd()
        }
        return Result.success(gameState)
    }

    private fun getRequiredConfirmations(): Int {
        val defenderId = gameState.currentDefender.id
        val playerWhoTookId = gameState.playerWhoTookCards

        if (playerWhoTookId != null) {
            return gameState.getActivePlayers().count {
                it.id != playerWhoTookId
            }
        }

        return gameState.getActivePlayers().count { it.id != defenderId }
    }

    private fun handleRoundEnd(): Result<GameState> {
        gameState = gameState.copy(tableCards = listOf())
        drawCardsAfterRound()
        return nextRound(defenderTookCards = false)
    }

    private fun nextRound(defenderTookCards: Boolean = false): Result<GameState> {
        if (checkGameOver()) {
            gameState = gameState.copy(phase = GameState.GamePhase.GAME_OVER)
            return Result.success(gameState)
        }

        val nextAttacker: Int
        val nextDefender: Int

        if (defenderTookCards) {
            // Защищающийся взял карты — пропускает ход
            // Атакующим становится тот, кто после защищающегося
            nextAttacker = gameState.getNextPlayerIndex(gameState.currentDefenderIndex)
            nextDefender = gameState.getNextPlayerIndex(nextAttacker)
        } else {
            // Защищающийся отбился — становится атакующим
            nextAttacker = gameState.currentDefenderIndex
            nextDefender = gameState.getNextPlayerIndex(nextAttacker)
        }

        gameState = gameState.copy(
            currentAttackerIndex = nextAttacker,
            currentDefenderIndex = nextDefender,
            phase = GameState.GamePhase.PLAYING,
            roundNumber = gameState.roundNumber + 1,
            playersConfirmedEnd = emptySet(),
            playerWhoTookCards = null
        )
        return Result.success(gameState)
    }

    private fun drawCardsAfterRound() {
        if (gameState.deck.isEmpty) return

        val drawOrder = getDrawOrder()
        for (playerIndex in drawOrder) {
            val player = gameState.players[playerIndex]
            if (!player.hasCards) continue

            val cardsToDraw = 6 - player.hand.size
            if (cardsToDraw > 0) {
                val drawnCards = gameState.deck.draw(cardsToDraw)
                player.addCards(drawnCards)
                player.sortHand(gameState.trumpSuit)
            }
        }
    }

    private fun getDrawOrder(): List<Int> {
        val order = mutableListOf<Int>()
        var current = gameState.currentAttackerIndex
        repeat(gameState.players.size) {
            order.add(current)
            current = (current + 1) % gameState.players.size
        }
        return order
    }

    private fun checkGameOver(): Boolean {
        val activePlayers = gameState.getActivePlayers()
        if (activePlayers.size <= 1) return true
        if (gameState.deck.isEmpty) {
            return activePlayers.size <= 1
        }
        return false
    }

    fun getGameResult(): GameResult {
        val playersWithoutCards = gameState.players.filter { !it.hasCards }
        val playersWithCards = gameState.players.filter { it.hasCards }

        return GameResult(
            winners = playersWithoutCards,
            loser = playersWithCards.firstOrNull(),
            finalState = gameState
        )
    }

    private fun findFirstAttacker(players: List<Player>, trumpSuit: Suit): Int {
        var lowestTrumpIndex = -1
        var lowestTrumpRank: Rank? = null

        for ((index, player) in players.withIndex()) {
            for (card in player.hand) {
                if (card.suit == trumpSuit) {
                    if (lowestTrumpIndex == -1 || card.rank.value < lowestTrumpRank!!.value) {
                        lowestTrumpIndex = index
                        lowestTrumpRank = card.rank
                    }
                }
            }
        }

        if (lowestTrumpIndex != -1) return lowestTrumpIndex
        return 0
    }

    private fun findNextPlayerWithCards(players: List<Player>, fromIndex: Int): Int {
        var next = (fromIndex + 1) % players.size
        while (!players[next].hasCards && next != fromIndex) {
            next = (next + 1) % players.size
        }
        return next
    }

    private fun generatePlayerId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    private fun getPlayer(playerId: String): Player {
        return gameState.players.find { it.id == playerId }
            ?: throw IllegalArgumentException("Игрок с ID $playerId не найден")
    }

    private fun findCardInHand(playerId: String, cardId: String): Card? {
        val player = gameState.players.find { it.id == playerId } ?: return null
        return player.hand.find { it.id == cardId }
    }
}

sealed class Action {
    abstract val playerId: String

    data class Attack(
        override val playerId: String,
        val cardId: String
    ) : Action()

    data class Defend(
        override val playerId: String,
        val cardId: String,
        val againstCardId: String
    ) : Action()

    data class Transfer(
        override val playerId: String,
        val cardId: String
    ) : Action()

    data class AddCard(
        override val playerId: String,
        val cardId: String
    ) : Action()

    data class DeclareTake(
        override val playerId: String
    ) : Action()

    data class TakeCards(
        override val playerId: String
    ) : Action()

    data class EndTurn(
        override val playerId: String
    ) : Action()

    data class PassAdding(
        override val playerId: String
    ) : Action()
}

data class GameResult(
    val winners: List<Player>,
    val loser: Player?,
    val finalState: GameState
)