package com.florent.shipgame
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.florent.shipgame.ui.theme.ShipGameTheme
import com.florent.shipgame.ui.navigation.Navigation


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


