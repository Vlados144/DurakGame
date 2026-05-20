package com.example.durakgame.ui.screens.lobby

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.DurakApplication
import com.example.durakgame.engine.model.GameConfig
import com.example.durakgame.engine.model.GameState
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.GoldButton
import com.example.durakgame.ui.components.DarkButton
import com.example.durakgame.ui.viewmodels.GameViewModel
import java.io.File

@Composable
fun GameLobbyScreen(
    gameCode: String,
    isHost: Boolean,
    onGameStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: GameViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as DurakApplication
    val players by viewModel.players.collectAsState()
    val displayCode by viewModel.gameCode.collectAsState()
    val gameState by viewModel.gameState.collectAsState()

    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val user = app.userRepository.getUser()
        val avatarFile = File(context.filesDir, "avatar.jpg").takeIf { it.exists() }
        
        if (isHost) {
            viewModel.hostGame(config = GameConfig(), name = user.nickname, avatarFile = avatarFile)
        } else {
            viewModel.joinGame(gameCode, user.nickname, avatarFile = avatarFile)
        }
    }

    LaunchedEffect(gameState) {
        if (gameState != null && gameState!!.phase != GameState.GamePhase.WAITING_FOR_PLAYERS) {
            onGameStarted()
        }
    }

    val codeToShow = if (displayCode.isNotEmpty()) displayCode else gameCode

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Лобби игры", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
            Spacer(modifier = Modifier.height(8.dp))

            if (isHost) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Код игры", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(codeToShow, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), letterSpacing = 8.sp)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Подключено к: $codeToShow", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Игроки", color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            if (players.isEmpty()) {
                PlayerSlot(name = "Ожидание игрока...", isHost = false, isFilled = false)
            } else {
                players.forEach { player ->
                    PlayerSlot(
                        name = "${player.name}${if (player.isHost) " (хост)" else ""}",
                        isHost = player.isHost,
                        isFilled = true,
                        avatarBase64 = player.avatarBase64
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isHost) {
                GoldButton(
                    text = "▶ Начать игру",
                    onClick = { viewModel.startGame() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = players.size >= 2
                )
            } else {
                GoldButton(
                    text = if (isReady) "✅ Готов" else "Нажмите для готовности",
                    onClick = { isReady = !isReady },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            DarkButton(text = "Выйти", onClick = {
                viewModel.leaveGame()
                onBack()
            })
        }
    }
}

@Composable
private fun PlayerSlot(name: String, isHost: Boolean, isFilled: Boolean, avatarBase64: String? = null) {
    val avatarBitmap = remember(avatarBase64) {
        avatarBase64?.let { base64 ->
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFilled) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFilled) Color(0xFFFFD700).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(if (isHost && isFilled) "👑" else if (isFilled) "😎" else "?", fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, color = if (isFilled) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
        }
    }
}