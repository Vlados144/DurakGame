package com.example.durakgame.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.durakgame.engine.model.Card
import com.example.durakgame.engine.model.GameState
import com.example.durakgame.ui.components.CardView

@Composable
fun GameTable(
    gameState: GameState,
    isDragOver: Boolean,
    onTableClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(8.dp)
    ) {
        // Слева — колода и козырь
        if (!gameState.deck.isEmpty) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Стопка рубашек
                Box {
                    Box(modifier = Modifier.offset(x = (-2).dp, y = (-2).dp)) {
                        CardView(label = "", faceUp = false, small = true)
                    }
                    Box(modifier = Modifier.offset(x = 2.dp, y = 2.dp)) {
                        CardView(label = "", faceUp = false, small = true)
                    }
                    CardView(label = "", faceUp = false, small = true)
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Козырь
                val trump = gameState.deck.peekTrump()
                CardView(
                    label = trump.toString(),
                    isRed = trump.suit.name == "HEARTS" || trump.suit.name == "DIAMONDS",
                    small = true
                )
            }
        }

        // Центр — карты на столе
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 60.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (gameState.tableCards.isNotEmpty()) {
                gameState.tableCards.forEach { slot ->
                    SlotView(slot = slot)
                }
            }
        }

        // Невидимая зона для броска карт
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 60.dp)
                .fillMaxWidth(0.8f)
                .height(120.dp)
                .background(
                    if (isDragOver) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isDragOver) 2.dp else 0.dp,
                    color = if (isDragOver) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}

@Composable
private fun SlotView(slot: GameState.TableSlot) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        CardView(
            label = slot.attackingCard.toString(),
            isRed = slot.attackingCard.suit.name == "HEARTS" ||
                    slot.attackingCard.suit.name == "DIAMONDS",
            small = true
        )
        val defending = slot.defendingCard
        if (defending != null) {
            CardView(
                label = defending.toString(),
                isRed = defending.suit.name == "HEARTS" ||
                        defending.suit.name == "DIAMONDS",
                small = true,
                modifier = Modifier.offset(x = 8.dp, y = (-4).dp)
            )
        } else {
            Spacer(modifier = Modifier.size(45.dp, 20.dp))
        }
    }
}