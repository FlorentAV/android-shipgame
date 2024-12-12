package com.florent.shipgame.logic
import android.util.Log
import androidx.lifecycle.ViewModel
import com.florent.shipgame.models.Coordinate
import com.florent.shipgame.models.Game
import com.florent.shipgame.models.Player
import com.florent.shipgame.models.Ship
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
    fun acceptChallenge(challengeId: String, playerId: String, onGameCreated: (String?) -> Unit) {
        val challengeRef = db.collection("challenges").document(challengeId)
        challengeRef.get().addOnSuccessListener { document ->
            val senderId = document.getString("senderId") ?: run {
                Log.e("GameModel", "senderId missing in challenge document")
                return@addOnSuccessListener
            }

            val game = Game(
                player1Id = senderId,
                player2Id = playerId,
                gameState = "placing-ships",
                currentTurn = senderId
            )

            createGame(game) { gameId ->
                if (gameId != null) {
                    // Log the game creation
                    Log.d("GameModel", "Game created with ID: $gameId")

                    // Update players with currentGameId
                    db.collection("players").document(senderId).update("currentGameId", gameId)
                    db.collection("players").document(playerId).update("currentGameId", gameId)

                    // Transition game state and navigate
                    updateChallengeStatus(challengeId, "accepted") {
                        transitionGameState(gameId, "placing-ships") { success ->
                            if (success) {
                                onGameCreated(gameId)
                            } else {
                                Log.e("GameModel", "Failed to transition game state")
                                onGameCreated(null)
                            }
                        }
                    }
                } else {
                    Log.e("GameModel", "Failed to create game")
                    onGameCreated(null)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("GameModel", "Failed to retrieve challenge: ${e.message}")
        }
    }
    // Create Game
    fun createGame(game: Game, onComplete: (String?) -> Unit) {
        val gameRef = db.collection("games").document() // Generate a new document ID
        gameRef.set(game)
            .addOnSuccessListener { onComplete(gameRef.id) }
            .addOnFailureListener { e ->
                Log.e("GameModel", "Failed to create game: ${e.message}")
                onComplete(null)
            }
    }


    // Transition to the game state
    fun transitionGameState(gameId: String, newState: String, onComplete: (Boolean) -> Unit) {
        db.collection("games").document(gameId).update("gameState", newState)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Check if all ships are placed to navigate to gamescreen
    fun checkIfAllShipsPlaced(gameId: String, shipStatus: (Boolean) -> Unit) {
        db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val player1Ships = snapshot.get("player1Ships") as? List<*>
                val player2Ships = snapshot.get("player2Ships") as? List<*>
                if (player1Ships != null && player2Ships != null) {
                    shipStatus(player1Ships.isNotEmpty() && player2Ships.isNotEmpty())

                }
            }
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


    // Observer for player state/status
    fun observePlayerState(playerId: String, playerStatus: (String) -> Unit) {
        db.collection("players").document(playerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val status = snapshot.getString("status")
                if (status != null) playerStatus(status)
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


    // get the sender id
    fun fetchChallengeSender(challengeId: String, onComplete: (String?) -> Unit) {
        db.collection("challenges").document(challengeId)
            .get()
            .addOnSuccessListener { document ->
                val senderId = document.getString("senderId")
                onComplete(senderId)
            }
            .addOnFailureListener { e ->
                onComplete(null)
            }
    }


    // Observer for player game

    fun observePlayerGame(playerId: String, onGameFound: (String) -> Unit) {
        db.collection("players").document(playerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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



    // Save ships to db
    fun saveShips(gameId: String, playerId: String, ships: List<Ship>, onComplete: (Boolean) -> Unit) {
        val field = if (playerId == gameMap.value[gameId]?.player1Id) "player1Ships" else "player2Ships"
        db.collection("games").document(gameId)
            .update(field, ships.map { ship ->
                mapOf(
                    "size" to ship.size,
                    "positions" to ship.positions.map { pos -> mapOf("x" to pos.x, "y" to pos.y) }
                )
            })
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }



    // Your turn
    fun takeTurn(
        gameId: String,
        playerId: String,
        target: Coordinate,
        onComplete: (String) -> Unit
    ) {
        val game = gameMap.value[gameId] ?: return onComplete("Game not found.")

        // Get opponents ships and hits/misses based on the current player
        val opponentShips = if (playerId == game.player1Id) game.player2Ships else game.player1Ships
        val playerHits = if (playerId == game.player1Id) game.player1Hits.toMutableList() else game.player2Hits.toMutableList()
        val playerMisses = if (playerId == game.player1Id) game.player1Misses.toMutableList() else game.player2Misses.toMutableList()

        if (playerHits.contains(target) || playerMisses.contains(target)) {
            onComplete("Position already targeted!")
            return
        }

        val isHit = opponentShips.any { ship -> ship.positions.contains(target) }
        if (isHit) {
            playerHits.add(target)


            updateLocalGameState(gameId, playerId, playerHits, isHit = true)

            // Update hits to db and check wincon
            val updates = if (playerId == game.player1Id) {
                mapOf("player1Hits" to playerHits.map { mapOf("x" to it.x, "y" to it.y) })
            } else {
                mapOf("player2Hits" to playerHits.map { mapOf("x" to it.x, "y" to it.y) })
            }

            db.collection("games").document(gameId).update(updates).addOnCompleteListener {
                if (checkWinningCondition(opponentShips, playerHits)) {
                    val winnerMessage = "${playerId} won!"
                    db.collection("games").document(gameId).update("gameState", "${playerId}_won")
                    updateLocalGameState(gameId, playerId, playerHits, isHit = true)
                    onComplete(winnerMessage)
                } else {
                    onComplete("Hit!")
                }
            }
        } else {
            playerMisses.add(target)

            // Update local state so you can see updated hits live
            updateLocalGameState(gameId, playerId, playerMisses, isHit = false)

            // Update misses to db
            val updates = if (playerId == game.player1Id) {
                mapOf("player1Misses" to playerMisses.map { mapOf("x" to it.x, "y" to it.y) })
            } else {
                mapOf("player2Misses" to playerMisses.map { mapOf("x" to it.x, "y" to it.y) })
            }

            db.collection("games").document(gameId).update(updates).addOnCompleteListener {
                switchTurn(gameId) { success ->
                    if (success) onComplete("Miss!") else onComplete("Error switching turn.")
                }
            }
        }
    }


    // update local gamestate when player makes its hit on a ship
    private fun updateLocalGameState(
        gameId: String,
        playerId: String,
        updatedList: List<Coordinate>,
        isHit: Boolean
    ) {
        val game = gameMap.value[gameId] ?: return
        val updatedGame = if (isHit) {
            if (playerId == game.player1Id) {
                game.copy(player1Hits = updatedList.toMutableList())
            } else {
                game.copy(player2Hits = updatedList.toMutableList())
            }
        } else {
            if (playerId == game.player1Id) {
                game.copy(player1Misses = updatedList.toMutableList())
            } else {
                game.copy(player2Misses = updatedList.toMutableList())
            }
        }


        gameMap.value = gameMap.value.toMutableMap().apply { put(gameId, updatedGame) }
    }

    // Win condition
    private fun checkWinningCondition(opponentShips: List<Ship>, opponentHits: List<Coordinate>): Boolean {
        return opponentShips.all { ship ->
            ship.positions.all { pos -> opponentHits.contains(pos) }
        }
    }




    // Switch turn
    fun switchTurn(gameId: String, onComplete: (Boolean) -> Unit) {
        val game = gameMap.value[gameId] ?: return
        val newTurn = if (game.currentTurn == game.player1Id) game.player2Id else game.player1Id
        db.collection("games").document(gameId)
            .update("currentTurn", newTurn)
            .addOnSuccessListener {
                Log.d("GameModel", "Turn switched to $newTurn")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("GameModel", "Failed to switch turn: ${e.message}")
                onComplete(false)
            }
    }


}