package com.example.durakgame.ui.screens.game

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        cards.forEachIndexed { index, card ->
            val isSelected = card.id == selectedCardId
            val totalCards = cards.size
            val centerIndex = (totalCards - 1) / 2f
            val offsetFromCenter = index - centerIndex

            val xOffset = when {
                totalCards <= 6 -> offsetFromCenter * 30
                totalCards <= 10 -> offsetFromCenter * 24
                else -> offsetFromCenter * 16
            }

            val rotation = when {
                totalCards <= 6 -> offsetFromCenter * 5f
                totalCards <= 10 -> offsetFromCenter * 3f
                else -> offsetFromCenter * 2f
            }

            val yOffset = kotlin.math.abs(offsetFromCenter) * 1.5f

            // Состояние перетаскивания
            var dragOffset by remember { mutableStateOf(Offset.Zero) }
            var isDragging by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .offset { IntOffset(xOffset.dp.roundToPx(), yOffset.dp.roundToPx()) }
                    .graphicsLayer {
                        rotationZ = rotation
                        translationY = if (isDragging) -50f else if (isSelected) -30f else 0f
                        scaleX = if (isDragging) 1.2f else if (isSelected) 1.1f else 1f
                        scaleY = if (isDragging) 1.2f else if (isSelected) 1.1f else 1f
                        translationX = dragOffset.x
                        translationY += dragOffset.y
                    }
                    .pointerInput(card.id) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                onCardSelected(card)
                            },
                            onDragEnd = {
                                isDragging = false
                                if (dragOffset.y < -100f) {
                                    // Потянули вверх — бросаем на стол
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