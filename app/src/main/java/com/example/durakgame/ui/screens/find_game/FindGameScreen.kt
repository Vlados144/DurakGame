package com.example.durakgame.ui.screens.find_game

import android.net.nsd.NsdServiceInfo
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.network.LocalNetworkManager
import com.example.durakgame.network.NsdHelper
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.GoldButton
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

    LaunchedEffect(Unit) {
        udpDiscovery.startListening()
    }

    DisposableEffect(Unit) {
        onDispose { udpDiscovery.stop() }
    }

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Найдено игр: ${hosts.size}", color = Color.White, fontSize = 20.sp)
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
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(hosts) { host ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            onGameJoined("${host.hostAddress}:${host.port}")
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🎮", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Игра: ${host.gameCode}", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(host.hostAddress, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            DarkButton(text = "Назад", onClick = onBack)
        }
    }
}

@Composable
private fun DiscoveredGameCard(
    service: NsdServiceInfo,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoin() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎮", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    service.serviceName.removePrefix("DurakGame-"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Коснитесь, чтобы подключиться",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}