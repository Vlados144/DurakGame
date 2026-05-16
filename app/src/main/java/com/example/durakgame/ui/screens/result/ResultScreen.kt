package com.example.durakgame.ui.screens.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.DurakApplication
import com.example.durakgame.engine.model.GameState
import com.example.durakgame.engine.model.Player
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.GoldButton
import com.example.durakgame.ui.components.DarkButton

@Composable
fun ResultScreen(
    players: List<Player>,
    betAmount: Long,
    rematchStatus: String,
    playerLeft: Boolean,
    onBackToMenu: () -> Unit,
    onRematch: () -> Unit,
    onCancelRematch: () -> Unit,
    isHost: Boolean
) {
    val context = LocalContext.current
    val app = context.applicationContext as DurakApplication
    var rematchRequested by remember { mutableStateOf(false) }

    // Обновляем статистику
    LaunchedEffect(Unit) {
        val userRepo = app.userRepository
        val walletRepo = app.walletRepository
        val myPlayer = players.firstOrNull()
        if (myPlayer != null) {
            val won = !myPlayer.hasCards
            userRepo.incrementGames(won)
            if (betAmount > 0) {
                walletRepo.addFunds(if (won) betAmount else -betAmount)
            }
        }
    }

    // Результаты: сортировка — вышедшие первые, дурак последний
    val results = remember(players) {
        val sorted = players.sortedBy { if (it.hasCards) 1 else 0 }
        sorted.mapIndexed { index, player ->
            val isDurak = index == sorted.lastIndex && player.hasCards
            Triple(player, index + 1, isDurak)
        }
    }

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎉", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Игра окончена!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
            Spacer(modifier = Modifier.height(24.dp))

            // Таблица результатов
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Результаты", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    results.forEach { (player, place, isDurak) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when {
                                        isDurak -> "💩"
                                        place == 1 -> "🥇"
                                        place == 2 -> "🥈"
                                        else -> "🥉"
                                    },
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    player.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = if (place == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Text(
                                when {
                                    isDurak -> "Дурак!"
                                    place == 1 -> "Победитель"
                                    else -> "$place место"
                                },
                                color = if (isDurak) Color.Red.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка реванш
            if (playerLeft) {
                Text("❌ Игрок вышел, реванш невозможен", color = Color.Red, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                DarkButton(text = "🏠 В меню", onClick = onBackToMenu)
            } else if (rematchRequested) {
                Text("Ожидание игроков $rematchStatus", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                DarkButton(text = "Отмена", onClick = {
                    rematchRequested = false
                    onCancelRematch()
                })
            } else {
                GoldButton(
                    text = "🔄 Реванш",
                    onClick = {
                        rematchRequested = true
                        onRematch()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                DarkButton(text = "🏠 В меню", onClick = onBackToMenu)
            }
        }
    }
}