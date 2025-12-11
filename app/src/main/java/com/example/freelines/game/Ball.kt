package com.example.freelines.game

enum class BallColor {
    RED,
    GREEN,
    BLUE,
    YELLOW,
    PURPLE,
    ORANGE
}

data class Ball(val color: BallColor)
