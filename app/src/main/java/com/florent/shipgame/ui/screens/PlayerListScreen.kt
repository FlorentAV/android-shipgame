package com.florent.shipgame.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.florent.shipgame.logic.GameModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListScreen(
    navController: NavController,
    gameModel: GameModel,
    playerId: String,
    playerName: String
) {
    val players = gameModel.playerMap.collectAsState().value
    var incomingChallengeId by remember { mutableStateOf("") }
    var showChallengePopup by remember { mutableStateOf(false) }
    var gameId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(playerId) {
        // Observe the players currentGameId and listening for challenges
        gameModel.observePlayerGame(playerId) { foundGameId ->
            gameId = foundGameId
            gameModel.observeGameState(foundGameId) { gameState ->
                if (gameState == "placing-ships") {
                    navController.navigate("shipPlacementScreen/$foundGameId/$playerId")
                }
            }
        }
        gameModel.listenForChallenges(playerId) { senderId, challengeId ->
            incomingChallengeId = challengeId
            showChallengePopup = true
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hello there, $playerName!") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(players.entries.toList()) { (id, player) ->
                if (id != playerId) { // Exclude the user from seeing their own name on the list
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = {
                            gameModel.sendChallenge(playerId, id) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Challenge sent to ${player.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to send challenge.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }) {
                            Text("Challenge")
                        }
                    }
                }
            }
        }
    }

    // Popup for sending challenge (Waiting for response adding after)
    if (showChallengePopup) {
        val senderName = players[incomingChallengeId]?.name ?: "Unknown Player"
        AlertDialog(
            onDismissRequest = { showChallengePopup = false },
            title = { Text("You've been challenged!") },
            text = { Text("$senderName has challenged you!") },
            confirmButton = {
                Button(onClick = {
                    gameModel.acceptChallenge(incomingChallengeId, playerId) { createdGameId ->
                        if (createdGameId != null) {
                            gameId = createdGameId // Save the gameId
                            navController.navigate("shipPlacementScreen/$createdGameId/$playerId")
                        } else {
                            Toast.makeText(context, "Failed to accept challenge.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Accept") }
            },
            dismissButton = {
                Button(onClick = {
                    gameModel.updateChallengeStatus(incomingChallengeId, "declined") {}
                    showChallengePopup = false
                }) { Text("Decline") }
            }
        )
    }
}