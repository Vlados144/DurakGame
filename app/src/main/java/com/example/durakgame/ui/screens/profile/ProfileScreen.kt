package com.example.durakgame.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.durakgame.ui.components.GameBackground
import com.example.durakgame.ui.components.GoldButton
import com.example.durakgame.ui.components.DarkButton
import androidx.compose.ui.graphics.Color
import com.example.durakgame.DurakApplication
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as DurakApplication
    val userRepository = app.userRepository

    var nickname by remember { mutableStateOf(userRepository.getUser().nickname) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editedNickname by remember { mutableStateOf(nickname) }

    LaunchedEffect(Unit) {
        val user = userRepository.getUser()
        nickname = user.nickname
        val avatarFile = File(context.filesDir, "avatar.jpg")
        if (avatarFile.exists()) {
            avatarUri = Uri.fromFile(avatarFile)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val avatarFile = File(context.filesDir, "avatar.jpg")
            val outputStream = FileOutputStream(avatarFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            // Обновляем URI
            avatarUri = Uri.fromFile(avatarFile)
        }
    }

    GameBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Аватарка (теперь прямоугольная как в GameScreen)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Аватар",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("📷", fontSize = 48.sp)
                }
            }

            Text(
                "Нажмите чтобы изменить",
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Никнейм
            if (isEditing) {
                OutlinedTextField(
                    value = editedNickname,
                    onValueChange = { editedNickname = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkButton(text = "Сохранить", onClick = {
                        userRepository.updateNickname(editedNickname)
                        nickname = editedNickname
                        isEditing = false
                    })
                    DarkButton(text = "Отмена", onClick = {
                        editedNickname = nickname
                        isEditing = false
                    })
                }
            } else {
                Text(
                    text = nickname,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                DarkButton(text = "✏ Изменить имя", onClick = {
                    isEditing = true
                })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Статистика
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Статистика",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow("Всего игр", "0")
                    StatRow("Побед", "0")
                    StatRow("Поражений", "0")
                    StatRow("Винрейт", "0%")
                    StatRow("Баланс", "1000 💰")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GoldButton(
                text = "Назад",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}