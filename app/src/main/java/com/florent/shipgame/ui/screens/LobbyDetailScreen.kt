package com.florent.shipgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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