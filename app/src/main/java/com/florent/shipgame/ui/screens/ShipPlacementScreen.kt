package com.florent.shipgame.ui.components
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.models.Ship


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipPlacementScreen(
    navController: NavController,
    gameModel: GameModel,
    gameId: String,
    playerId: String
) {
    val context = LocalContext.current
    val players = gameModel.playerMap.collectAsState().value
    val opponentReady = remember { mutableStateOf(false) }

    var currentShipSize by remember { mutableStateOf(4) }
    var isVertical by remember { mutableStateOf(true) }
    val placedShips = remember { mutableStateListOf<Ship>() }
    val gridHighlights = remember { mutableStateListOf<Pair<Int, Int>>() }
    var showWaitingMessage by remember { mutableStateOf(false) }

    // Track the number of ships to place
    val shipSizes = listOf(4, 3, 2, 2, 1, 1) // Ship sizes in order, places biggest first
    val totalShips = shipSizes.size

    // Check if all ships are placed
    val allShipsPlaced = placedShips.size == totalShips

    val gridSize = 10 // Grid size for board

    // Observe opponent's ready status
    LaunchedEffect(gameId) {
        gameModel.observeGameState(gameId) { gameState ->
            if (gameState == "ready") {
                opponentReady.value = true
            }
        }
    }

    // Helper to highlight positions
    fun calculateHighlightPositions(start: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (x, y) = start
        return (0 until currentShipSize).mapNotNull { offset ->
            val pos = if (isVertical) Pair(x, y + offset) else Pair(x + offset, y)
            if (pos.first in 0 until gridSize && pos.second in 0 until gridSize) pos else null
        }
    }

    // Validate ship placement
    fun isPlacementValid(positions: List<Pair<Int, Int>>): Boolean {
        return positions.all { pos ->
            pos.first in 0 until gridSize && pos.second in 0 until gridSize &&
                    placedShips.none { ship -> ship.positions.contains(pos) }
        }
    }

    // Place ship
    fun placeShip(start: Pair<Int, Int>) {
        if (allShipsPlaced) {
            Toast.makeText(context, "All ships are already placed!", Toast.LENGTH_SHORT).show()
            return
        }

        val positions = calculateHighlightPositions(start)
        if (isPlacementValid(positions)) {
            placedShips.add(Ship(size = currentShipSize, positions = positions.toMutableList()))

            // Move to next ship, if there are no left, proceed down
            if (placedShips.size < totalShips) {
                currentShipSize = shipSizes[placedShips.size]
            } else {
                Toast.makeText(context, "All ships placed! Confirm to proceed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Invalid placement!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = players[playerId]?.name ?: "Player",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Opponent Ready Status
                if (opponentReady.value) {
                    Text("Opponent is ready!", color = Color.Green)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ship Placement Grid
                Column {
                    for (row in 0 until gridSize) {
                        Row {
                            for (col in 0 until gridSize) {
                                val position = Pair(row, col)
                                val isHighlighted = gridHighlights.contains(position)
                                val isOccupied = placedShips.any { ship -> ship.positions.contains(position) }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(1.dp, Color.Black)
                                        .background(
                                            when {
                                                isOccupied -> Color.Green
                                                isHighlighted -> if (isPlacementValid(gridHighlights)) Color.Gray else Color.Red
                                                else -> Color.White
                                            }
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    gridHighlights.clear()
                                                    gridHighlights.addAll(calculateHighlightPositions(position))
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Rotate Button
                    IconButton(onClick = {
                        isVertical = !isVertical
                        if (gridHighlights.isNotEmpty()) {
                            // Recalculate highlights with the new orientation
                            val start = gridHighlights.first() // Take the first highlighted position
                            gridHighlights.clear()
                            gridHighlights.addAll(calculateHighlightPositions(start))
                        }
                    }) {
                        Icon(Icons.Default.Rotate90DegreesCw, contentDescription = "Rotate")
                    }

                    // Place Ship Button
                    IconButton(onClick = {
                        if (gridHighlights.isNotEmpty()) {
                            placeShip(gridHighlights.first())
                            gridHighlights.clear()
                        }
                    }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Place Ship")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Button
                Button(
                    onClick = {
                        showWaitingMessage = true
                        gameModel.updatePlayerStatus(playerId, "ready") {}
                        gameModel.transitionGameState(gameId, "ready") {}
                    },
                    enabled = allShipsPlaced // Enable only when all ships are placed
                ) {
                    Text("Confirm")
                }

                if (showWaitingMessage) {
                    Text("Waiting for opponent to confirm...", color = Color.Gray)
                }
            }
        }
    )
}