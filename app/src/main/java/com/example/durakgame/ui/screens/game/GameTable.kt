package com.example.durakgame.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.durakgame.engine.model.GameState
import com.example.durakgame.engine.model.Suit
import com.example.durakgame.ui.viewmodels.GameViewModel

@Composable
fun GameTable(
    gameState: GameState,
    isDragOver: Boolean,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    skinId: String = "classic"
) {
    val dragAlpha by animateFloatAsState(
        targetValue = if (isDragOver) 0.15f else 0f,
        label = "dragAlpha"
    )
    
    val highlightedCardId by viewModel.highlightedCardId.collectAsState()

    // Динамическое смещение стола при появлении второго ряда
    val rows = gameState.tableCards.chunked(3)
    val tableOffset by animateDpAsState(
        targetValue = if (rows.size > 1) (-80).dp else 0.dp,
        label = "tableVerticalOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 4.dp)
    ) {
        // Колода и козырь слева
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-80).dp)
        ) {
            if (gameState.deck.size > 0) {
                val trump = gameState.deck.peekTrump()
                CardView(
                    card = trump,
                    small = true,
                    skinId = skinId,
                    modifier = Modifier
                        .offset(x = 15.dp, y = 10.dp)
                        .graphicsLayer { rotationZ = 90f }
                )

                if (gameState.deck.size > 1) {
                    CardView(faceUp = false, small = true, skinId = skinId)

                    Text(
                        text = "${gameState.deck.size}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-40).dp, x = 55.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 120.dp)
                        .offset(x = 85.dp, y = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gameState.trumpSuit.symbol,
                        color = if (gameState.trumpSuit == Suit.HEARTS || gameState.trumpSuit == Suit.DIAMONDS) 
                                    Color.Red.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Карты на столе
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp, start = 20.dp) // нельзя менять
                .offset(y = tableOffset)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp), // Уменьшено расстояние между рядами
            horizontalAlignment = Alignment.Start 
        ) {
            rows.forEach { rowCards ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-10).dp), // нельзя менять
                    modifier = Modifier.animateContentSize()
                ) {
                    rowCards.forEach { slot ->
                        key(slot.attackingCard.id) {
                            SlotView(
                                slot = slot, 
                                skinId = skinId,
                                isHighlighted = highlightedCardId == slot.attackingCard.id,
                                onPositioned = { rect -> 
                                    viewModel.updateCardBounds(slot.attackingCard.id, rect)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Зона сброса
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 20.dp, top = 80.dp, bottom = 80.dp, end = 20.dp)
                .fillMaxSize()
                .onGloballyPositioned { viewModel.updateTableBounds(it.boundsInRoot()) }
                .background(Color.White.copy(alpha = dragAlpha), RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun SlotView(
    slot: GameState.TableSlot, 
    skinId: String,
    isHighlighted: Boolean,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    Box(
        modifier = Modifier.size(width = 120.dp, height = 140.dp), // Уменьшена высота для предотвращения обрезки ряда
        contentAlignment = Alignment.TopCenter
    ) {
        // Атакующая карта
        var showAttacker by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { showAttacker = true }

        AnimatedVisibility(
            visible = showAttacker,
            enter = fadeIn(tween(200)) + 
                    scaleIn(initialScale = 1.2f, animationSpec = spring(dampingRatio = 0.6f)) +
                    slideInVertically { -it / 4 }
        ) {
            Box {
                CardView(
                    card = slot.attackingCard, 
                    small = true, 
                    skinId = skinId,
                    modifier = Modifier.onGloballyPositioned { 
                        if (!slot.isDefended) onPositioned(it.boundsInRoot()) 
                    }
                )
                if (isHighlighted && !slot.isDefended) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    )
                }
            }
        }
        
        // Защищающаяся карта
        slot.defendingCard?.let { defCard ->
            var showDefender by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { showDefender = true }

            AnimatedVisibility(
                visible = showDefender,
                enter = fadeIn() + 
                        scaleIn(initialScale = 1.5f, animationSpec = spring(dampingRatio = 0.6f)) +
                        slideInVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = (5).dp, y = (-10).dp)
            ) {
                CardView(
                    card = defCard, 
                    small = true, 
                    skinId = skinId,
                    modifier = Modifier.graphicsLayer { rotationZ = 15f }
                )
            }
        }
    }
}
