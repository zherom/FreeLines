package com.example.freelines.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.freelines.data.GameRepository
import com.example.freelines.data.HighScore
import com.example.freelines.data.Settings
import com.example.freelines.game.Ball
import com.example.freelines.game.Board
import com.example.freelines.game.BoardData
import com.example.freelines.game.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


data class GameState(val boardData: BoardData, val score: Int, val elapsedTime: Long)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)
    private lateinit var settings: Settings
    private val availableColors = (0..10).toList()

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
    private val _isGameWon = MutableLiveData(false)
    val isGameWon: LiveData<Boolean> = _isGameWon
    private val _canUndo = MutableLiveData(false)
    val canUndo: LiveData<Boolean> = _canUndo
    private val _canRedo = MutableLiveData(false)
    val canRedo: LiveData<Boolean> = _canRedo

    private var history = mutableListOf<GameState>()
    private var historyIndex = -1

    fun startGame(isNew: Boolean) {
        viewModelScope.launch {
            settings = repository.settings.first()
            if (isNew) {
                repository.clearSavedGame()
                newGame()
            } else {
                if (repository.hasSavedGame()) {
                    loadGame()
                } else {
                    newGame()
                }
            }
            startTimer()
        }
    }

    private suspend fun loadGame() {
        history = repository.gameStateHistory.first().toMutableList()
        historyIndex = repository.gameStateIndex.first()
        if (history.isNotEmpty() && historyIndex != -1) {
            updateUiFromState(history[historyIndex])
            updateUndoRedoState()
        }
    }

    fun newGame() {
        _isGameOver.value = false
        _isGameWon.value = false
        val newBoard = Board(settings.boardWidth, settings.boardHeight)
        spawnBalls(newBoard)
        history.clear()
        historyIndex = -1
        _elapsedTime.value = 0L
        saveState(newBoard, 0)
        startTimer()
    }

    fun onCellClicked(position: Position) {
        if (_isGameOver.value == true || _isGameWon.value == true) return

        val currentBoard = _board.value?.copy() ?: return
        val selectedPos = _selectedBallPosition.value

        if (selectedPos == null) {
            if (currentBoard.getBallAt(position) != null) {
                _selectedBallPosition.value = position
            }
        } else {
            if (currentBoard.getBallAt(position) != null) {
                _selectedBallPosition.value = position
            } else {
                if (currentBoard.hasPath(selectedPos, position)) {
                    currentBoard.moveBall(selectedPos, position)
                    _selectedBallPosition.value = null

                    var newScore = _score.value ?: 0
                    val linesAfterMove = currentBoard.findLinesAt(position, settings.lineSize)
                    
                    if (linesAfterMove.isNotEmpty()) {
                        // Line cleared: remove balls, add score, NO new balls (free turn)
                        currentBoard.removeBalls(linesAfterMove)
                        newScore += calculateScore(linesAfterMove.size)
                    } else {
                        // No line cleared: spawn new balls and check if THEY form lines
                        val newBallPositions = spawnBalls(currentBoard)
                        val linesAfterSpawn = newBallPositions.flatMap { newPos ->
                            currentBoard.findLinesAt(newPos, settings.lineSize)
                        }.toSet()

                        if (linesAfterSpawn.isNotEmpty()) {
                            currentBoard.removeBalls(linesAfterSpawn.toList())
                            newScore += calculateScore(linesAfterSpawn.size)
                        }
                    }
                    
                    if (currentBoard.isEmpty()) {
                        _isGameWon.value = true
                        timerJob?.cancel()
                    } else if (currentBoard.isFull()) {
                        _isGameOver.value = true
                        timerJob?.cancel()
                    }
                    
                    saveState(currentBoard, newScore)
                }
            }
        }
    }
    
    private fun spawnBalls(board: Board): List<Position> {
        val emptyCells = mutableListOf<Position>()
        for (row in 0 until board.height) {
            for (col in 0 until board.width) {
                val pos = Position(row, col)
                if (board.getBallAt(pos) == null) {
                    emptyCells.add(pos)
                }
            }
        }
        emptyCells.shuffle()
        val colorsToUse = availableColors.take(settings.colorCount)
        val spawnedPositions = mutableListOf<Position>()
        
        for (i in 0 until settings.spawnCount) {
            if (emptyCells.isNotEmpty() && colorsToUse.isNotEmpty()) {
                val pos = emptyCells.removeAt(0)
                val randomColorType = colorsToUse.random()
                board.placeBall(Ball(randomColorType), pos)
                spawnedPositions.add(pos)
            }
        }
        return spawnedPositions
    }

    private fun saveState(board: Board, score: Int) {
        val newGameState = GameState(board.toBoardData(), score, _elapsedTime.value ?: 0L)
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newGameState)
        historyIndex++
        updateUiFromState(newGameState)
        updateUndoRedoState()

        if (_isGameOver.value != true && _isGameWon.value != true) {
            repository.saveGame(history, historyIndex)
        } else {
            repository.clearSavedGame()
        }
    }
    
    fun saveHighScore(playerName: String) {
        repository.addHighScore(HighScore(playerName, _score.value ?: 0, _elapsedTime.value ?: 0))
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            updateUiFromState(history[historyIndex])
            updateUndoRedoState()
            repository.saveGame(history, historyIndex)
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            updateUiFromState(history[historyIndex])
            updateUndoRedoState()
            repository.saveGame(history, historyIndex)
        }
    }

    private fun updateUiFromState(gameState: GameState) {
        _board.value = Board(gameState.boardData)
        _score.value = gameState.score
        _elapsedTime.value = gameState.elapsedTime
    }

    private fun updateUndoRedoState() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    private fun calculateScore(ballsRemoved: Int): Int {
        return ballsRemoved * 2
    }

    private fun startTimer() {
        timerJob?.cancel()
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
