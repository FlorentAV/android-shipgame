package com.florent.shipgame.models



data class Game(
    val player1Id: String = "",
    val player2Id: String = "",
    val player1Ships: List<Ship> = emptyList(),
    val player2Ships: List<Ship> = emptyList(),
    val player1Hits: MutableList<Coordinate> = mutableListOf(),
    val player2Hits: MutableList<Coordinate> = mutableListOf(),
    val player1Misses: MutableList<Coordinate> = mutableListOf(),
    val player2Misses: MutableList<Coordinate> = mutableListOf(),
    val gameState: String = "placing-ships",
    val currentTurn: String = ""
)