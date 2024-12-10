package com.florent.shipgame.models


data class Ship(
    val size: Int,
    val positions: MutableList<Pair<Int, Int>> = mutableListOf()
)
