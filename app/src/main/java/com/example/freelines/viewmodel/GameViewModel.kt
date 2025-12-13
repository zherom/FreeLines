package com.example.freelines.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.freelines.data.GameRepository
import com.example.freelines.data.HighScore
import com.example.freelines.game.Ball
import com.example.freelines.game.BallColor
import com.example.freelines.game.Board
import com.example.freelines.game.BoardData
import com.example.freelines.game.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// GameState now holds the simple, serializable BoardData
data class GameState(val boardData: BoardData, val score: Int)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    private val _board = MutableLiveData<Board>()
    val board: LiveData<Board> = _board

    private val _selectedBallPosition = MutableLiveData<Position?>()
    val selectedBallPosition: LiveData<Position?> = _selectedBallPosition

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _elapsedTime = MutableLiveData(0L)
    val elapsedTime: LiveData<Long> = _elapsedTime
    private var timerJob: Job? = null

    private val _isGameOver = MutableLiveData(false)
    val isGameOver: LiveData<Boolean> = _isGameOver

    private val _canUndo = MutableLiveData(false)
    val canUndo: LiveData<Boolean> = _canUndo

    private val _canRedo = MutableLiveData(false)
    val canRedo: LiveData<Boolean> = _canRedo

    private var history = mutableListOf<GameState>()
    private var historyIndex = -1
    private val isInitialized = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            if (repository.hasSavedGame()) {
                loadGame()
            } else {
                newGame()
            }
            isInitialized.set(true)
        }
    }

    private suspend fun loadGame() {
        history = repository.gameStateHistory.first().toMutableList()
        historyIndex = repository.gameStateIndex.first()
        updateUiFromState(history[historyIndex])
        startTimer()
    }

    fun newGame() {
        _isGameOver.value = false
        val newBoard = Board()
        spawnBalls(newBoard, 3)
        history.clear()
        historyIndex = -1
        saveState(newBoard, 0)
        startTimer()
    }

    fun onCellClicked(position: Position) {
        if (!isInitialized.get() || _isGameOver.value == true) return

        val currentBoard = _board.value?.copy() ?: return
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

                    val lines = currentBoard.findLines()
                    var newScore = _score.value ?: 0
                    if (lines.isNotEmpty()) {
                        currentBoard.removeBalls(lines)
                        newScore += calculateScore(lines.size)
                    } else {
                        spawnBalls(currentBoard, 3)
                        if (currentBoard.isFull()) {
                            _isGameOver.value = true
                            timerJob?.cancel()
                        }
                        val newLines = currentBoard.findLines()
                        if (newLines.isNotEmpty()) {
                            currentBoard.removeBalls(newLines)
                            newScore += calculateScore(newLines.size)
                        }
                    }
                    saveState(currentBoard, newScore)
                }
            }
        }
    }

    private fun saveState(board: Board, score: Int) {
        val newGameState = GameState(board.toBoardData(), score)
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newGameState)
        historyIndex++
        updateUiFromState(newGameState)
        updateUndoRedoState()

        viewModelScope.launch {
            if (_isGameOver.value != true) {
                repository.saveGame(history, historyIndex)
            } else {
                repository.clearSavedGame()
            }
        }
    }
    
    fun saveHighScore(playerName: String) {
        viewModelScope.launch {
            val newHighScore = HighScore(playerName, _score.value ?: 0, _elapsedTime.value ?: 0)
            repository.addHighScore(newHighScore)
        }
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            updateUiFromState(history[historyIndex])
            updateUndoRedoState()
            viewModelScope.launch { repository.saveGame(history, historyIndex) }
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            updateUiFromState(history[historyIndex])
            updateUndoRedoState()
            viewModelScope.launch { repository.saveGame(history, historyIndex) }
        }
    }

    private fun updateUiFromState(gameState: GameState) {
        _board.value = Board(gameState.boardData)
        _score.value = gameState.score
    }

    private fun updateUndoRedoState() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    private fun spawnBalls(board: Board, count: Int) {
        val emptyCells = mutableListOf<Position>()
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                val position = Position(row, col)
                if (board.getBallAt(position) == null) {
                    emptyCells.add(position)
                }
            }
        }
        emptyCells.shuffle()
        for (i in 0 until count) {
            if (emptyCells.isNotEmpty()) {
                val position = emptyCells.removeAt(0)
                val randomColor = BallColor.values().random()
                board.placeBall(Ball(randomColor), position)
            }
        }
    }

    private fun calculateScore(ballsRemoved: Int): Int {
        return ballsRemoved * 2
    }

    private fun startTimer() {
        timerJob?.cancel()
        // Time is part of the loaded state now, so don't reset to 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedTime.postValue((_elapsedTime.value ?: 0) + 1)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
