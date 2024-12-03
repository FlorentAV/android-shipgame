package com.florent.shipgame.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

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
