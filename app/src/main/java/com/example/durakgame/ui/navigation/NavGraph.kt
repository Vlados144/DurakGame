package com.example.durakgame.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.durakgame.engine.model.GameState
import com.example.durakgame.network.NsdHelper
import com.example.durakgame.ui.screens.create_game.CreateGameScreen
import com.example.durakgame.ui.screens.find_game.FindGameScreen
import com.example.durakgame.ui.screens.game.GameScreen
import com.example.durakgame.ui.screens.lobby.GameLobbyScreen
import com.example.durakgame.ui.screens.menu.MenuScreen
import com.example.durakgame.ui.screens.profile.ProfileScreen
import com.example.durakgame.ui.screens.result.ResultScreen
import com.example.durakgame.ui.viewmodels.GameViewModel

sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object Profile : Screen("profile")
    object CreateGame : Screen("create_game")
    object FindGame : Screen("find_game")
    object Lobby : Screen("lobby/{gameCode}/{isHost}") {
        fun createRoute(gameCode: String, isHost: Boolean) = "lobby/$gameCode/$isHost"
    }
    object Game : Screen("game/{gameCode}") {
        fun createRoute(gameCode: String) = "game/$gameCode"
    }
    object Result : Screen("result")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val nsdHelper = remember { NsdHelper(context) }
    val gameViewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(nsdHelper))

    NavHost(
        navController = navController,
        startDestination = Screen.Menu.route
    ) {
        composable(Screen.Menu.route) {
            MenuScreen(
                onCreateGame = { navController.navigate(Screen.CreateGame.route) },
                onFindGame = { navController.navigate(Screen.FindGame.route) },
                onProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CreateGame.route) {
            CreateGameScreen(
                onGameCreated = { gameCode ->
                    navController.navigate(Screen.Lobby.createRoute(gameCode, true))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FindGame.route) {
            FindGameScreen(
                networkManager = gameViewModel.getNetworkManager(),
                onGameJoined = { hostAddress ->
                    navController.navigate(Screen.Lobby.createRoute(hostAddress, false))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Lobby.route) { backStackEntry ->
            val gameCode = backStackEntry.arguments?.getString("gameCode") ?: ""
            val isHost = backStackEntry.arguments?.getString("isHost")?.toBoolean() ?: false
            GameLobbyScreen(
                gameCode = gameCode,
                isHost = isHost,
                onGameStarted = {
                    navController.navigate(Screen.Game.createRoute(gameCode))
                },
                onBack = {
                    gameViewModel.leaveGame()
                    navController.popBackStack()
                },
                viewModel = gameViewModel
            )
        }

        composable(Screen.Game.route) { backStackEntry ->
            val hostAddress = backStackEntry.arguments?.getString("gameCode") ?: ""
            GameScreen(
                hostAddress = hostAddress,
                onGameOver = {
                    navController.navigate(Screen.Result.route)
                },
                onBack = {
                    gameViewModel.leaveGame()
                    navController.popBackStack()
                },
                viewModel = gameViewModel
            )
        }

        composable(Screen.Result.route) {
            val players = gameViewModel.gameState.value?.players ?: emptyList()
            val betAmount = gameViewModel.getBetAmount()
            val rematchStatus by gameViewModel.rematchStatus.collectAsState()
            val playerLeft by gameViewModel.playerLeft.collectAsState()
            val gameState by gameViewModel.gameState.collectAsState()

            LaunchedEffect(gameState) {
                if (gameState != null && gameState!!.phase == GameState.GamePhase.PLAYING) {
                    navController.navigate(Screen.Game.createRoute(gameViewModel.gameCode.value))
                }
            }

            ResultScreen(
                players = players,
                betAmount = betAmount,
                rematchStatus = rematchStatus,
                playerLeft = playerLeft,
                onBackToMenu = {
                    gameViewModel.playerLeft()
                    gameViewModel.leaveGame()
                    navController.popBackStack(Screen.Menu.route, inclusive = false)
                },
                onRematch = { gameViewModel.requestRematch() },
                onCancelRematch = { gameViewModel.cancelRematch() },
                isHost = gameViewModel.isHost()

            )
        }
    }
}