package com.florent.shipgame.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.florent.shipgame.ui.screens.HostScreen
import com.florent.shipgame.ui.screens.JoinScreen
import com.florent.shipgame.ui.screens.LobbyDetailsScreen
import com.florent.shipgame.ui.screens.StartingPage

// Navigation Setup
@Composable
fun Navigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "startingPage",
        modifier = modifier
    ) {
        composable("startingPage") {
            StartingPage(
                onHostClick = { navController.navigate("hostScreen") },
                onJoinClick = { navController.navigate("joinScreen") }
            )
        }
        composable("hostScreen") {
            HostScreen(onLobbyCreated = { roomCode ->
                // Navigate to the lobby details screen with the room code
                navController.navigate("lobbyDetails/$roomCode")
            })
        }
        composable("joinScreen") {
            JoinScreen(onLobbySelected = { roomCode ->
                // Navigate to the lobby details screen with the selected lobby
                navController.navigate("lobbyDetails/$roomCode")
            })
        }

        composable(
            "lobbyDetails/{roomCode}",
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            LobbyDetailsScreen(roomCode)
        }
    }
}