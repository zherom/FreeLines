package com.example.freelines

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.freelines.data.GameRepository
import com.example.freelines.game.Position
import com.example.freelines.viewmodel.GameViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()
    private lateinit var repository: GameRepository

    private lateinit var boardGrid: GridLayout
    private val cells = mutableListOf<ImageView>()
    private lateinit var scoreTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var undoButton: Button
    private lateinit var redoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        repository = GameRepository(application)
        val isNewGame = intent.getBooleanExtra("IS_NEW_GAME", true)
        viewModel.startGame(isNewGame)

        boardGrid = findViewById(R.id.grid_board)
        scoreTextView = findViewById(R.id.tv_score)
        timeTextView = findViewById(R.id.tv_time)
        undoButton = findViewById(R.id.btn_undo)
        redoButton = findViewById(R.id.btn_redo)
        val newGameButton: Button = findViewById(R.id.btn_new_game_ingame)
        val backButton: Button = findViewById(R.id.btn_back_to_menu)

        runBlocking {
            val settings = repository.settings.first()
            if (settings.unproportionalStretch) {
                val params = boardGrid.layoutParams as ConstraintLayout.LayoutParams
                params.dimensionRatio = null
                boardGrid.requestLayout()
            } else {
                 val ratio = if (settings.boardWidth > 0 && settings.boardHeight > 0) "${settings.boardHeight}:${settings.boardWidth}" else "1:1"
                 val params = boardGrid.layoutParams as ConstraintLayout.LayoutParams
                 params.dimensionRatio = ratio
                 boardGrid.requestLayout()
            }
        }

        setupObservers()
        setupClickListeners(newGameButton, backButton)
    }

    private fun initializeBoardUI(width: Int, height: Int) {
        boardGrid.removeAllViews()
        cells.clear()
        boardGrid.columnCount = width
        boardGrid.rowCount = height

        for (i in 0 until width * height) {
            val cell = ImageView(this)
            val params = GridLayout.LayoutParams(
                GridLayout.spec(i / width, 1f),
                GridLayout.spec(i % width, 1f)
            ).apply {
                this.width = 0
                this.height = 0
                setMargins(2, 2, 2, 2)
            }
            cell.layoutParams = params
            cell.setOnClickListener { viewModel.onCellClicked(Position(i / width, i % width)) }
            boardGrid.addView(cell)
            cells.add(cell)
        }
    }

    private fun setupObservers() {
        viewModel.board.observe(this) { board ->
            if (cells.isEmpty() || boardGrid.columnCount != board.width || boardGrid.rowCount != board.height) {
                initializeBoardUI(board.width, board.height)
            }
            
            for (row in 0 until board.height) {
                for (col in 0 until board.width) {
                    val index = row * board.width + col
                    val pos = Position(row, col)
                    val ball = board.getBallAt(pos)
                    val drawableId = ball?.colorType?.toDrawable() ?: R.drawable.ic_cell_background
                    if(index < cells.size) cells[index].setImageResource(drawableId)
                }
            }
        }

        viewModel.selectedBallPosition.observe(this) { position ->
            val board = viewModel.board.value ?: return@observe
            for (i in 0 until cells.size) {
                cells[i].background = null
            }
            if (position != null) {
                val index = position.row * board.width + position.col
                if(index < cells.size) {
                    cells[index].setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                }
            }
        }

        viewModel.score.observe(this) { score ->
            scoreTextView.text = getString(R.string.score_label, score)
        }

        viewModel.elapsedTime.observe(this) { time ->
            timeTextView.text = getString(R.string.time_label, formatTime(time))
        }

        viewModel.canUndo.observe(this) { undoButton.isEnabled = it }
        viewModel.canRedo.observe(this) { redoButton.isEnabled = it }
        
        viewModel.isGameOver.observe(this) { isOver ->
            if (isOver) {
                showGameOverDialog()
            }
        }
    }

    private fun showGameOverDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_over, null)
        val scoreText = dialogView.findViewById<TextView>(R.id.tv_final_score)
        val timeText = dialogView.findViewById<TextView>(R.id.tv_final_time)
        val nameEditText = dialogView.findViewById<EditText>(R.id.et_player_name)

        scoreText.text = getString(R.string.your_score_label, viewModel.score.value ?: 0)
        timeText.text = getString(R.string.time_label, formatTime(viewModel.elapsedTime.value ?: 0))

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val playerName = nameEditText.text.toString()
                if (playerName.isNotBlank()) {
                    viewModel.saveHighScore(playerName)
                    startActivity(Intent(this, ScoreActivity::class.java))
                    finish()
                }
            }
            .show()
    }

    private fun setupClickListeners(newGameButton: Button, backButton: Button) {
        undoButton.setOnClickListener { viewModel.undo() }
        redoButton.setOnClickListener { viewModel.redo() }
        newGameButton.setOnClickListener { viewModel.newGame() }
        backButton.setOnClickListener { finish() }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
    
    private fun Int.toDrawable(): Int {
        return when (this) {
            0 -> R.drawable.ball_red
            1 -> R.drawable.ball_green
            2 -> R.drawable.ball_blue
            3 -> R.drawable.ball_yellow
            4 -> R.drawable.ball_purple
            5 -> R.drawable.ball_orange
            else -> R.drawable.ic_cell_background
        }
    }
}
