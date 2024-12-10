package com.florent.shipgame.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.models.Ship


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    gameModel: GameModel,
    gameId: String?
) {
    // States and data
    val players by gameModel.playerMap.collectAsStateWithLifecycle()
    val games by gameModel.gameMap.collectAsStateWithLifecycle()
    val localPlayerId by gameModel.localPlayerId.collectAsStateWithLifecycle()
    val game = gameId?.let { games[it] }

    // Exit early if player or game is missing
    if (game == null || localPlayerId == null) {
        navController.popBackStack()
        return
    }

    // Determine Current Player's Role
    val isPlayer1 = game.player1Id == localPlayerId
    val currentPlayerShips = if (isPlayer1) game.player1Ships else game.player2Ships
    val opponentShips = if (isPlayer1) game.player2Ships else game.player1Ships
    val currentHits = if (isPlayer1) game.player1Hits else game.player2Hits
    val currentMisses = if (isPlayer1) game.player1Misses else game.player2Misses
    val opponentHits = if (isPlayer1) game.player2Hits else game.player1Hits

    // Observing game state to update screen after
    val isCurrentTurn = game.currentTurn == localPlayerId
    val gameStateMessage = when (game.gameState) {
        "placing-ships" -> "Players are placing their ships."
        "in-progress" -> if (isCurrentTurn) "It's your turn!" else "Waiting for opponent's turn."
        "game-over" -> if (game.currentTurn == localPlayerId) "Victory!" else "Defeat"
        else -> "UNKOWN"
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Battleship - Game ID: $gameId") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Section with Game State and Player Info
                HeaderSection(
                    player1 = players[game.player1Id]?.name ?: "Player 1",
                    player2 = players[game.player2Id]?.name ?: "Player 2",
                    gameStateMessage = gameStateMessage
                )

                // Game Boards Section
                GameBoards(
                    currentShips = currentPlayerShips,
                    opponentHits = opponentHits,
                    onCellClick = { position ->
                        if (isCurrentTurn && game.gameState == "in-progress") {
                            gameModel.takeTurn(gameId, localPlayerId!!, position) { result ->
                                if (result.contains("wins")) {
                                    gameModel.transitionGameState(gameId, "game-over") {}
                                }
                            }
                        }
                    }
                )

                // Footer Actions
                FooterActions(
                    isCurrentTurn = isCurrentTurn,
                    onBackToLobby = { navController.popBackStack() }
                )
            }
        }
    )
}

@Composable
fun HeaderSection(player1: String, player2: String, gameStateMessage: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = gameStateMessage, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Player 1: $player1", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Player 2: $player2", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun GameBoards(
    currentShips: List<Ship>,
    opponentHits: List<Pair<Int, Int>>,
    onCellClick: (Pair<Int, Int>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Your Board
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Your Board", style = MaterialTheme.typography.titleMedium)
            GameBoardView(
                ships = currentShips,
                hits = emptyList(),
                isInteractive = false,
                onCellClick = {}
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Opponent's Board
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Opponent's Board", style = MaterialTheme.typography.titleMedium)
            GameBoardView(
                ships = emptyList(), // Don't show opponent's ships
                hits = opponentHits,
                isInteractive = true,
                onCellClick = onCellClick
            )
        }
    }
}

@Composable
fun FooterActions(
    isCurrentTurn: Boolean,
    onBackToLobby: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCurrentTurn) {
            Text("Waiting for opponent...", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBackToLobby) {
            Text("Back to Lobby")
        }
    }
}

@Composable
fun GameBoardView(
    ships: List<Ship>,
    hits: List<Pair<Int, Int>>,
    isInteractive: Boolean,
    onCellClick: (Pair<Int, Int>) -> Unit
) {
    val gridSize = 10
    Column {
        for (row in 0 until gridSize) {
            Row {
                for (col in 0 until gridSize) {
                    val position = Pair(row, col)
                    val isShip = ships.any { it.positions.contains(position) }
                    val isHit = hits.contains(position)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color.Black)
                            .background(
                                when {
                                    isShip && isHit -> Color.Red // Ship hit
                                    isShip -> Color.Blue        // Ship unhit
                                    isHit -> Color.Gray         // Miss
                                    else -> Color.White         // Empty
                                }
                            )
                            .clickable(enabled = isInteractive) {
                                onCellClick(position)
                            }
                    )
                }
            }
        }
    }
}
