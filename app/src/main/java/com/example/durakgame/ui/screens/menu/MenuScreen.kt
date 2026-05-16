package com.example.durakgame.ui.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.DurakApplication
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.GoldButton
import com.example.durakgame.ui.components.DarkButton
import kotlinx.coroutines.launch

@Composable
fun MenuScreen(
    onCreateGame: () -> Unit,
    onFindGame: () -> Unit,
    onProfile: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as DurakApplication
    val userRepository = app.userRepository

    var nickname by remember { mutableStateOf("Игрок") }
    var balance by remember { mutableStateOf(1000L) }

    LaunchedEffect(Unit) {
        val user = userRepository.getUser()
        nickname = user.nickname
        balance = user.balance

    }
    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "♠ ♥ ДУРАК ♣ ♦",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$nickname | Баланс: $balance 💰",
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(64.dp))

            GoldButton(
                text = "🎯 Создать игру",
                onClick = onCreateGame,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            GoldButton(
                text = "🔍 Найти игру",
                onClick = onFindGame,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            DarkButton(text = "👤 Профиль", onClick = onProfile)
        }
    }
}