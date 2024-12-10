package com.florent.shipgame.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.ui.components.ShipPlacementScreen
import com.florent.shipgame.ui.screens.GameScreen
import com.florent.shipgame.ui.screens.PlayerListScreen
import com.florent.shipgame.ui.screens.ResultScreen
import com.florent.shipgame.ui.screens.StartingScreen


// Navigation for different screens
@Composable
fun Navigation(navController: NavHostController, gameModel: GameModel) {
    NavHost(
        navController = navController,
        startDestination = "startingScreen"
    ) {
        // Starting Screen
        composable("startingScreen") {
            StartingScreen(
                navController = navController,
                gameModel = gameModel
            )
        }

        // Player List Screen
        composable(
            "playerListScreen/{playerId}/{playerName}",
            arguments = listOf(
                navArgument("playerId") { type = NavType.StringType },
                navArgument("playerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            val playerName = backStackEntry.arguments?.getString("playerName") ?: ""
            PlayerListScreen(
                navController = navController,
                gameModel = gameModel,
                playerId = playerId,
                playerName = playerName
            )
        }

        // Ship Placement Screen
        composable(
            "shipPlacementScreen/{gameId}/{playerId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            ShipPlacementScreen(
                navController = navController,
                gameModel = gameModel,
                gameId = gameId,
                playerId = playerId
            )
        }

        // Game Screen
        composable(
            "gameScreen/{gameId}/{playerId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            GameScreen(
                navController = navController,
                gameModel = gameModel,
                gameId = gameId
            )
        }

        // Result Screen
        composable(
            "resultScreen/{winnerMessage}",
            arguments = listOf(
                navArgument("winnerMessage") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val winnerMessage = backStackEntry.arguments?.getString("winnerMessage") ?: "Game Over"
            ResultScreen(
                winnerMessage = winnerMessage,
                onBackToLobby = { navController.navigate("startingScreen") }
            )
        }
    }
}
