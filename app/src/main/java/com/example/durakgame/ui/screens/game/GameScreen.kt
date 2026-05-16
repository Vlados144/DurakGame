package com.example.durakgame.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.engine.model.*
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.viewmodels.GameViewModel

@Composable
fun GameScreen(
    hostAddress: String,
    onGameOver: () -> Unit,
    onBack: () -> Unit,
    viewModel: GameViewModel
) {
    val gameState by viewModel.gameState.collectAsState()
    val selectedCardId by viewModel.selectedCardId.collectAsState()
    var isDragOver by remember { mutableStateOf(false) }

    LaunchedEffect(gameState) {
        if (gameState != null && gameState!!.phase == GameState.GamePhase.GAME_OVER) {
            onGameOver()
        }
    }

    if (gameState == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
                Spacer(Modifier.height(16.dp))
                Text("Ожидание игры...", color = Color.White)
            }
        }
        return
    }

    val state = gameState!!
    val myId = viewModel.getMyPlayerId()
    val me = state.players.find { it.id == myId } ?: return
    val phase = state.phase

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Код: $hostAddress", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)

                // Кнопка выхода
                var showExitDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = { showExitDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("🚪 Выйти", fontSize = 14.sp)
                }

                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("Выйти из игры?", color = Color.White) },
                        text = { Text("Вы уверены? Игра будет завершена для всех.", color = Color.White.copy(alpha = 0.7f)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showExitDialog = false
                                viewModel.exitGame()
                            }) {
                                Text("Выйти", color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) {
                                Text("Отмена", color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF1A1A2E)
                    )
                }
            }
            // Оппоненты
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                state.players
                    .filter { it.id != me.id }
                    .forEach { player ->
                        key(player.id + player.hand.size) {
                            OpponentView(
                                player = player,
                                isActive = player.id == state.currentDefender.id ||
                                        player.id == state.currentAttacker.id
                            )
                        }
                    }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Козырь: ${state.trumpSuit.symbol} | Колода: ${state.deck.size}",
                    color = Color(0xFFFFD700).copy(alpha = 0.7f), fontSize = 11.sp)
            }

            Spacer(Modifier.weight(0.2f))

            // Стол
            GameTable(
                gameState = state,
                isDragOver = isDragOver,
                onTableClick = { }
            )

            Spacer(Modifier.weight(0.4f))

            // Рука игрока
            PlayerHand(
                cards = me.hand,
                selectedCardId = selectedCardId,
                onCardSelected = { card ->
                    viewModel.selectCard(if (selectedCardId == card.id) null else card.id)
                },
                onCardDragged = { card, offset ->
                    isDragOver = offset.y < -80f
                },
                onCardDragEnd = { card ->
                    isDragOver = false
                    android.util.Log.d("GameScreen", "Карта брошена: ${card.id}, фаза=$phase")
                    if (state.phase == GameState.GamePhase.PLAYING) {
                        android.util.Log.d("GameScreen", "Вызываю playCard")
                        viewModel.playCard(card.id)
                    } else {
                        android.util.Log.d("GameScreen", "Не фаза PLAYING: ${state.phase}")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // Панель действий
            ActionPanel(
                state = ActionPanelState(
                    text = when {
                        state.phase == GameState.GamePhase.PLAYING && state.currentAttacker.id == me.id && state.tableCards.isEmpty() -> "ВАШ ХОД — киньте карту"
                        state.phase == GameState.GamePhase.PLAYING && state.currentAttacker.id == me.id -> "Подкидывайте или БИТО"
                        state.phase == GameState.GamePhase.PLAYING && state.currentDefender.id == me.id && state.tableCards.any { !it.isDefended } -> "ЗАЩИЩАЙТЕСЬ"
                        state.phase == GameState.GamePhase.PLAYING && state.currentDefender.id != me.id && state.tableCards.isNotEmpty() -> "Подкидывайте или ПАС"
                        state.phase == GameState.GamePhase.PLAYING -> "Ожидание..."
                        else -> ""
                    },
                    showTakeButton = state.phase == GameState.GamePhase.PLAYING && state.currentDefender.id == me.id && state.tableCards.any { !it.isDefended },
                    showPassButton = false,
                    showBitoButton = state.phase == GameState.GamePhase.PLAYING && state.currentDefender.id != me.id && state.tableCards.isNotEmpty()
                ),
                onTakeCards = { viewModel.declareTake() },
                onPass = { viewModel.endTurn() },
                onBito = { viewModel.endTurn() }
            )
    }
}
}