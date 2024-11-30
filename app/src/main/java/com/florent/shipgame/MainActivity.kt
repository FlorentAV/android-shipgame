package com.florent.shipgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.florent.shipgame.ui.theme.ShipGameTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.florent.shipgame.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.Typography
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.database.*
import androidx.compose.runtime.DisposableEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.navArgument
import androidx.navigation.NavType




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShipGameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Navigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}


// Starting page of the app with Join and Host option
@Composable
fun StartingPage(
    onHostClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {




        // Title image
        Image(
            painter = rememberAsyncImagePainter(model = R.drawable.img),
            contentDescription = "Title Image",
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        )

        // Ship image
        Image(
            painter = rememberAsyncImagePainter(model = R.drawable.battle),
            contentDescription = "Ship Image",
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        )



        Spacer(modifier = Modifier.height(32.dp))

        // Host Button
        Button(
            onClick = onHostClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Host", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Join Button
        Button(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Join", fontSize = 18.sp)
        }
    }
}


// Placeholder Host Screen
@Composable
fun HostScreen(onLobbyCreated: (String) -> Unit) {
    var playerName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }

    val database = FirebaseDatabase.getInstance()
    val lobbiesRef = database.getReference("lobbies")


    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Input for Player Name
        Text(text = "Enter Your Name", style = MaterialTheme.typography.titleMedium)
        TextField(
            value = playerName,
            onValueChange = { playerName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Player Name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Create Lobby Button
        Button(onClick = {
            if (playerName.isBlank()) {

                Toast.makeText(context, "Player name cannot be empty!", Toast.LENGTH_SHORT).show()
                return@Button
            }

            try {
                // Generate a unique room code
                roomCode = UUID.randomUUID().toString().take(6)

                // Create the lobby in Firebase
                val lobbyName = "${playerName}'s Lobby"
                val lobbyData = mapOf(
                    "lobbyName" to lobbyName,
                    "hostName" to playerName,
                    "players" to mapOf("player1" to playerName)
                )

                lobbiesRef.child(roomCode).setValue(lobbyData)
                    .addOnSuccessListener {
                        onLobbyCreated(roomCode) // Navigate to LobbyDetailsScreen
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Failed to create lobby: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Create Lobby")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Room Code
        if (roomCode.isNotEmpty()) {
            Text(
                text = "Room Code: $roomCode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@Composable
fun JoinScreen(onLobbySelected: (String) -> Unit) {
    var playerName by remember { mutableStateOf("") }
    val lobbies = remember { mutableStateListOf<Map<String, String>>() }

    val database = FirebaseDatabase.getInstance()
    val lobbiesRef = database.getReference("lobbies")

    // Fetch the list of lobbies
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lobbies.clear()
                snapshot.children.forEach { child ->
                    val lobby = child.value as? Map<String, String> ?: return
                    lobbies.add(lobby + ("id" to child.key!!))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        lobbiesRef.addValueEventListener(listener)

        onDispose { lobbiesRef.removeEventListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        Text(text = "Enter Your Name", style = MaterialTheme.typography.titleMedium)
        TextField(
            value = playerName,
            onValueChange = { playerName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Player Name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Lobby list
        Text(text = "Available Lobbies", style = MaterialTheme.typography.titleMedium)
        lobbies.forEach { lobby ->
            Button(
                onClick = {
                    if (playerName.isNotBlank()) {
                        val roomCode = lobby["id"] ?: return@Button

                        // Add the player to the selected lobby
                        lobbiesRef.child(roomCode).child("players")
                            .push().setValue(playerName)

                        // Navigate to Lobby Details
                        onLobbySelected(roomCode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = lobby["lobbyName"] ?: "Unknown Lobby")
            }
        }
    }
}



@Composable
fun LobbyDetailsScreen(roomCode: String) {
    val players = remember { mutableStateListOf<String>() }
    val lobbyName = remember { mutableStateOf("") }

    val database = FirebaseDatabase.getInstance()
    val lobbyRef = database.getReference("lobbies").child(roomCode)

    // Fetch lobby details
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lobbyData = snapshot.value as? Map<String, Any> ?: return
                lobbyName.value = lobbyData["lobbyName"] as? String ?: ""
                val playersMap = lobbyData["players"] as? Map<String, String> ?: emptyMap()
                players.clear()
                players.addAll(playersMap.values)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
        lobbyRef.addValueEventListener(listener)

        onDispose { lobbyRef.removeEventListener(listener) }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Lobby: ${lobbyName.value}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Players:",
            style = MaterialTheme.typography.titleMedium
        )
        players.forEach { player ->
            Text(text = player, style = MaterialTheme.typography.bodyLarge)
        }
    }
}




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







@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShipGameTheme {

    }
}