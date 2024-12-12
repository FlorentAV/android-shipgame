package com.florent.shipgame.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
        var hasNavigatedToShipPlacement = false // Prevent duplication of ShipPlacementScreen

        // Observe the playerscurrent GameId and listen for challenges
        gameModel.observePlayerGame(playerId) { foundGameId ->
            gameId = foundGameId

            gameModel.observeGameState(foundGameId) { gameState ->
                if (gameState == "placing-ships" && !hasNavigatedToShipPlacement) {
                    hasNavigatedToShipPlacement = true // Set the flag to true
                    Log.d("PlayerListScreen", "Navigating to ShipPlacementScreen")
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
            TopAppBar(
                title = {
                    Text(
                        text = "Hello there, $playerName!",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = "Available Players",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.Start)
            )

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(players.entries.toList()) { (id, player) ->
                    if (id != playerId) { // Exclude the user from seeing their own name on the list
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Button(
                                    onClick = {
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
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text("Challenge")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Popup for incoming challenge
    if (showChallengePopup) {
        var senderName by remember { mutableStateOf("Unknown Player") }

        // Fetch the senderId and resolve the name
        LaunchedEffect(incomingChallengeId) {
            gameModel.fetchChallengeSender(incomingChallengeId) { fetchedSenderId ->
                val fetchedName = gameModel.playerMap.value[fetchedSenderId]?.name
                senderName = fetchedName ?: "Unknown Player"
            }
        }

        AlertDialog(
            onDismissRequest = { showChallengePopup = false },
            title = {
                Text(
                    text = "You've been challenged!",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "$senderName has challenged you!",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(onClick = {
                    gameModel.acceptChallenge(incomingChallengeId, playerId) { createdGameId ->
                        if (createdGameId != null) {
                            navController.navigate("shipPlacementScreen/$createdGameId/$playerId")
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to accept challenge. Try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Text("Accept")
                }
            },
            dismissButton = {
                Button(onClick = {
                    gameModel.updateChallengeStatus(incomingChallengeId, "declined") {}
                    showChallengePopup = false
                }) {
                    Text("Decline")
                }
            }
        )
    }
}
