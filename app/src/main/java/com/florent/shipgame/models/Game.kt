package com.florent.shipgame.models



data class Game(
    val player1Id: String = "",
    val player2Id: String = "",
    val player1Ships: MutableList<Ship> = mutableListOf(),
    val player2Ships: MutableList<Ship> = mutableListOf(),
    val player1Hits: MutableList<Pair<Int, Int>> = mutableListOf(),
    val player2Hits: MutableList<Pair<Int, Int>> = mutableListOf(),
    val player1Misses: MutableList<Pair<Int, Int>> = mutableListOf(),
    val player2Misses: MutableList<Pair<Int, Int>> = mutableListOf(),
    val gameState: String = "placing",
    val currentTurn: String = ""
)