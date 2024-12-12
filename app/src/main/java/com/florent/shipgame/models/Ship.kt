package com.florent.shipgame.models




data class Ship(
    val size: Int = 0,
    val positions: MutableList<Coordinate> = mutableListOf(),
    var isSunk: Boolean = false
)