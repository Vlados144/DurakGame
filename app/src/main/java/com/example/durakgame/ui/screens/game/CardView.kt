package com.example.durakgame.ui.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.engine.model.Card
import com.example.durakgame.engine.model.Rank

@Composable
fun CardView(
    modifier: Modifier = Modifier,
    card: Card? = null,
    skinId: String = "classic",
    faceUp: Boolean = true,
    selected: Boolean = false,
    small: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    // Размеры карт менять строго нельзя
    val width = if (small) 85.dp else 120.dp
    val height = if (small) 121.dp else 170.dp
    
    val imageName = remember(card, skinId, faceUp) {
        if (faceUp && card != null) {
            val rankName = when (card.rank) {
                Rank.SIX -> "6"
                Rank.SEVEN -> "7"
                Rank.EIGHT -> "8"
                Rank.NINE -> "9"
                Rank.TEN -> "10"
                else -> card.rank.name.lowercase()
            }
            "card_${skinId}_${card.suit.name.lowercase()}_$rankName"
        } else {
            "card_${skinId}_back"
        }
    }
    
    val imageRes = remember(imageName) {
        val id = context.resources.getIdentifier(imageName, "drawable", context.packageName)
        if (id == 0) context.resources.getIdentifier("card_${skinId}_back", "drawable", context.packageName) else id
    }

    Box(
        modifier = modifier
            .size(width, height)
            .shadow(if (selected) 20.dp else 2.dp, RoundedCornerShape(6.dp)) 
            .clip(RoundedCornerShape(8.dp)) // Сглаживание нельзя менять
            .background(Color.White)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != 0) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (small) 2.dp else 10.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            val label = card?.toString() ?: ""
            Text(
                text = label,
                fontSize = if (small) 16.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (card?.suit?.name == "HEARTS" || card?.suit?.name == "DIAMONDS") Color.Red else Color.Black
            )
        }
    }
}
