package com.example.durakgame.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    val dragAlpha by animateFloatAsState(
        targetValue = if (isDragOver) 0.15f else 0f,
        label = "dragAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
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
                    CardView(label = "", faceUp = false, small = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Козырь
                val trump = gameState.deck.peekTrump()
                CardView(
                    label = trump.toString(),
                    isRed = trump.suit.name == "HEARTS" || trump.suit.name == "DIAMONDS",
                    small = true
                )
            }
        }

        // Центр — карты на столе с анимацией появления
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 60.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            gameState.tableCards.forEachIndexed { index, slot ->
                key(slot.attackingCard.id) {
                    SlotView(slot = slot)
                }
            }
        }

        // Зона для броска карт с плавной подсветкой
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 60.dp)
                .fillMaxWidth(0.8f)
                .height(140.dp)
                .background(
                    color = Color.White.copy(alpha = dragAlpha),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isDragOver) 2.dp else 0.dp,
                    color = if (isDragOver) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
private fun SlotView(slot: GameState.TableSlot) {
    Box(
        modifier = Modifier.padding(horizontal = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Атакующая карта
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f) + slideInVertically { it / 2 }
        ) {
            CardView(
                label = slot.attackingCard.toString(),
                isRed = slot.attackingCard.suit.name == "HEARTS" ||
                        slot.attackingCard.suit.name == "DIAMONDS",
                small = true
            )
        }

        // Защищающаяся карта
        val defending = slot.defendingCard
        if (defending != null) {
            var defVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { defVisible = true }

            AnimatedVisibility(
                visible = defVisible,
                enter = fadeIn() + expandVertically() + scaleIn(initialScale = 0.5f),
                modifier = Modifier.offset(x = 12.dp, y = 16.dp)
            ) {
                CardView(
                    label = defending.toString(),
                    isRed = defending.suit.name == "HEARTS" ||
                            defending.suit.name == "DIAMONDS",
                    small = true
                )
            }
        }
    }
}
