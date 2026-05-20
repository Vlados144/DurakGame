package com.example.durakgame.engine.logic

import com.example.durakgame.engine.model.*

sealed class Action {
    abstract val playerId: String
    data class Attack(override val playerId: String, val cardId: String) : Action()
    data class Defend(override val playerId: String, val cardId: String, val againstCardId: String) : Action()
    data class Transfer(override val playerId: String, val cardId: String) : Action()
    data class AddCard(override val playerId: String, val cardId: String) : Action()
    data class DeclareTake(override val playerId: String) : Action()
    data class TakeCards(override val playerId: String) : Action()
    data class EndTurn(override val playerId: String) : Action()
    data class PassAdding(override val playerId: String) : Action()
}

data class GameResult(
    val winners: List<Player>,
    val loser: Player?,
    val finalState: GameState
)

class GameEngine(private val config: GameConfig) {

    private var gameState: GameState = GameState(
        deck = Deck(),
        players = emptyList(),
        config = config
    )

    fun getState(): GameState = gameState

    fun addPlayer(name: String, isHost: Boolean = false, avatarBase64: String? = null): Player {
        val player = Player(
            id = generatePlayerId(),
            name = name,
            isHost = isHost,
            avatarBase64 = avatarBase64
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
            player.copy(hand = deck.draw(6))
        }.map { it.sortedHand(deck.trumpSuit) }

        val firstAttackerIndex = findFirstAttacker(playersWithCards, deck.trumpSuit)

        gameState = gameState.copy(
            deck = deck,
            players = playersWithCards,
            currentAttackerIndex = firstAttackerIndex,
            currentDefenderIndex = findNextPlayerWithCards(playersWithCards, firstAttackerIndex),
            phase = GameState.GamePhase.PLAYING,
            roundNumber = 1,
            tableCards = emptyList(),
            playersConfirmedEnd = emptySet(),
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

    private fun updatePlayer(playerId: String, transform: (Player) -> Player) {
        gameState = gameState.copy(
            players = gameState.players.map { if (it.id == playerId) transform(it) else it }
        )
    }

    private fun handleAttack(action: Action.Attack): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя атаковать"))
        }

        val isAttacker = action.playerId == gameState.currentAttacker.id
        val isAdding = gameState.tableCards.isNotEmpty() && action.playerId != gameState.currentDefender.id

        if (!isAttacker && !isAdding) {
            return Result.failure(IllegalStateException("Не ваша очередь атаковать"))
        }

        val player = gameState.players.find { it.id == action.playerId }
            ?: return Result.failure(IllegalStateException("Игрок не найден"))
        val card = player.hand.find { it.id == action.cardId }
            ?: return Result.failure(IllegalStateException("Карта не найдена"))

        val defender = gameState.currentDefender
        val unbeatenCount = gameState.tableCards.count { !it.isDefended }
        if (unbeatenCount + 1 > defender.hand.size) {
            return Result.failure(IllegalStateException("У защищающегося недостаточно карт"))
        }

        if (gameState.tableCards.isNotEmpty()) {
            val tableRanks = gameState.tableCards.flatMap {
                listOfNotNull(it.attackingCard.rank, it.defendingCard?.rank)
            }.toSet()
            if (card.rank !in tableRanks) {
                return Result.failure(IllegalStateException("Можно подкидывать только карты того же достоинства"))
            }
        }

        updatePlayer(player.id) { it.removeCard(card.id) }
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

        // Ищем слот именно с той картой, которую игрок хочет побить
        val slotIndex = gameState.tableCards.indexOfFirst { 
            it.attackingCard.id == action.againstCardId && !it.isDefended 
        }
        
        if (slotIndex == -1) return Result.failure(IllegalStateException("Эта карта уже отбита или не найдена"))

        val slot = gameState.tableCards[slotIndex]
        if (!card.beats(slot.attackingCard, gameState.trumpSuit)) {
            return Result.failure(IllegalStateException("Не бьёт"))
        }

        updatePlayer(defender.id) { it.removeCard(card.id) }
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

        val defender = gameState.currentDefender
        val unbeatenCount = gameState.tableCards.count { !it.isDefended }
        if (unbeatenCount + 1 > defender.hand.size) {
            return Result.failure(IllegalStateException("У защищающегося недостаточно карт"))
        }

        val tableRanks = gameState.tableCards.flatMap {
            listOfNotNull(it.attackingCard.rank, it.defendingCard?.rank)
        }.toSet()
        if (card.rank !in tableRanks) {
            return Result.failure(IllegalStateException("Можно подкидывать только карты с достоинством на столе"))
        }

        updatePlayer(player.id) { it.removeCard(card.id) }
        gameState = gameState.copy(
            tableCards = gameState.tableCards + GameState.TableSlot(attackingCard = card),
            playersConfirmedEnd = emptySet()
        )
        return Result.success(gameState)
    }

