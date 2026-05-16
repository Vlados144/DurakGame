package com.example.durakgame.ui.screens.create_game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.engine.model.GameConfig
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.GoldButton
import com.example.durakgame.ui.components.DarkButton

@Composable
fun CreateGameScreen(
    onGameCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    var gameMode by remember { mutableStateOf(GameConfig.GameMode.PODKIDNOY) }
    var maxPlayers by remember { mutableIntStateOf(2) }
    var betAmount by remember { mutableLongStateOf(0) }
    var turnTimer by remember { mutableIntStateOf(0) }

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())  // ДОБАВЛЕН СКРОЛЛ
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Создание игры",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Режим игры
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Режим игры", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = gameMode == GameConfig.GameMode.PODKIDNOY,
                            onClick = { gameMode = GameConfig.GameMode.PODKIDNOY },
                            label = { Text("Подкидной") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700),
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = gameMode == GameConfig.GameMode.PEREVODNOY,
                            onClick = { gameMode = GameConfig.GameMode.PEREVODNOY },
                            label = { Text("Переводной") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Количество игроков
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Количество игроков", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (count in 2..6) {
                            FilterChip(
                                selected = maxPlayers == count,
                                onClick = { maxPlayers = count },
                                label = { Text("$count") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ставка
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ставка: $betAmount 💰", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val betValues = listOf(
                        0L, 10L, 50L, 100L,
                        250L, 500L, 1000L, 2500L,
                        5000L, 10000L, 25000L, 50000L,
                        100000L, 250000L, 500000L, 1000000L
                    )
                    var sliderPosition by remember {
                        mutableFloatStateOf(betValues.indexOf(betAmount).toFloat())
                    }
                    Slider(
                        value = sliderPosition,
                        onValueChange = { newPosition ->
                            val index = newPosition.toInt()
                            sliderPosition = index.toFloat()
                            betAmount = betValues[index]
                        },
                        valueRange = 0f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("1M", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Таймер
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Таймер на ход", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val timers = listOf(0 to "Без", 30 to "30с", 60 to "60с", 90 to "90с")
                        for ((value, label) in timers) {
                            FilterChip(
                                selected = turnTimer == value,
                                onClick = { turnTimer = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки
            GoldButton(
                text = "🎯 Создать игру",
                onClick = {
                    val gameCode = generateGameCode()
                    onGameCreated(gameCode)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            DarkButton(text = "Назад", onClick = onBack, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun generateGameCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..4).map { chars.random() }.joinToString("")
}
private fun formatBetAmount(amount: Long): String {
    return when {
        amount >= 1_000_000 -> "${amount / 1_000_000} млн"
        amount >= 1_000 -> "${amount / 1_000} тыс"
        else -> amount.toString()
    }
}

private fun formatBetAmountShort(amount: Long): String {
    return when {
        amount >= 1_000_000 -> "${amount / 1_000_000}M"
        amount >= 1_000 -> "${amount / 1_000}K"
        else -> amount.toString()
    }
}