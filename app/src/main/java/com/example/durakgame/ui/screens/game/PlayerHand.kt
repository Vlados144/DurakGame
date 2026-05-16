package com.example.durakgame.ui.screens.game

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.durakgame.engine.model.Card
import com.example.durakgame.ui.components.CardView
import kotlin.math.roundToInt

@Composable
fun PlayerHand(
    cards: List<Card>,
    selectedCardId: String?,
    onCardSelected: (Card) -> Unit,
    onCardDragged: (Card, Offset) -> Unit,
    onCardDragEnd: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        cards.forEachIndexed { index, card ->
            val isSelected = card.id == selectedCardId
            val totalCards = cards.size
            val centerIndex = (totalCards - 1) / 2f
            val offsetFromCenter = index - centerIndex

            val targetX = when {
                totalCards <= 6 -> offsetFromCenter * 40
                totalCards <= 10 -> offsetFromCenter * 30
                else -> offsetFromCenter * 20
            }.dp

            val targetY = (kotlin.math.abs(offsetFromCenter) * 2f).dp

            // Анимированное смещение для плавного перемещения карт в руке
            val animatedOffset by animateIntOffsetAsState(
                targetValue = IntOffset(targetX.value.toInt(), targetY.value.toInt()),
                animationSpec = spring(stiffness = 300f)
            )

            var dragOffset by remember { mutableStateOf(Offset.Zero) }
            var isDragging by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .offset { 
                        // Базовое анимированное положение + текущий драг
                        IntOffset(
                            (animatedOffset.x.dp.toPx() + dragOffset.x).roundToInt(),
                            (animatedOffset.y.dp.toPx() + dragOffset.y).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        rotationZ = offsetFromCenter * 3f
                        translationY = if (isDragging) -60f else if (isSelected) -40f else 0f
                        scaleX = if (isDragging) 1.2f else if (isSelected) 1.1f else 1f
                        scaleY = if (isDragging) 1.2f else if (isSelected) 1.1f else 1f
                        alpha = if (isDragging) 0.9f else 1f
                    }
                    .pointerInput(card.id) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                onCardSelected(card)
                            },
                            onDragEnd = {
                                isDragging = false
                                if (dragOffset.y < -150f) {
                                    onCardDragEnd(card)
                                }
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                onCardDragged(card, dragOffset)
                            }
                        )
                    }
            ) {
                CardView(
                    label = card.toString(),
                    isRed = card.suit.name == "HEARTS" || card.suit.name == "DIAMONDS",
                    selected = isSelected || isDragging
                )
            }
        }
    }
}
