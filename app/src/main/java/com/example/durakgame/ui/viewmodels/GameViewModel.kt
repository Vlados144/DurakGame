package com.example.durakgame.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.durakgame.engine.logic.Action
import com.example.durakgame.engine.logic.GameEngine
import com.example.durakgame.engine.model.*
import com.example.durakgame.network.GameMessage
import com.example.durakgame.network.LocalNetworkManager
import com.example.durakgame.network.NsdHelper
import com.example.durakgame.network.PlayerInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class GameViewModel(private val nsdHelper: NsdHelper? = null) : ViewModel() {

    private var engine: GameEngine? = null
    private val networkManager = LocalNetworkManager()
    private var myPlayerId = ""
    private var hostMode = false
    private var playerName = ""
    //
    private var rematchCount = 0
    private val _rematchStatus = MutableStateFlow("0/2")
    val rematchStatus: StateFlow<String> = _rematchStatus.asStateFlow()
    private val _playerLeft = MutableStateFlow(false)
    val playerLeft: StateFlow<Boolean> = _playerLeft.asStateFlow()
    //
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState = _gameState.asStateFlow()

    private val _selectedCardId = MutableStateFlow<String?>(null)
    val selectedCardId = _selectedCardId.asStateFlow()

    private val _players = MutableStateFlow<List<PlayerInfo>>(emptyList())
    val players = _players.asStateFlow()

    private val _gameCode = MutableStateFlow("")
    val gameCode = _gameCode.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var gameConfig: GameConfig = GameConfig()


    init {
        observeNetworkMessages()
    }
    fun getNetworkManager(): LocalNetworkManager = networkManager
    fun hostGame(config: GameConfig, name: String) {
        gameConfig = config
        hostMode = true
        playerName = name
        engine = GameEngine(config)
        val player = engine!!.addPlayer(name, isHost = true)
        myPlayerId = player.id
        val code = networkManager.startHost(config, name)
        _gameCode.value = code
        _players.value = listOf(PlayerInfo(player.id, player.name, 0, true))
    }

    fun getBetAmount(): Long = gameConfig.betAmount

    fun joinGame(hostAddress: String, name: String) {
        hostMode = false
        playerName = name
        networkManager.connectToGame(hostAddress, name)
    }



    fun requestRematch() {
        if (hostMode) {
            rematchCount++
            val total = _players.value.size
            val status = "$rematchCount/$total"
            _rematchStatus.value = status
            networkManager.sendAction(GameMessage(type = "REMATCH_UPDATE", playersJson = status))

            if (rematchCount >= total) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startNewGame()
                }, 1000)
            }
        } else {
            networkManager.sendAction(GameMessage(type = "REMATCH_REQUEST", playerId = myPlayerId))
        }
    }

    fun cancelRematch() {
        if (hostMode) {
            if (rematchCount > 0) rematchCount--
            val total = _players.value.size
            val status = "$rematchCount/$total"
            _rematchStatus.value = status
            networkManager.sendAction(GameMessage(type = "REMATCH_UPDATE", playersJson = status))
        } else {
            networkManager.sendAction(GameMessage(type = "REMATCH_CANCEL", playerId = myPlayerId))
        }
    }

    fun playerLeft() {
        networkManager.sendAction(GameMessage(type = "PLAYER_LEFT", playerId = myPlayerId))
    }

    private fun startNewGame() {
        if (hostMode) {
            rematchCount = 0

            engine = GameEngine(gameConfig)

            // Добавляем ВСЕХ игроков, которые были в предыдущей игре
            val previousPlayers = _players.value
            for (p in previousPlayers) {
                engine!!.addPlayer(p.name, p.isHost)
                if (p.name == playerName) {
                    myPlayerId = engine!!.getState().players.last().id
                }
            }

            engine!!.startGame()
            _gameState.value = engine!!.getState()
            _rematchStatus.value = ""

            // Обновляем список игроков
            _players.value = engine!!.getState().players.map {
                PlayerInfo(it.id, it.name, it.hand.size, it.isHost)
            }

            // Отправляем START_GAME с полным списком игроков
            networkManager.sendAction(
                GameMessage(
                    type = "START_GAME",
                    playersJson = playersToJson(engine!!.getState().players),
                    trumpSuit = engine!!.getState().trumpSuit.name,
                    trumpCardId = engine!!.getState().deck.peekTrump().id
                )
            )

            sendStateUpdate()
        }
    }

    fun startGame() {
        if (hostMode && engine != null) {
            engine!!.startGame()
            _gameState.value = engine!!.getState()

            val playersJson = playersToJson(engine!!.getState().players)
            val trumpCard = engine!!.getState().deck.peekTrump()
            networkManager.sendAction(
                GameMessage(
                    type = "START_GAME",
                    playersJson = playersJson,
                    trumpSuit = trumpCard.suit.name,
                    trumpCardId = trumpCard.id
                )
            )

            sendStateUpdate()
        }
    }
    fun exitGame() {
        _gameState.value = _gameState.value?.copy(phase = GameState.GamePhase.GAME_OVER)
        _playerLeft.value = true

        // Отправляем PLAYER_LEFT
        networkManager.sendAction(GameMessage(type = "PLAYER_LEFT", playerId = myPlayerId))

        // Даём время на отправку, потом отключаемся
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            networkManager.disconnect()
        }, 500)
    }

    fun selectCard(cardId: String?) {
        _selectedCardId.value = cardId
    }

    fun playCard(cardId: String) {
        val state = _gameState.value ?: return

        val action = when {
            state.currentDefender.id != myPlayerId -> Action.Attack(myPlayerId, cardId)
            state.currentDefender.id == myPlayerId -> {
                val against = state.tableCards.find { !it.isDefended }?.attackingCard?.id
                if (against != null) Action.Defend(myPlayerId, cardId, against) else null
            }
            else -> null
        }

        if (action != null) {
            if (hostMode) {
                val result = engine?.processAction(action)
                if (result?.isSuccess == true) {
                    val s = engine?.getState() ?: return
                    _gameState.value = s.copy(tableCards = s.tableCards.toList())
                    sendStateUpdate()
                }
            } else {
                val msg = when (action) {
                    is Action.Attack -> GameMessage("ATTACK", myPlayerId, cardId = action.cardId)
                    is Action.Defend -> GameMessage("DEFEND", myPlayerId, cardId = action.cardId, againstCardId = action.againstCardId)
                    else -> null
                }
                msg?.let { networkManager.sendAction(it) }
            }
        }
        _selectedCardId.value = null
    }

    fun declareTake() {
        android.util.Log.d("GameViewModel", "declareTake: hostMode=$hostMode")
        if (hostMode) {
            val result = engine?.processAction(Action.DeclareTake(myPlayerId))
            android.util.Log.d("GameViewModel", "declareTake result: ${result?.isSuccess}")
            if (result?.isSuccess == true) {
                val s = engine?.getState() ?: return
                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                sendStateUpdate()
            }
        } else {
            networkManager.sendAction(GameMessage("DECLARE_TAKE", myPlayerId))
        }
    }

    fun endTurn() {
        if (hostMode) {
            val result = engine?.processAction(Action.EndTurn(myPlayerId))
            if (result?.isSuccess == true) {
                val s = engine?.getState() ?: return
                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                sendStateUpdate()
            }
        } else {
            networkManager.sendAction(GameMessage("END_TURN", myPlayerId))
        }
    }

    fun passAdding() {
        if (hostMode) {
            val result = engine?.processAction(Action.PassAdding(myPlayerId))
            if (result?.isSuccess == true) {
                val s = engine?.getState() ?: return
                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                sendStateUpdate()
            }
        } else {
            networkManager.sendAction(GameMessage("PASS_ADDING", myPlayerId))
        }
    }

    fun leaveGame() {
        networkManager.disconnect()
        engine = null
        _gameState.value = null
        _players.value = emptyList()
        _isConnected.value = false
        hostMode = false
        rematchCount = 0
        _rematchStatus.value = ""
        _playerLeft.value = false
    }

    fun isHost() = hostMode
    fun getMyPlayerId() = myPlayerId

    override fun onCleared() { super.onCleared() }

    private fun sendStateUpdate() {
        val state = engine?.getState() ?: return
        android.util.Log.d("GameViewModel", "Отправка STATE_UPDATE: players=${state.players.map { "${it.name}:${it.hand.size}" }}")
        _gameState.value = state.copy(tableCards = state.tableCards.toList())

        val json = JSONObject()
        json.put("players", JSONArray(playersToJson(state.players)))
        json.put("phase", state.phase.name)
        json.put("attackerIndex", state.currentAttackerIndex)
        json.put("defenderIndex", state.currentDefenderIndex)
        json.put("trumpSuit", state.trumpSuit.name)
        json.put("deckSize", state.deck.size)
        json.put("lastTrumpSuit", state.lastTrumpSuit.name)

        // Карты колоды — только если не пуста
        if (state.deck.size > 0) {
            val deckArr = JSONArray()
            for (card in state.deck.getAllCards()) {
                val cardObj = JSONObject()
                cardObj.put("suit", card.suit.name)
                cardObj.put("rank", card.rank.name)
                cardObj.put("id", card.id)
                deckArr.put(cardObj)
            }
            json.put("deckCards", deckArr)

            val trumpCard = state.deck.peekTrump()
            val trumpObj = JSONObject()
            trumpObj.put("suit", trumpCard.suit.name)
            trumpObj.put("rank", trumpCard.rank.name)
            trumpObj.put("id", trumpCard.id)
            json.put("trumpCard", trumpObj)
        } else {
            json.put("trumpSuit", "")  // пустой козырь
        }

        // Карты на столе
        val tableArr = JSONArray()
        for (slot in state.tableCards) {
            val slotObj = JSONObject()
            slotObj.put("attackingSuit", slot.attackingCard.suit.name)
            slotObj.put("attackingRank", slot.attackingCard.rank.name)
            slotObj.put("attackingId", slot.attackingCard.id)
            slot.defendingCard?.let {
                slotObj.put("defendingSuit", it.suit.name)
                slotObj.put("defendingRank", it.rank.name)
                slotObj.put("defendingId", it.id)
            }
            tableArr.put(slotObj)
        }
        json.put("tableCards", tableArr)

        networkManager.sendAction(GameMessage(type = "STATE_UPDATE", playersJson = json.toString()))
    }

    private fun updateStateFromJson(json: String) {
        val obj = JSONObject(json)
        val playersArr = obj.getJSONArray("players")
        val players = mutableListOf<Player>()

        for (i in 0 until playersArr.length()) {
            val pObj = playersArr.getJSONObject(i)
            val player = Player(
                id = pObj.getString("id"),
                name = pObj.getString("name"),
                isHost = pObj.optBoolean("isHost", false)
            )
            val cardsArr = pObj.optJSONArray("cards")
            if (cardsArr != null) {
                for (j in 0 until cardsArr.length()) {
                    val cObj = cardsArr.getJSONObject(j)
                    player.hand.add(Card(
                        suit = Suit.valueOf(cObj.getString("suit")),
                        rank = Rank.valueOf(cObj.getString("rank"))
                    ))
                }
            }
            players.add(player)
        }

        // Колода — только если есть в JSON
        val deck: Deck
        val deckCardsArr = obj.optJSONArray("deckCards")
        if (deckCardsArr != null && deckCardsArr.length() > 0) {
            val deckCards = mutableListOf<Card>()
            for (i in 0 until deckCardsArr.length()) {
                val cObj = deckCardsArr.getJSONObject(i)
                deckCards.add(Card(
                    suit = Suit.valueOf(cObj.getString("suit")),
                    rank = Rank.valueOf(cObj.getString("rank"))
                ))
            }
            deck = Deck.fromCards(deckCards)

            val trumpObj = obj.optJSONObject("trumpCard")
            if (trumpObj != null) {
                deck.setTrumpCard(Card(
                    suit = Suit.valueOf(trumpObj.getString("suit")),
                    rank = Rank.valueOf(trumpObj.getString("rank"))
                ))
            }
        } else {
            // Колода пуста или не передана — сохраняем старую
            deck = Deck.fromCards(emptyList())
        }

        val tableCards = mutableListOf<GameState.TableSlot>()
        val tableArr = obj.optJSONArray("tableCards")
        if (tableArr != null) {
            for (i in 0 until tableArr.length()) {
                val slotObj = tableArr.getJSONObject(i)
                val attackingCard = Card(
                    suit = Suit.valueOf(slotObj.getString("attackingSuit")),
                    rank = Rank.valueOf(slotObj.getString("attackingRank"))
                )
                val defendingCard = if (slotObj.has("defendingSuit")) {
                    Card(
                        suit = Suit.valueOf(slotObj.getString("defendingSuit")),
                        rank = Rank.valueOf(slotObj.getString("defendingRank"))
                    )
                } else null
                tableCards.add(GameState.TableSlot(attackingCard, defendingCard))
            }
        }

        val phase = GameState.GamePhase.valueOf(obj.getString("phase"))
        val attackerIndex = obj.getInt("attackerIndex")
        val defenderIndex = obj.getInt("defenderIndex")
        val lastTrumpSuit = Suit.valueOf(obj.optString("lastTrumpSuit", "SPADES"))
        _gameState.value = GameState(
            deck = deck,
            players = players,
            config = _gameState.value?.config ?: GameConfig(),
            currentAttackerIndex = attackerIndex,
            currentDefenderIndex = defenderIndex,
            phase = phase,
            tableCards = tableCards,
            lastTrumpSuit = lastTrumpSuit
        )
        android.util.Log.d("GameViewModel", "updateState: players=${players.map { "${it.name}:${it.hand.size}" }}")
    }

    private fun observeNetworkMessages() {
        viewModelScope.launch {
            networkManager.observeMessages().collect { msg ->
                when (msg.type) {
                    "JOIN" -> {
                        if (hostMode) {
                            val newPlayer = engine?.addPlayer(msg.playerName ?: "Гость")
                            refreshPlayers()
                            val correctId = newPlayer?.id ?: msg.playerId
                            networkManager.sendAction(
                                GameMessage(
                                    type = "PLAYER_ID_ASSIGNED",
                                    playerId = msg.playerId,
                                    gameCode = correctId
                                )
                            )
                            val list = engine?.getState()?.players ?: emptyList()
                            networkManager.sendAction(GameMessage("PLAYERS_UPDATE", playersJson = playersToJson(list)))
                        }
                    }

                    "JOINED" -> {
                        myPlayerId = msg.playerId ?: myPlayerId
                        _gameCode.value = msg.gameCode ?: ""
                        _isConnected.value = true
                    }

                    "PLAYER_ID_ASSIGNED" -> {
                        val newId = msg.gameCode
                        if (newId != null && myPlayerId == msg.playerId) {
                            myPlayerId = newId
                        }
                    }

                    "PLAYERS_UPDATE" -> {
                        msg.playersJson?.let { _players.value = parsePlayers(it) }
                    }

                    "START_GAME" -> {
                        android.util.Log.d("GameViewModel", "START_GAME получен, hostMode=$hostMode")
                        if (!hostMode) {
                            val pl = msg.playersJson?.let { parsePlayers(it) } ?: emptyList()
                            _players.value = pl
                            val me = pl.find { it.name == playerName }
                            if (me != null) myPlayerId = me.id
                            _gameState.value = GameState(
                                deck = Deck(),
                                players = pl.map { p -> Player(p.id, p.name, isHost = p.isHost) },
                                config = GameConfig(),
                                phase = GameState.GamePhase.PLAYING
                            )
                            _rematchStatus.value = ""
                            _playerLeft.value = false
                        }
                    }

                    "STATE_UPDATE" -> {
                        if (!hostMode) {
                            msg.playersJson?.let { json -> updateStateFromJson(json) }
                        }
                    }

                    "ATTACK" -> {
                        if (hostMode && msg.playerId != myPlayerId) {
                            val result = engine?.processAction(Action.Attack(msg.playerId!!, msg.cardId!!))
                            if (result?.isSuccess == true) {
                                val s = engine?.getState() ?: return@collect
                                android.util.Log.d("GameViewModel", "После ATTACK: players=${s.players.map { "${it.name}:${it.hand.size}" }}")
                                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                                sendStateUpdate()
                            }
                        }
                    }

                    "DEFEND" -> {
                        if (hostMode && msg.playerId != myPlayerId) {
                            val result = engine?.processAction(Action.Defend(msg.playerId!!, msg.cardId!!, msg.againstCardId!!))
                            if (result?.isSuccess == true) {
                                val s = engine?.getState() ?: return@collect
                                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                                sendStateUpdate()
                            }
                        }
                    }

                    "ADD_CARD" -> {
                        if (hostMode && msg.playerId != myPlayerId) {
                            val result = engine?.processAction(Action.AddCard(msg.playerId!!, msg.cardId!!))
                            if (result?.isSuccess == true) {
                                val s = engine?.getState() ?: return@collect
                                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                                sendStateUpdate()
                            }
                        }
                    }

                    "DECLARE_TAKE" -> {
                        if (hostMode && msg.playerId != myPlayerId) {
                            val result = engine?.processAction(Action.DeclareTake(msg.playerId!!))
                            if (result?.isSuccess == true) {
                                val s = engine?.getState() ?: return@collect
                                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                                sendStateUpdate()
                            }
                        }
                    }

                    "PASS_ADDING" -> {
                        if (hostMode && msg.playerId != myPlayerId) {
                            val result = engine?.processAction(Action.PassAdding(msg.playerId!!))
                            if (result?.isSuccess == true) {
                                val s = engine?.getState() ?: return@collect
                                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                                sendStateUpdate()
                            }
                        }
                    }

                    "END_TURN" -> {
                        if (hostMode && msg.playerId != myPlayerId) {
                            android.util.Log.d("GameViewModel", "Хост получил END_TURN от клиента")
                            val result = engine?.processAction(Action.EndTurn(msg.playerId!!))
                            android.util.Log.d("GameViewModel", "END_TURN result: ${result?.isSuccess}")
                            if (result?.isSuccess == true) {
                                val s = engine?.getState() ?: return@collect
                                android.util.Log.d("GameViewModel", "После END_TURN: attacker=${s.currentAttackerIndex}, defender=${s.currentDefenderIndex}")
                                _gameState.value = s.copy(tableCards = s.tableCards.toList())
                                sendStateUpdate()
                            }
                        }
                    }
                    "REMATCH_REQUEST" -> {
                        if (hostMode) {
                            rematchCount++
                            val total = _players.value.size
                            val status = "$rematchCount/$total"
                            android.util.Log.d("GameViewModel", "Реванш: $status")
                            _rematchStatus.value = status
                            networkManager.sendAction(GameMessage(type = "REMATCH_UPDATE", playersJson = status))

                            if (rematchCount >= total) {
                                android.util.Log.d("GameViewModel", "Все готовы, запуск новой игры")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    startNewGame()
                                }, 1000)
                            }
                        }
                    }

                    "REMATCH_CANCEL" -> {
                        if (hostMode) {
                            if (rematchCount > 0) rematchCount--
                            val total = _players.value.size
                            val status = "$rematchCount/$total"
                            _rematchStatus.value = status
                            networkManager.sendAction(GameMessage(type = "REMATCH_UPDATE", playersJson = status))
                        }
                    }

                    "REMATCH_UPDATE" -> {
                        val status = msg.playersJson ?: "0/2"
                        android.util.Log.d("GameViewModel", "REMATCH_UPDATE: $status")
                        _rematchStatus.value = status
                    }

                    "PLAYER_LEFT" -> {
                        android.util.Log.d("GameViewModel", "PLAYER_LEFT получен")
                        _playerLeft.value = true

                        val oldState = _gameState.value
                        if (oldState != null) {
                            val newState = oldState.copy(phase = GameState.GamePhase.GAME_OVER, tableCards = listOf())
                            _gameState.value = newState
                            android.util.Log.d("GameViewModel", "Установлен GAME_OVER: ${_gameState.value?.phase}")
                        }

                        if (hostMode) {
                            networkManager.sendAction(GameMessage(type = "PLAYER_LEFT"))
                        }
                    }
                }
            }
        }
    }

    private fun refreshPlayers() {
        val list = engine?.getState()?.players?.map {
            PlayerInfo(it.id, it.name, it.hand.size, it.isHost)
        } ?: emptyList()
        _players.value = list
    }

    private fun playersToJson(players: List<Player>): String {
        val arr = JSONArray()
        for (p in players) {
            android.util.Log.d("GameViewModel", "Player ${p.name}: hand size = ${p.hand.size}, cards = ${p.hand.map { it.id }}")
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("isHost", p.isHost)
            val cardsArr = JSONArray()
            for (c in p.hand) {
                val cardObj = JSONObject()
                cardObj.put("suit", c.suit.name)
                cardObj.put("rank", c.rank.name)
                cardObj.put("id", c.id)
                cardsArr.put(cardObj)
            }
            obj.put("cards", cardsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parsePlayers(json: String): List<PlayerInfo> {
        val arr = JSONArray(json)
        val list = mutableListOf<PlayerInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(PlayerInfo(o.getString("id"), o.getString("name"), 0, o.optBoolean("isHost", false)))
        }
        return list
    }

    class Factory(private val nsdHelper: NsdHelper? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(nsdHelper) as T
    }
}