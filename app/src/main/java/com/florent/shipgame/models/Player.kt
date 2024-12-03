package com.florent.shipgame.models



data class Player(
    val name: String,
    val board: GameBoard = GameBoard(),
    var isReady: Boolean = false
)