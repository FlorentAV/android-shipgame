package com.florent.shipgame.logic
import android.util.Log
import androidx.lifecycle.ViewModel
import com.florent.shipgame.models.Game
import com.florent.shipgame.models.Player
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow


// Game rules and misc.
class GameModel : ViewModel() {

    val db = Firebase.firestore

    // State flows for player and game
    val localPlayerId = MutableStateFlow<String?>(null)
    val playerMap = MutableStateFlow<Map<String, Player>>(emptyMap())
    val gameMap = MutableStateFlow<Map<String, Game>>(emptyMap())

    init {
        listenToPlayers()
        listenToGames()
    }

    // Listen for player changes
    private fun listenToPlayers() {
        db.collection("players")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val updatedMap = value?.documents?.associate { doc ->
                    doc.id to doc.toObject(Player::class.java)!!
                } ?: emptyMap()
                playerMap.value = updatedMap
            }
    }

    // Listen for game changes
    private fun listenToGames() {
        db.collection("games")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val updatedMap = value?.documents?.associate { doc ->
                    doc.id to doc.toObject(Game::class.java)!!
                } ?: emptyMap()
                gameMap.value = updatedMap
            }
    }

    // Accept the challenge and create game
    fun acceptChallenge(
        challengeId: String,
        playerId: String,
        onGameCreated: (String?) -> Unit
    ) {
        val challengeRef = db.collection("challenges").document(challengeId)
        challengeRef.get().addOnSuccessListener { document ->
            val senderId = document.getString("senderId") ?: return@addOnSuccessListener
            val game = Game(
                player1Id = senderId,
                player2Id = playerId,
                gameState = "placing-ships",
                currentTurn = senderId
            )
            createGame(game) { gameId ->
                if (gameId != null) {
                    // Update both players with the gameId
                    db.collection("players").document(senderId).update("currentGameId", gameId)
                    db.collection("players").document(playerId).update("currentGameId", gameId)

                    // Update the challenge status
                    updateChallengeStatus(challengeId, "accepted") {
                        transitionGameState(gameId, "placing-ships") { success ->
                            if (success) onGameCreated(gameId)
                        }
                    }
                } else {
                    onGameCreated(null)
                }
            }
        }
    }

    // Transition to the game state
    fun transitionGameState(gameId: String, newState: String, onComplete: (Boolean) -> Unit) {
        db.collection("games").document(gameId).update("gameState", newState)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Observe Game State
    fun observeGameState(gameId: String, onGameStateChange: (String) -> Unit) {
        db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val gameState = snapshot.getString("gameState")
                if (gameState != null) onGameStateChange(gameState)
            }
    }

    // Create player
    fun createPlayer(player: Player, onComplete: (Boolean) -> Unit) {
        db.collection("players")
            .document(player.playerId)
            .set(player)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // update players status
    fun updatePlayerStatus(playerId: String, status: String, onComplete: (Boolean) -> Unit) {
        db.collection("players").document(playerId)
            .update("status", status)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }


    // Send challenge
    fun sendChallenge(senderId: String, receiverId: String, onComplete: (Boolean) -> Unit) {
        val challenge = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "status" to "pending"
        )
        db.collection("challenges")
            .add(challenge)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Listen for challenge
    fun listenForChallenges(playerId: String, onChallengeReceived: (String, String) -> Unit) {
        db.collection("challenges")
            .whereEqualTo("receiverId", playerId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameModel", "Error listening for challenges: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach { document ->
                    val senderId = document.getString("senderId") ?: return@forEach
                    val challengeId = document.id
                    onChallengeReceived(senderId, challengeId)
                }
            }
    }


    // Observer for player game

    fun observePlayerGame(playerId: String, onGameFound: (String) -> Unit) {
        db.collection("players").document(playerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameModel", "Error observing player game: ${error.message}")
                    return@addSnapshotListener
                }
                val gameId = snapshot?.getString("currentGameId")
                if (gameId != null) {
                    onGameFound(gameId)
                }
            }
    }

    // Update challenge status
    fun updateChallengeStatus(challengeId: String, status: String, onComplete: (Boolean) -> Unit) {
        db.collection("challenges").document(challengeId)
            .update("status", status)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Create Game
    private fun createGame(game: Game, onComplete: (String?) -> Unit) {
        val gameRef = db.collection("games").document() // Generate new document ID
        gameRef.set(game)
            .addOnSuccessListener { onComplete(gameRef.id) }
            .addOnFailureListener { onComplete(null) }
    }



    // Your turn
    fun takeTurn(
        gameId: String,
        playerId: String,
        target: Pair<Int, Int>,
        onComplete: (String) -> Unit
    ) {
        val game = gameMap.value[gameId] ?: return onComplete("Game not found.")
        val opponentShips = if (playerId == game.player1Id) game.player2Ships else game.player1Ships
        val opponentHits = if (playerId == game.player1Id) game.player2Hits else game.player1Hits
        val opponentMisses = if (playerId == game.player1Id) game.player2Misses else game.player1Misses

        // Check if target already hit
        if (opponentHits.contains(target) || opponentMisses.contains(target)) {
            onComplete("Position already targeted!")
            return
        }

        // Check if the target is a hit or a miss
        if (opponentShips.any { ship -> ship.positions.contains(target) }) {
            opponentHits.add(target)

            // Check for winning condition
            if (checkWinningCondition(game)) {
                db.collection("games").document(gameId).update("gameState", "${playerId}_won")
                onComplete("You won!")
            } else {
                onComplete("Hit!")
            }
        } else {
            opponentMisses.add(target)
            switchTurn(gameId) { success ->
                if (success) onComplete("Miss!")
            }
        }

        val updates = if (playerId == game.player1Id) {
            mapOf("player2Hits" to opponentHits, "player2Misses" to opponentMisses)
        } else {
            mapOf("player1Hits" to opponentHits, "player1Misses" to opponentMisses)
        }
        db.collection("games").document(gameId).update(updates)
    }


    // Win condition
    private fun checkWinningCondition(game: Game): Boolean {
        val opponentShips = if (game.currentTurn == game.player1Id) game.player2Ships else game.player1Ships
        val opponentHits = if (game.currentTurn == game.player1Id) game.player2Hits else game.player1Hits
        return opponentShips.all { ship ->
            ship.positions.all { pos -> opponentHits.contains(pos) }
        }
    }


    // Switch turn
    fun switchTurn(gameId: String, onComplete: (Boolean) -> Unit) {
        val game = gameMap.value[gameId] ?: return
        val newTurn = if (game.currentTurn == game.player1Id) game.player2Id else game.player1Id
        db.collection("games").document(gameId).update("currentTurn", newTurn)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }


}