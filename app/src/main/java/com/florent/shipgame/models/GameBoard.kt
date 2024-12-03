package com.florent.shipgame.models

data class GameBoard(
    val size: Int = 10,
    val ships: MutableList<Ship> = mutableListOf(),
    val hits: MutableSet<Pair<Int, Int>> = mutableSetOf(),
    val misses: MutableSet<Pair<Int, Int>> = mutableSetOf()
)
