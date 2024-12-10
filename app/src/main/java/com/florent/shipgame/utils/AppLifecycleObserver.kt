package com.florent.shipgame.utils

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class AppLifecycleObserver(private val playerId: String) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        val playerDoc = Firebase.firestore.collection("players").document(playerId)

        playerDoc.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Update the status to offline when player leave (To not show up on Lobby as up for a challenge)
                playerDoc.update("status", "offline")
                    .addOnSuccessListener {
                        Log.d("AppLifecycleObserver", "Player status updated to offline.")
                    }
                    .addOnFailureListener { error ->
                        Log.e("AppLifecycleObserver", "Error updating status: ${error.message}")
                    }
            }

        }.addOnFailureListener { error ->
            Log.e("AppLifecycleObserver", "Error checking : ${error.message}")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val playerDoc = Firebase.firestore.collection("players").document(playerId)

        playerDoc.get().addOnSuccessListener { document ->
            if (document.exists()) {

                playerDoc.update("status", "lobby")
                    .addOnSuccessListener {
                        Log.d("AppLifecycleObserver", "Player status updated to lobby.")
                    }
                    .addOnFailureListener { error ->
                        Log.e("AppLifecycleObserver", "Error updating status: ${error.message}")
                    }

            }
        }.addOnFailureListener { error ->
            Log.e("AppLifecycleObserver", "Error checking : ${error.message}")
        }
    }
}