package com.florent.shipgame.logic
import com.florent.shipgame.models.Player
import com.florent.shipgame.models.GameBoard
import com.florent.shipgame.models.Ship
import kotlin.math.abs


class GameManager {
    lateinit var player1: Player
    lateinit var player2: Player
    private var currentPlayer: Player? = null

    fun startGame(player1Name: String, player2Name: String) {
        player1 = Player(player1Name, GameBoard())
        player2 = Player(player2Name, GameBoard())
        currentPlayer = player1
    }


    fun placeShip(player: Player, ship: Ship, startX: Int, startY: Int): Boolean {
        val board = player.board
        val positions = calculatePositions(ship, startX, startY)
        if (canPlaceShip(board, positions)) {
            ship.positions = positions
            board.ships.add(ship)
            return true
        }
        return false
    }

    private fun calculatePositions(ship: Ship, startX: Int, startY: Int): List<Pair<Int, Int>> {
        return (0 until ship.size).map { offset ->
            if (ship.orientation == Ship.Orientation.HORIZONTAL) {
                Pair(startX + offset, startY)
            } else {
                Pair(startX, startY + offset)
            }
        }
    }

    private fun canPlaceShip(board: GameBoard, positions: List<Pair<Int, Int>>): Boolean {
        return positions.all { (x, y) ->
            x in 0 until board.size && y in 0 until board.size &&
                    board.ships.none { ship ->
                        ship.positions.any { it == Pair(x, y) || isAdjacent(it, Pair(x, y)) }
                    }
        }
    }

    private fun isAdjacent(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Boolean {
        val (x1, y1) = pos1
        val (x2, y2) = pos2
        return abs(x1 - x2) <= 1 && abs(y1 - y2) <= 1
    }

    private fun processAttack(opponent: Player, x: Int, y: Int): String {
        val board = opponent.board
        if (board.misses.contains(Pair(x, y)) || board.ships.any { it.hits.contains(Pair(x, y)) }) {
            return "You already fired at this position!"
        }
        for (ship in board.ships) {
            if (ship.positions.contains(Pair(x, y))) {
                ship.hits.add(Pair(x, y))
                return if (ship.isSunk()) {
                    "Hit and sunk ${opponent.name}'s ship!"
                } else {
                    "Hit ${opponent.name}'s ship!"
                }
            }
        }
        board.misses.add(Pair(x, y))
        return "Miss!"
    }

    fun takeTurn(player: Player, targetX: Int, targetY: Int): String {
        val opponent = if (player == player1) player2 else player1
        val result = processAttack(opponent, targetX, targetY)

        // Check for a winner
        val winner = checkWinCondition()
        if (winner != null) {
            return "${winner.name} has won the game!"
        }

        // Switch turn
        currentPlayer = opponent
        return result
    }

    private fun checkWinCondition(): Player? {
        return when {
            player1.board.ships.all { it.isSunk() } -> player2
            player2.board.ships.all { it.isSunk() } -> player1
            else -> null
        }
    }

    fun displayBoard(player: Player) {
        val board = Array(player.board.size) { Array(player.board.size) { "." } }
        player.board.ships.forEach { ship ->
            ship.positions.forEach { (x, y) -> board[y][x] = "S" }
            ship.hits.forEach { (x, y) -> board[y][x] = "H" }
        }
        player.board.misses.forEach { (x, y) -> board[y][x] = "M" }
        board.forEach { println(it.joinToString(" ")) }
    }

    fun resetGame() {
        player1.board.ships.clear()
        player1.board.misses.clear()
        player2.board.ships.clear()
        player2.board.misses.clear()
        currentPlayer = null
    }

}


