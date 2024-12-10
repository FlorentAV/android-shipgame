package com.florent.shipgame
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.compose.ShipGameTheme
import com.florent.shipgame.logic.GameModel
import com.florent.shipgame.ui.navigation.Navigation
import com.florent.shipgame.utils.AppLifecycleObserver


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameModel = GameModel()
        val sharedPreferences = getSharedPreferences("ShipGamePrefs", Context.MODE_PRIVATE)
        val playerId = getSharedPreferences("ShipGamePrefs", Context.MODE_PRIVATE)
            .getString("playerId", null)

        if (playerId != null) {
            lifecycle.addObserver(AppLifecycleObserver(playerId))
        }
        if (playerId == null) {
            Log.e("MainActivity", "playerId is null. Generate a new")
        }

        setContent {
            ShipGameTheme {
                val navController = rememberNavController()
                Navigation(navController = navController, gameModel = gameModel)
            }
        }
    }
}