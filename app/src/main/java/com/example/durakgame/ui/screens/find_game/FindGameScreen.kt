package com.example.durakgame.ui.screens.find_game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.network.LocalNetworkManager
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.DarkButton

@Composable
fun FindGameScreen(
    networkManager: LocalNetworkManager,
    onGameJoined: (String) -> Unit,
    onBack: () -> Unit
) {
    val udpDiscovery = networkManager.getUdpDiscovery()
    val hosts by udpDiscovery.hosts.collectAsState()
    var playerName by remember { mutableStateOf("Игрок") }

    // Запускаем поиск при входе на экран
    LaunchedEffect(Unit) {
        udpDiscovery.startListening()
    }

    // Останавливаем при выходе
    DisposableEffect(Unit) {
        onDispose {
            udpDiscovery.stop()
        }
    }

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hosts.isEmpty()) "Поиск игр..." else "Найдено игр: ${hosts.size}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Ваше имя") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFFFD700),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (hosts.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hosts, key = { it.hostAddress + it.gameCode }) { host ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onGameJoined("${host.hostAddress}:${host.port}")
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🎮", fontSize = 20.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Комната: ${host.gameCode}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "IP: ${host.hostAddress}",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Text("→", color = Color.White.copy(alpha = 0.5f), fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DarkButton(
                text = "Назад",
                onClick = onBack
            )
        }
    }
}
