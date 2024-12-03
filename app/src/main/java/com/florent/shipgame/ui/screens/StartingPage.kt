package com.florent.shipgame.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.florent.shipgame.R

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