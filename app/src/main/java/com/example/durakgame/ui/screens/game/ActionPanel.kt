package com.example.durakgame.ui.screens.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ActionPanelState(
    val text: String = "",
    val isMyTurn: Boolean = false,
    val showTakeButton: Boolean = false,
    val showPassButton: Boolean = false,
    val showBitoButton: Boolean = false
)

@Composable
fun ActionPanel(
    state: ActionPanelState,
    onTakeCards: () -> Unit,
    onPass: () -> Unit,
    onBito: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            state.text.contains("ВАШ ХОД") -> Color(0xFF2E7D32)
            state.text.contains("ЗАЩИЩАЙТЕСЬ") -> Color(0xFFC62828)
            state.text.contains("ПОДКИДЫВАЙТЕ") -> Color(0xFFF9A825)
            state.text.startsWith("Ходит") -> Color(0xFF37474F)
            else -> Color.Transparent
        },
        label = "panelColor"
    )

    if (state.text.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = state.text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        if (state.showTakeButton) {
            Button(
                onClick = onTakeCards,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFC62828)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text("ВЗЯТЬ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        if (state.showPassButton) {
            Button(
                onClick = onPass,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = Color(0xFF37474F)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text("ПАС", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        if (state.showBitoButton) {
            Button(
                onClick = onBito,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2E7D32)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text("БИТО", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}