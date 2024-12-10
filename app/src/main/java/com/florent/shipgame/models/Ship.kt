package com.florent.shipgame.models


data class Ship(
    val size: Int,
    var orientation: Orientation = Orientation.HORIZONTAL,
    var positions: MutableList<Pair<Int, Int>> = mutableListOf()
) {
    enum class Orientation {
        HORIZONTAL, VERTICAL
    }


    fun deepCopy(
        size: Int = this.size,
        orientation: Orientation = this.orientation,
        positions: MutableList<Pair<Int, Int>> = this.positions.toMutableList()
    ): Ship {
        return Ship(size, orientation, positions)
    }
}
