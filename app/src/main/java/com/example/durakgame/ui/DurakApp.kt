package com.example.durakgame.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.durakgame.ui.navigation.NavGraph
@Composable
fun DurakApp() {
    MaterialTheme {
        NavGraph()
    }
}