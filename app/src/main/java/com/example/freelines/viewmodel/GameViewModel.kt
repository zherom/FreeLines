package com.example.freelines.viewmodel

import androidx.lifecycle.ViewModel
import com.example.freelines.game.Ball
import com.example.freelines.game.BallColor
import com.example.freelines.game.Board
import com.example.freelines.game.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _board = MutableStateFlow(Board())
    val board: StateFlow<Board> = _board.asStateFlow()

    private val _selectedBallPosition = MutableStateFlow<Position?>(null)
    val selectedBallPosition: StateFlow<Position?> = _selectedBallPosition.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    init {
        spawnBalls(3)
    }

    fun onCellClicked(position: Position) {
        val currentBoard = _board.value
        val ballAtTarget = currentBoard.getBallAt(position)
        val selectedPos = _selectedBallPosition.value

        if (selectedPos == null) {
            if (ballAtTarget != null) {
                _selectedBallPosition.value = position
            }
        } else {
            if (ballAtTarget != null) {
                _selectedBallPosition.value = position
            } else {
                if (currentBoard.hasPath(selectedPos, position)) {
                    currentBoard.moveBall(selectedPos, position)
                    _selectedBallPosition.value = null

                    if (!checkAndRemoveLines()) {
                        spawnBalls(3)
                        checkAndRemoveLines()
                    }
                    _board.update { currentBoard } // Trigger UI update
                }
            }
        }
    }

    private fun checkAndRemoveLines(): Boolean {
        val currentBoard = _board.value
        val lines = currentBoard.findLines()
        if (lines.isNotEmpty()) {
            currentBoard.removeBalls(lines)
            _score.value += calculateScore(lines.size)
            return true
        }
        return false
    }

    private fun calculateScore(ballsRemoved: Int): Int {
        return ballsRemoved * 2
    }

    private fun spawnBalls(count: Int) {
        val currentBoard = _board.value
        val emptyCells = mutableListOf<Position>()
        for (row in 0 until currentBoard.size) {
            for (col in 0 until currentBoard.size) {
                val position = Position(row, col)
                if (currentBoard.getBallAt(position) == null) {
                    emptyCells.add(position)
                }
            }
        }

        emptyCells.shuffle()

        for (i in 0 until count) {
            if (emptyCells.isNotEmpty()) {
                val position = emptyCells.removeAt(0)
                val randomColor = BallColor.values().random()
                currentBoard.placeBall(Ball(randomColor), position)
            }
        }
    }
}
