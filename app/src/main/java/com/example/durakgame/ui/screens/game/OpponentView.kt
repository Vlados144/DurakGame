package com.example.durakgame.ui.screens.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    // Анимация пульсации для активного игрока
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isActive) 0.8f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Внешний пульсирующий круг
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale)
                        .background(Color(0xFFFFD700).copy(alpha = pulseAlpha), CircleShape)
                )
            }
            
            // Аватарка
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFFFFD700)
                        else Color.White.copy(alpha = 0.2f)
                    )
                    .then(
                        if (isActive) Modifier.border(2.dp, Color.White, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("😎", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Имя
        Text(
            player.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )

        // Карты оппонента (рубашки)
        if (player.hand.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🂠", 
                    color = Color.White.copy(alpha = 0.7f), 
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "${player.hand.size}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
