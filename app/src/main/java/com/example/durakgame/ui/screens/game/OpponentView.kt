package com.example.durakgame.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.engine.model.Player

@Composable
fun OpponentView(
    player: Player,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Аватарка
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color(0xFFFFD700).copy(alpha = 0.8f)
                    else Color.White.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("😎", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Имя
        Text(
            player.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )

        // Карты оппонента (рубашки)
        if (player.hand.isNotEmpty()) {
            Text(
                "🂠 × ${player.hand.size}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}