package com.example.durakgame.ui.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import java.io.ByteArrayOutputStream
import java.io.File

class GameViewModel(private val nsdHelper: NsdHelper? = null) : ViewModel() {

    private var engine: GameEngine? = null
    private val networkManager = LocalNetworkManager()
    private var myPlayerId = ""
    private var hostMode = false
    private var playerName = ""
    
    private var rematchCount = 0
    private val _rematchStatus = MutableStateFlow("0/2")
    val rematchStatus: StateFlow<String> = _rematchStatus.asStateFlow()
    private val _playerLeft = MutableStateFlow(false)
    val playerLeft: StateFlow<Boolean> = _playerLeft.asStateFlow()
    
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

    // Логика точечной защиты и границ стола
    private val tableCardBounds = mutableMapOf<String, Rect>()
    private var tableBounds: Rect = Rect.Zero
    
    private val _highlightedCardId = MutableStateFlow<String?>(null)
    val highlightedCardId = _highlightedCardId.asStateFlow()

    private val _isDragging = MutableStateFlow(false)
    val isDragging = _isDragging.asStateFlow()

    init {
        observeNetworkMessages()
    }

    fun updateCardBounds(cardId: String, rect: Rect) {
        tableCardBounds[cardId] = rect.inflate(100f)
    }

    fun updateTableBounds(rect: Rect) {
        tableBounds = rect
    }

    fun onCardDragStart(cardId: String) {
        _selectedCardId.value = cardId
        _isDragging.value = true
    }

    fun onCardDragged(fingerPosition: Offset) {
        val state = _gameState.value ?: return
        val activeAttackingIds = state.tableCards.filter { !it.isDefended }.map { it.attackingCard.id }.toSet()
        
        val target = tableCardBounds.entries.toList().reversed().find { (id, rect) -> 
            id in activeAttackingIds && rect.contains(fingerPosition)
        }?.key
        
        _highlightedCardId.value = target
    }

    fun onCardDragEnd(card: Card, finalFingerPosition: Offset) {
        val state = _gameState.value ?: return
        val target = _highlightedCardId.value
        val isDefender = state.currentDefender.id == myPlayerId

        if (isDefender) {
            if (target != null) {
                playCard(card.id, target)
            }
        } else {
            if (tableBounds.contains(finalFingerPosition)) {
                playCard(card.id)
            }
        }
        
        _highlightedCardId.value = null
        _isDragging.value = false
        _selectedCardId.value = null
    }

    fun onCardDragCancel() {
        _isDragging.value = false
        _highlightedCardId.value = null
        _selectedCardId.value = null
    }

    fun getNetworkManager(): LocalNetworkManager = networkManager
    
    fun hostGame(config: GameConfig, name: String, avatarFile: File?) {
        Log.d("GameViewModel", "Hosting game for: $name")
        gameConfig = config
        hostMode = true
        playerName = name
        engine = GameEngine(config)
        
        val avatarBase64 = avatarFile?.let { encodeFileToBase64(it) }
        val player = engine!!.addPlayer(name, isHost = true, avatarBase64 = avatarBase64)
        myPlayerId = player.id
        
        val code = networkManager.startHost(config, name, avatarBase64)
        _gameCode.value = code
        _players.value = listOf(PlayerInfo(player.id, player.name, 0, true, avatarBase64))
        _isConnected.value = true
    }

    fun joinGame(hostAddress: String, name: String, avatarFile: File?) {
        Log.d("GameViewModel", "Joining game at $hostAddress as $name")
        hostMode = false
        playerName = name
        myPlayerId = networkManager.getMyPlayerId()
        val avatarBase64 = avatarFile?.let { encodeFileToBase64(it) }
        networkManager.connectToGame(hostAddress, name, avatarBase64)
    }

