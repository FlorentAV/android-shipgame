package com.florent.shipgame.ui.screens

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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