package com.example.durakgame.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.durakgame.engine.model.GameState
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.viewmodels.GameViewModel

@Composable
fun GameScreen(
    hostAddress: String,
    onGameOver: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    viewModel: GameViewModel
) {
    val gameState by viewModel.gameState.collectAsState()
    val selectedCardId by viewModel.selectedCardId.collectAsState()
    val isDragging by viewModel.isDragging.collectAsState()
    
    val currentSkin = "classic" 

    LaunchedEffect(gameState) {
        if (gameState != null && gameState!!.phase == GameState.GamePhase.GAME_OVER) {
            onGameOver()
        }
    }

    if (gameState == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD700))
        }
        return
    }

    val state = gameState!!
    val myId = viewModel.getMyPlayerId()
    val me = state.players.find { it.id == myId } ?: return

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Слой Стола и Оппонентов
            Column(modifier = Modifier.fillMaxSize()) {
                GameTopBar(hostAddress = hostAddress, onExit = { viewModel.exitGame() })

                // Оппоненты
                Row(
                    Modifier.fillMaxWidth().padding(top = 0.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    state.players.filter { it.id != me.id }.forEach { player ->
                        key(player.id) {
                            OpponentView(
                                player = player,
                                isActive = player.id == state.currentDefender.id || player.id == state.currentAttacker.id,
                                skinId = currentSkin
                            )
                        }
                    }
                }

                // Игровой стол (Занимает максимум места)
                GameTable(
                    gameState = state,
                    isDragOver = isDragging,
                    viewModel = viewModel,
                    skinId = currentSkin,
                    modifier = Modifier.weight(1f)
                )
                
                // Уменьшено пространство под руку, чтобы дать столу больше места (предотвращение обрезки)
                Spacer(modifier = Modifier.height(140.dp))
            }

            // 2. Слой руки игрока
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp) // Подняли руку выше
                    .zIndex(1f)
            ) {
                PlayerHand(
                    cards = me.hand,
                    selectedCardId = selectedCardId,
                    viewModel = viewModel,
                    skinId = currentSkin
                )
            }

            // 3. Панель действий
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            ) {
                val isDefender = state.currentDefender.id == me.id
                
                ActionPanel(
                    state = ActionPanelState(
                        text = getInstructionText(state, me.id),
                        showTakeButton = isDefender && state.tableCards.any { !it.isDefended },
                        showPassButton = !isDefender && state.tableCards.isNotEmpty(),
                        showBitoButton = state.currentAttacker.id == me.id && state.tableCards.isNotEmpty() && state.tableCards.all { it.isDefended }
                    ),
                    onTakeCards = { viewModel.declareTake() },
                    onPass = { viewModel.passAdding() },
                    onBito = { viewModel.endTurn() }
                )
            }
        }
    }
}

@Composable
private fun GameTopBar(hostAddress: String, onExit: () -> Unit) {
    var showExitDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ID: $hostAddress", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
        IconButton(onClick = { showExitDialog = true }, modifier = Modifier.size(32.dp)) {
            Text("🚪", fontSize = 18.sp)
        }
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выйти?") },
            text = { Text("Игра будет завершена.") },
            confirmButton = { 
                TextButton(onClick = { 
                    showExitDialog = false
                    onExit() 
                }) { 
                    Text("Выход", color = Color.Red)
                } 
            },
            dismissButton = { 
                TextButton(onClick = { showExitDialog = false }) { 
                    Text("Остаться")
                } 
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }
}

private fun getInstructionText(state: GameState, myId: String): String {
    return when {
        state.phase != GameState.GamePhase.PLAYING -> "Ожидание..."
        state.currentAttacker.id == myId && state.tableCards.isEmpty() -> "ВАШ ХОД"
        state.currentDefender.id == myId -> "ЗАЩИЩАЙТЕСЬ"
        else -> "МОЖНО ПОДКИНУТЬ"
    }
}