    private fun encodeFileToBase64(file: File): String? {
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            val scaled = if (bitmap.width > 96 || bitmap.height > 96) {
                Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            } else bitmap
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error encoding avatar: ${e.message}")
            null
        }
    }

    fun getBetAmount(): Long = gameConfig.betAmount

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
            val oldPlayers = engine?.getState()?.players ?: emptyList()
            engine = GameEngine(gameConfig)
            for (p in oldPlayers) {
                engine!!.addPlayer(p.name, p.isHost, p.avatarBase64)
            }
            engine!!.startGame()
            val state = engine!!.getState()
            _gameState.value = state
            _rematchStatus.value = ""
            _players.value = state.players.map {
                PlayerInfo(it.id, it.name, it.hand.size, it.isHost, it.avatarBase64)
            }
            networkManager.sendAction(
                GameMessage(
                    type = "START_GAME",
                    playersJson = playersToJson(state.players, includeAvatars = true),
                    trumpSuit = state.trumpSuit.name,
                    trumpCardId = state.deck.peekTrump().id
                )
            )
            sendStateUpdate()
        }
    }

    fun startGame() {
        if (hostMode && engine != null) {
            engine!!.startGame()
            val state = engine!!.getState()
            _gameState.value = state
            val playersJson = playersToJson(state.players, includeAvatars = true)
            val trumpCard = state.deck.peekTrump()
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
        networkManager.sendAction(GameMessage(type = "PLAYER_LEFT", playerId = myPlayerId))
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            networkManager.disconnect()
        }, 500)
    }

    fun selectCard(cardId: String?) {
        _selectedCardId.value = cardId
    }

    fun playCard(cardId: String, targetCardId: String? = null) {
        val state = _gameState.value ?: return
        val action = when {
            state.currentDefender.id != myPlayerId -> Action.Attack(myPlayerId, cardId)
            state.currentDefender.id == myPlayerId -> {
                val against = targetCardId ?: state.tableCards.find { !it.isDefended }?.attackingCard?.id
                if (against != null) Action.Defend(myPlayerId, cardId, against) else null
            }
            else -> null
        }
        if (action != null) {
            if (hostMode) {
                val result = engine?.processAction(action)
                if (result?.isSuccess == true) {
                    val s = engine?.getState() ?: return
                    _gameState.value = s
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
        if (hostMode) {
            val result = engine?.processAction(Action.DeclareTake(myPlayerId))
            if (result?.isSuccess == true) {
                val s = engine?.getState() ?: return
                _gameState.value = s
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
                _gameState.value = s
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
                _gameState.value = s
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

    private fun sendStateUpdate() {
        val state = engine?.getState() ?: return
        _gameState.value = state
        val json = JSONObject()
        json.put("players", JSONArray(playersToJson(state.players, includeAvatars = false)))
        json.put("phase", state.phase.name)
        json.put("attackerIndex", state.currentAttackerIndex)
        json.put("defenderIndex", state.currentDefenderIndex)
        json.put("trumpSuit", state.trumpSuit.name)
        json.put("deckSize", state.deck.size)
        json.put("lastTrumpSuit", state.lastTrumpSuit.name)
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
        }
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
        try {
            val obj = JSONObject(json)
            val playersArr = obj.getJSONArray("players")
            val newPlayers = mutableListOf<Player>()
            val currentPlayers = _gameState.value?.players ?: emptyList()

            for (i in 0 until playersArr.length()) {
                val pObj = playersArr.getJSONObject(i)
                val pid = pObj.getString("id")
                val currentHand = mutableListOf<Card>()
                val cardsArr = pObj.optJSONArray("cards")
                if (cardsArr != null) {
                    for (j in 0 until cardsArr.length()) {
                        val cObj = cardsArr.getJSONObject(j)
                        currentHand.add(Card(
                            suit = Suit.valueOf(cObj.getString("suit")), 
                            rank = Rank.valueOf(cObj.getString("rank")),
                            id = cObj.optString("id", "")
                        ))
                    }
                }
                val existingAvatar = currentPlayers.find { it.id == pid }?.avatarBase64
                val incomingAvatar = pObj.optStringOrNull("avatarBase64")
                newPlayers.add(Player(id = pid, name = pObj.getString("name"), hand = currentHand, isHost = pObj.optBoolean("isHost", false), avatarBase64 = incomingAvatar ?: existingAvatar))
            }
            val deck: Deck
            val deckCardsArr = obj.optJSONArray("deckCards")
            if (deckCardsArr != null && deckCardsArr.length() > 0) {
                val deckCards = mutableListOf<Card>()
                for (i in 0 until deckCardsArr.length()) {
                    val cObj = deckCardsArr.getJSONObject(i)
                    deckCards.add(Card(suit = Suit.valueOf(cObj.getString("suit")), rank = Rank.valueOf(cObj.getString("rank")), id = cObj.optString("id", "")))
                }
                deck = Deck.fromCards(deckCards)
                val trumpObj = obj.optJSONObject("trumpCard")
                if (trumpObj != null) {
                    deck.setTrumpCard(Card(suit = Suit.valueOf(trumpObj.getString("suit")), rank = Rank.valueOf(trumpObj.getString("rank")), id = trumpObj.optString("id", "")))
                }
            } else { deck = Deck.fromCards(emptyList()) }
            val tableCards = mutableListOf<GameState.TableSlot>()
            val tableArr = obj.optJSONArray("tableCards")
            if (tableArr != null) {
                for (i in 0 until tableArr.length()) {
                    val slotObj = tableArr.getJSONObject(i)
                    val attackingCard = Card(suit = Suit.valueOf(slotObj.getString("attackingSuit")), rank = Rank.valueOf(slotObj.getString("attackingRank")), id = slotObj.getString("attackingId"))
                    val defendingCard = if (slotObj.has("defendingSuit")) {
                        Card(suit = Suit.valueOf(slotObj.getString("defendingSuit")), rank = Rank.valueOf(slotObj.getString("defendingRank")), id = slotObj.getString("defendingId"))
                    } else null
                    tableCards.add(GameState.TableSlot(attackingCard, defendingCard))
                }
            }
            val phase = GameState.GamePhase.valueOf(obj.getString("phase"))
            val attackerIndex = obj.getInt("attackerIndex")
            val defenderIndex = obj.getInt("defenderIndex")
            val lastTrumpSuit = Suit.valueOf(obj.optString("lastTrumpSuit", "SPADES"))
            _gameState.value = GameState(deck = deck, players = newPlayers, config = _gameState.value?.config ?: GameConfig(), currentAttackerIndex = attackerIndex, currentDefenderIndex = defenderIndex, phase = phase, tableCards = tableCards, lastTrumpSuit = lastTrumpSuit)
        } catch (e: Exception) { Log.e("GameViewModel", "Error parsing state JSON: ${e.message}") }
    }

    private fun observeNetworkMessages() {
        viewModelScope.launch {
            networkManager.observeMessages().collect { msg ->
                when (msg.type) {
                    "JOIN" -> {
                        if (hostMode) {
                            val newPlayer = engine?.addPlayer(msg.playerName ?: "Гость", avatarBase64 = msg.avatarBase64)
                            refreshPlayers()
                            val correctId = newPlayer?.id ?: msg.playerId
                            networkManager.sendAction(GameMessage(type = "PLAYER_ID_ASSIGNED", playerId = msg.playerId, gameCode = correctId))
                            val list = engine?.getState()?.players ?: emptyList()
                            networkManager.sendAction(GameMessage("PLAYERS_UPDATE", playersJson = playersToJson(list, includeAvatars = true)))
                        }
                    }
                    "JOINED" -> {
                        myPlayerId = msg.playerId ?: myPlayerId
                        _gameCode.value = msg.gameCode ?: ""
                        _isConnected.value = true
                    }
                    "PLAYER_ID_ASSIGNED" -> {
                        if (myPlayerId == msg.playerId || myPlayerId == "") {
                            myPlayerId = msg.gameCode ?: myPlayerId
                        }
                    }
                    "PLAYERS_UPDATE" -> {
                        msg.playersJson?.let { _players.value = parsePlayers(it) }
                    }
                    "START_GAME" -> {
                        if (!hostMode) {
                            val pl = msg.playersJson?.let { parsePlayers(it) } ?: emptyList()
                            _players.value = pl
                            val me = pl.find { it.name == playerName }
                            if (me != null) myPlayerId = me.id
                            _gameState.value = GameState(deck = Deck(), players = pl.map { Player(it.id, it.name, isHost = it.isHost, avatarBase64 = it.avatarBase64) }, config = GameConfig(), phase = GameState.GamePhase.PLAYING)
                            _rematchStatus.value = ""
                            _playerLeft.value = false
                        }
                    }
                    "STATE_UPDATE" -> {
                        if (!hostMode) { msg.playersJson?.let { updateStateFromJson(it) } }
                    }
                    "ATTACK" -> {
                        if (hostMode) {
                            val result = engine?.processAction(Action.Attack(msg.playerId!!, msg.cardId!!))
                            if (result?.isSuccess == true) sendStateUpdate()
                        }
                    }
                    "DEFEND" -> {
                        if (hostMode) {
                            val result = engine?.processAction(Action.Defend(msg.playerId!!, msg.cardId!!, msg.againstCardId!!))
                            if (result?.isSuccess == true) sendStateUpdate()
                        }
                    }
                    "DECLARE_TAKE" -> {
                        if (hostMode) {
                            val result = engine?.processAction(Action.DeclareTake(msg.playerId!!))
                            if (result?.isSuccess == true) sendStateUpdate()
                        }
                    }
                    "PASS_ADDING" -> {
                        if (hostMode) {
                            val result = engine?.processAction(Action.PassAdding(msg.playerId!!))
                            if (result?.isSuccess == true) sendStateUpdate()
                        }
                    }
                    "END_TURN" -> {
                        if (hostMode) {
                            val result = engine?.processAction(Action.EndTurn(msg.playerId!!))
                            if (result?.isSuccess == true) sendStateUpdate()
                        }
                    }
                    "REMATCH_REQUEST" -> {
                        if (hostMode) {
                            rematchCount++
                            val total = _players.value.size
                            val status = "$rematchCount/$total"
                            _rematchStatus.value = status
                            networkManager.sendAction(GameMessage(type = "REMATCH_UPDATE", playersJson = status))
                            if (rematchCount >= total) {
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ startNewGame() }, 1000)
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
                    "REMATCH_UPDATE" -> { _rematchStatus.value = msg.playersJson ?: "0/2" }
                    "PLAYER_LEFT" -> {
                        _playerLeft.value = true
                        _gameState.value = _gameState.value?.copy(phase = GameState.GamePhase.GAME_OVER, tableCards = emptyList())
                    }
                }
            }
        }
    }

    private fun refreshPlayers() {
        val list = engine?.getState()?.players?.map {
            PlayerInfo(it.id, it.name, it.hand.size, it.isHost, it.avatarBase64)
        } ?: emptyList()
        _players.value = list
    }

    private fun playersToJson(players: List<Player>, includeAvatars: Boolean): String {
        val arr = JSONArray()
        for (p in players) {
            val obj = JSONObject()
            obj.put("id", p.id); obj.put("name", p.name); obj.put("isHost", p.isHost)
            if (includeAvatars) obj.put("avatarBase64", p.avatarBase64 ?: "")
            val cardsArr = JSONArray()
            for (c in p.hand) {
                val cardObj = JSONObject()
                cardObj.put("suit", c.suit.name); cardObj.put("rank", c.rank.name); cardObj.put("id", c.id)
                cardsArr.put(cardObj)
            }
            obj.put("cards", cardsArr); arr.put(obj)
        }
        return arr.toString()
    }

    private fun parsePlayers(json: String): List<PlayerInfo> {
        val arr = JSONArray(json)
        val list = mutableListOf<PlayerInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(PlayerInfo(id = o.getString("id"), name = o.getString("name"), cardCount = 0, isHost = o.optBoolean("isHost", false), avatarBase64 = o.optStringOrNull("avatarBase64")))
        }
        return list
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        val s = optString(key)
        return if (s.isNullOrEmpty()) null else s
    }

    class Factory(private val nsdHelper: NsdHelper? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(nsdHelper) as T
    }
}
