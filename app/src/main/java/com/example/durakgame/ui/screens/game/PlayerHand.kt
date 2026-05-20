package com.example.durakgame.ui.screens.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.zIndex
import com.example.durakgame.engine.model.Card
import com.example.durakgame.ui.viewmodels.GameViewModel
import kotlin.math.roundToInt

@Composable
fun PlayerHand(
    cards: List<Card>,
    selectedCardId: String?,
    viewModel: GameViewModel,
    skinId: String = "classic",
    modifier: Modifier = Modifier
) {
    val activeIndex = cards.indexOfFirst { it.id == selectedCardId }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        cards.forEachIndexed { index, card ->
            val isSelected = card.id == selectedCardId
            val totalCards = cards.size
            val centerIndex = (totalCards - 1) / 2f
            val offsetFromCenter = index - centerIndex

            // Уплотнение колоды: общая длина фиксируется на значении для 6 карт (5 интервалов по 48dp = 240dp)
            // При количестве карт > 5 шаг уменьшается с каждой новой картой, сохраняя общую ширину 240dp.
            val step = if (totalCards <= 1) 0f else {
                val maxWidth = 240f // (6 - 1) * 48f
                val calculatedStep = maxWidth / (totalCards - 1)
                // Ограничиваем максимальный шаг 60dp для малого количества карт, чтобы они не разлетались слишком широко
                if (calculatedStep > 60f) 60f else calculatedStep
            }

            val targetNeighborOffset = when {
                activeIndex == -1 || index == activeIndex -> 0f
                index < activeIndex -> -45f
                else -> 45f
            }
            val neighborOffset by animateFloatAsState(
                targetValue = targetNeighborOffset,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )

            val targetX = offsetFromCenter * step + neighborOffset
            val targetY = kotlin.math.abs(offsetFromCenter) * 5f + 95f 

            val animatedX by animateFloatAsState(targetValue = targetX, animationSpec = spring(dampingRatio = 0.8f))
            val animatedY by animateFloatAsState(targetValue = targetY, animationSpec = spring(dampingRatio = 0.8f))

            var dragOffset by remember { mutableStateOf(Offset.Zero) }
            var currentFingerPositionInRoot by remember { mutableStateOf(Offset.Zero) }

            val rotation by animateFloatAsState(
                targetValue = if (isSelected) 0f else offsetFromCenter * 4f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )

            val verticalLift by animateFloatAsState(
                targetValue = if (isSelected) -45f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )

            Box(
                modifier = Modifier
                    .zIndex(if (isSelected) 100f else index.toFloat())
                    .offset { 
                        IntOffset(
                            (animatedX.dp.toPx() + dragOffset.x).roundToInt(),
                            (animatedY.dp.toPx() + dragOffset.y + verticalLift.dp.toPx()).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = if (isSelected) 1.1f else 1f
                        scaleY = if (isSelected) 1.1f else 1f
                    }
                    // Замеряем позицию ПОСЛЕ смещения для абсолютной точности хитбокса
                    .onGloballyPositioned { layoutCoordinates ->
                        if (isSelected) {
                            currentFingerPositionInRoot = layoutCoordinates.positionInRoot()
                        }
                    }
                    .pointerInput(card.id) {
                        detectDragGestures(
                            onDragStart = { 
                                viewModel.onCardDragStart(card.id) 
                            },
                            onDragEnd = {
                                // Используем координаты центра карты/пальца для завершения
                                viewModel.onCardDragEnd(card, currentFingerPositionInRoot + Offset(size.width/2f, size.height/4f))
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                viewModel.onCardDragCancel()
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                // Передаем во ViewModel чистую позицию пальца на экране
                                viewModel.onCardDragged(currentFingerPositionInRoot + change.position)
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.selectCard(if (isSelected) null else card.id)
                    }
            ) {
                CardView(card = card, skinId = skinId, selected = false)
            }
        }
    }
}
