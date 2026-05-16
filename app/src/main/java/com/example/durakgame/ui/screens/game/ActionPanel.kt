package com.example.durakgame.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
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
            state.text.isNotEmpty() -> Color(0xFF1A1A2E).copy(alpha = 0.8f)
            else -> Color.Transparent
        },
        label = "panelColor"
    )

    val height by animateDpAsState(
        targetValue = if (state.text.isEmpty()) 0.dp else 64.dp,
        label = "panelHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Анимированный текст инструкций
            AnimatedContent(
                targetState = state.text,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut())
                        .using(SizeTransform(clip = false))
                },
                label = "textAnimation",
                modifier = Modifier.weight(1f)
            ) { targetText ->
                Text(
                    text = targetText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            // Кнопки с анимацией появления
            Row {
                AnimatedVisibility(
                    visible = state.showTakeButton,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Button(
                        onClick = onTakeCards,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.padding(start = 4.dp).height(40.dp)
                    ) {
                        Text("ВЗЯТЬ", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }

                AnimatedVisibility(
                    visible = state.showPassButton,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Button(
                        onClick = onPass,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.9f),
                            contentColor = Color(0xFF37474F)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.padding(start = 4.dp).height(40.dp)
                    ) {
                        Text("ПАС", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }

                AnimatedVisibility(
                    visible = state.showBitoButton,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Button(
                        onClick = onBito,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.padding(start = 4.dp).height(40.dp)
                    ) {
                        Text("БИТО", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
