package com.example.durakgame.ui.screens.game

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.engine.model.Player

@Composable
fun OpponentView(
    player: Player,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    skinId: String = "classic"
) {
    val avatarBitmap = remember(player.avatarBase64) {
        player.avatarBase64?.let { base64 ->
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Аватарка
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) Color(0xFFADFF2F) else Color.White.copy(alpha = 0.1f)) 
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray),
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
                Text("👤", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            player.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )

        // Контейнер для счетчика карт
        Box(
            modifier = Modifier.height(28.dp), 
            contentAlignment = Alignment.Center
        ) {
            if (player.hand.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 14.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${player.hand.size}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