    private fun handleDeclareTake(action: Action.DeclareTake): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя взять карты"))
        }
        if (action.playerId != gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Только защищающийся может взять"))
        }

        val defenderId = gameState.currentDefender.id
        val allCards = gameState.tableCards.flatMap { listOfNotNull(it.attackingCard, it.defendingCard) }
        
        updatePlayer(defenderId) { 
            it.addCards(allCards).sortedHand(gameState.trumpSuit)
        }
        
        gameState = gameState.copy(tableCards = emptyList())
        drawCardsAfterRound()

        return nextRound(defenderTookCards = true)
    }

    private fun handleTakeCards(action: Action.TakeCards): Result<GameState> {
        val defenderId = gameState.currentDefender.id
        val allCards = gameState.tableCards.flatMap { listOfNotNull(it.attackingCard, it.defendingCard) }
        
        updatePlayer(defenderId) {
            it.addCards(allCards).sortedHand(gameState.trumpSuit)
        }
        
        gameState = gameState.copy(tableCards = emptyList())
        drawCardsAfterRound()
        return nextRound()
    }

    private fun handleEndTurn(action: Action.EndTurn): Result<GameState> {
        if (gameState.phase != GameState.GamePhase.PLAYING) {
            return Result.failure(IllegalStateException("Нельзя завершить ход"))
        }
        if (action.playerId == gameState.currentDefender.id) {
            return Result.failure(IllegalStateException("Защищающийся не может нажать Бито"))
        }
        if (gameState.tableCards.isEmpty()) {
            return Result.failure(IllegalStateException("Нет карт на столе"))
        }

        if (gameState.tableCards.any { !it.isDefended }) {
            return Result.failure(IllegalStateException("Не все карты отбиты"))
        }

        gameState = gameState.copy(
            playersConfirmedEnd = gameState.playersConfirmedEnd + action.playerId
        )

        if (allNonDefendersConfirmed()) {
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

    private fun handleRoundEnd(): Result<GameState> {
        gameState = gameState.copy(tableCards = emptyList())
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
            nextAttacker = gameState.getNextPlayerIndex(gameState.currentDefenderIndex)
            nextDefender = gameState.getNextPlayerIndex(nextAttacker)
        } else {
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
            
            val cardsToDraw = 6 - player.hand.size
            if (cardsToDraw > 0 && !gameState.deck.isEmpty) {
                val drawnCards = gameState.deck.draw(cardsToDraw)
                updatePlayer(player.id) {
                    it.addCards(drawnCards).sortedHand(gameState.trumpSuit)
                }
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
        if (!gameState.deck.isEmpty) return false
        
        val playersWithCards = gameState.players.count { it.hasCards }
        return playersWithCards <= 1
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
        var checked = 0
        while (!players[next].hasCards && checked < players.size) {
            next = (next + 1) % players.size
            checked++
        }
        return next
    }

    private fun generatePlayerId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
