package com.florent.shipgame.models



data class Player(
    val id: String,
    val name: String,
    val status: String = "offline"
)

