package com.florent.shipgame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.florent.shipgame.R
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.models.Coordinate
import com.florent.shipgame.models.Ship


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    gameModel: GameModel,
    gameId: String,
    playerId: String
) {
    val game = gameModel.gameMap.collectAsState().value[gameId]
    val localPlayerId = gameModel.localPlayerId.collectAsState().value
    val players = gameModel.playerMap.collectAsState().value
    val opponentId = if (playerId == game?.player1Id) game?.player2Id else game?.player1Id
    val opponentName = players[opponentId]?.name ?: "Opponent"

    val myShips = if (playerId == game?.player1Id) game?.player1Ships else game?.player2Ships
    val myHits = if (playerId == game?.player1Id) game?.player1Hits else game?.player2Hits
    val myMisses = if (playerId == game?.player1Id) game?.player1Misses else game?.player2Misses
    val opponentHits = if (playerId == game?.player1Id) game?.player2Hits else game?.player1Hits
    val opponentMisses = if (playerId == game?.player1Id) game?.player2Misses else game?.player1Misses

    // Determine whose turn it is
    val turnMessage = if (game?.currentTurn == playerId) {
        "Your turn"
    } else {
        "$opponentName's turn"
    }

    // Navigate to result screen if game is over
    LaunchedEffect(gameId) {
        gameModel.observeGameState(gameId) { gameState ->
            when {
                gameState.endsWith("_won") -> {
                    val winnerId = gameState.removeSuffix("_won")
                    val winnerMessage = if (winnerId == playerId) "You won!" else "You lost!"
                    navController.navigate("resultScreen/$winnerMessage")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(turnMessage) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Opponent's Board",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                GameBoardView(
                    ships = emptyList(), // Opponent's ships are hidden
                    hits = myHits ?: emptyList(),
                    misses = myMisses ?: emptyList(),
                    isInteractive = game?.currentTurn == playerId,
                    onCellClick = { coordinate ->
                        if (game?.currentTurn == playerId) {
                            gameModel.takeTurn(gameId, playerId, coordinate) {}
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your Board",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                GameBoardView(
                    ships = myShips ?: emptyList(), // hide ships visibility for opponent
                    hits = opponentHits ?: emptyList(),
                    misses = opponentMisses ?: emptyList(),
                    isInteractive = false,
                    onCellClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }
        }
    )
}

@Composable
fun GameBoardView(
    ships: List<Ship>,
    hits: List<Coordinate>,
    misses: List<Coordinate>,
    isInteractive: Boolean,
    onCellClick: (Coordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = 10
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.LightGray)
    ) {
        // Add a background image
        Image(
            painter = painterResource(id = R.drawable.water_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Grid overlay
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until gridSize) {
                Row(modifier = Modifier.weight(1f)) {
                    for (col in 0 until gridSize) {
                        val position = Coordinate(row, col)
                        val isShip = ships.any { ship -> ship.positions.contains(position) }
                        val isHit = hits.contains(position)
                        val isMiss = misses.contains(position)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f) // Each cell is square
                                .border(1.dp, Color.Black)
                                .background(
                                    when {
                                        isHit -> Color.Red
                                        isMiss -> Color.Gray
                                        isShip -> Color.Blue
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isInteractive) Modifier.clickable { onCellClick(position) }
                                    else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}