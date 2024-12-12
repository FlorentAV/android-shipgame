package com.florent.shipgame.ui.components
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.florent.shipgame.R
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.models.Coordinate
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
    val playerReady = remember { mutableStateOf(false) }


    val savedPlacedShips = rememberSaveable(saver = shipSaver()) { mutableStateListOf() }
    var currentShipSize by remember { mutableIntStateOf(4) }
    var isVertical by remember { mutableStateOf(true) }
    val placedShips = remember { savedPlacedShips }
    val gridHighlights = remember { mutableStateListOf<Coordinate>() }
    var showWaitingMessage by remember { mutableStateOf(false) }
    var isDonePlacing by remember { mutableStateOf(false) }


    val shipSizes = listOf(4, 3, 2, 2, 1, 1) // Ship sizes in order, places biggest first
    val totalShips = shipSizes.size

    // Check if all ships are placed
    val allShipsPlaced = placedShips.size == totalShips

    val gridSize = 10


    BackHandler {

    }

    Log.d("ShipPlacementScreen", "Shiplacnment launched")
    LaunchedEffect(gameId) {
        gameModel.observePlayerState(playerId) { playerStatus ->
            Log.d("ShipPlacementScreen", "Player status changed: $playerStatus")
            if (playerStatus == "ready") {
                playerReady.value = true

            }
            gameModel.observeGameState(gameId) { gameState -> // Navigate to gamescreen when state is in-game
                if (gameState == "in-game" && !isDonePlacing) {
                    isDonePlacing = true
                    Log.d("ShipPlacementScreen", "Navigating to game screen")
                    navController.navigate("gameScreen/$gameId/$playerId")



                }
            }
        }
    }

    // Helper to highlight positions
    fun calculateHighlightPositions(start: Coordinate): List<Coordinate> {
        val positions = mutableListOf<Coordinate>()
        for (i in 0 until currentShipSize) {
            val position = if (isVertical) {
                Coordinate(start.x + i, start.y)
            } else {
                Coordinate(start.x, start.y + i)
            }
            positions.add(position)
        }
        return positions
    }

    // Validate ship placement
    fun isPlacementValid(positions: List<Coordinate>): Boolean {
        return positions.all { pos ->
            pos.x in 0 until gridSize && pos.y in 0 until gridSize &&
                    placedShips.none { ship -> ship.positions.contains(pos) }
        }
    }

    // Place ship
    fun placeShip(start: Coordinate) {
        if (allShipsPlaced) {
            Toast.makeText(context, "All ships are already placed!", Toast.LENGTH_SHORT).show()
            return
        }

        val positions = calculateHighlightPositions(start)
        if (isPlacementValid(positions)) {
            placedShips.add(Ship(size = currentShipSize, positions = positions.toMutableList()))

            // Move to the next ship, if there are none left, show a confirmation message
            if (placedShips.size < totalShips) {
                currentShipSize = shipSizes[placedShips.size]
            } else {
                Toast.makeText(context, "All ships placed! Confirm to proceed.", Toast.LENGTH_SHORT)
                    .show()
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
                // Ship border grid
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .size(320.dp)
                        .border(2.dp, Color.Black)
                ) {
                    // Water background image
                    Image(
                        painter = painterResource(id = R.drawable.water_background),
                        contentDescription = "water background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay the grid cells
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (row in 0 until gridSize) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (col in 0 until gridSize) {
                                    val position = Coordinate(row, col)
                                    val isHighlighted = gridHighlights.contains(position)
                                    val isOccupied =
                                        placedShips.any { ship -> ship.positions.contains(position) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .border(
                                                1.dp,
                                                Color.Black
                                            )
                                            .background(
                                                when {
                                                    isOccupied -> Color.Green
                                                    isHighlighted -> if (isPlacementValid(
                                                            gridHighlights
                                                        )
                                                    ) Color.Gray else Color.Red

                                                    else -> Color.Transparent
                                                }
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        gridHighlights.clear()
                                                        gridHighlights.addAll(
                                                            calculateHighlightPositions(position)
                                                        )
                                                    }
                                                )
                                            }
                                    )
                                }
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
                            val start = gridHighlights.first()
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

                // Confirm Button (enabled when all ships are placed)
                Button(
                    onClick = {
                        showWaitingMessage = true
                        gameModel.updatePlayerStatus(playerId, "ready") {}
                        gameModel.saveShips(gameId, playerId, placedShips) {}
                        gameModel.checkIfAllShipsPlaced(gameId) {
                            if (it) {
                                gameModel.transitionGameState(gameId, "in-game") {}
                            }
                        }
                    },
                    enabled = allShipsPlaced

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
@Composable
fun shipSaver() = Saver<MutableList<Ship>, List<Ship>>(
    save = { it.toList() },
    restore = { it.toMutableList() }
)