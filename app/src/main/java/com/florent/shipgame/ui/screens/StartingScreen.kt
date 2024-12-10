package com.florent.shipgame.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.florent.shipgame.R
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.models.Player
import java.util.UUID




// Start screen with namefield and join button
@Composable
fun StartingScreen(
    navController: NavController,
    gameModel: GameModel
) {
    val context = LocalContext.current
    var playerName by remember { mutableStateOf("") }
    val sharedPreferences = context.getSharedPreferences("ShipGamePrefs", Context.MODE_PRIVATE)
    val playerId = sharedPreferences.getString("id", null) ?: run {
        val newId = UUID.randomUUID().toString()
        sharedPreferences.edit().putString("id", newId).apply()
        newId
    } // Shared pref to store player from same device so db does not get unnecessary players



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Image
            Image(
                painter = rememberAsyncImagePainter(model = R.drawable.img), // Replace with your resource
                contentDescription = "Title Image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )

            // Ship Image logo
            Image(
                painter = rememberAsyncImagePainter(model = R.drawable.battle), // Replace with your resource
                contentDescription = "Ship Image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // TextField for player name
            TextField(
                value = playerName,
                onValueChange = { playerName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text("Enter Your Name") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Join Button
            Button(
                onClick = {
                    if (playerName.isNotBlank()) {
                        playerId?.let { id ->
                            gameModel.createPlayer(
                                Player(id, playerName, status = "lobby")
                            ) { success ->
                                if (success != null) {
                                    navController.navigate("playerListScreen/$id/$playerName")
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error creating player. Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please enter a valid name.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                enabled = playerName.isNotBlank()
            ) {
                Text(
                    text = "Join",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp)
                )
            }
        }
    }
}
