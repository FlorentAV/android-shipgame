package com.florent.shipgame.models

data class Ship(
    val size: Int,
    var orientation: Orientation = Orientation.HORIZONTAL,
    var positions: List<Pair<Int, Int>> = emptyList(),
    val hits: MutableSet<Pair<Int, Int>> = mutableSetOf()
) {
    enum class Orientation {
        HORIZONTAL, VERTICAL
    }


    fun isSunk(): Boolean {
        return hits.containsAll(positions)
    }
}