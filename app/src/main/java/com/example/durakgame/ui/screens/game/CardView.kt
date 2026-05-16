package com.example.durakgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CardView(
    label: String,
    isRed: Boolean = false,
    faceUp: Boolean = true,
    selected: Boolean = false,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val width = if (small) 45.dp else 55.dp
    val height = if (small) 65.dp else 75.dp
    val fontSize = if (small) 13.sp else 16.sp

    val cardColor = if (faceUp) Color(0xFFF5F0E8) else Color(0xFF1A237E)
    val textColor = if (faceUp) {
        if (isRed) Color(0xFFC62828) else Color(0xFF212121)
    } else Color.White

    val borderColor = if (selected) Color(0xFFFFD700) else Color.Transparent
    val borderWidth = if (selected) 2.dp else 0.5.dp
    val borderColorDefault = if (faceUp) Color(0xFFCCCCCC) else Color(0xFF0D1642)

    Box(
        modifier = modifier
            .size(width, height)
            .shadow(if (selected) 8.dp else 2.dp, RoundedCornerShape(6.dp))
            .background(cardColor, RoundedCornerShape(6.dp))
            .border(
                if (selected) borderWidth else 0.5.dp,
                if (selected) borderColor else borderColorDefault,
                RoundedCornerShape(6.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (faceUp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Верхний индекс
                Text(
                    text = label,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        } else {
            // Рубашка — узор
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("♠", fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                Text("♥", fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                Text("♦", fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                Text("♣", fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
            }
        }
    }
}