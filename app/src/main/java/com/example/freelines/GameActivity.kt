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
import androidx.core.content.ContextCompat
import com.example.freelines.game.BallColor
import com.example.freelines.game.Position
import com.example.freelines.viewmodel.GameViewModel

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()

    private lateinit var boardGrid: GridLayout
    private val cells = mutableListOf<ImageView>()
    private lateinit var scoreTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var undoButton: Button
    private lateinit var redoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val isNewGame = intent.getBooleanExtra("IS_NEW_GAME", true)
        viewModel.startGame(isNewGame)

        boardGrid = findViewById(R.id.grid_board)
        scoreTextView = findViewById(R.id.tv_score)
        timeTextView = findViewById(R.id.tv_time)
        undoButton = findViewById(R.id.btn_undo)
        redoButton = findViewById(R.id.btn_redo)
        val newGameButton: Button = findViewById(R.id.btn_new_game_ingame)
        val backButton: Button = findViewById(R.id.btn_back_to_menu)

        initializeBoardUI()
        setupObservers()
        setupClickListeners(newGameButton, backButton)
    }

    private fun initializeBoardUI() {
        for (i in 0 until 81) {
            val cell = ImageView(this)
            val params = GridLayout.LayoutParams(
                GridLayout.spec(i / 9, 1f),
                GridLayout.spec(i % 9, 1f)
            ).apply {
                width = 0
                height = 0
                setMargins(4, 4, 4, 4)
            }
            cell.layoutParams = params
            cell.setOnClickListener { 
                viewModel.onCellClicked(Position(i / 9, i % 9)) 
            }
            boardGrid.addView(cell)
            cells.add(cell)
        }
    }

    private fun setupObservers() {
        viewModel.board.observe(this) { board ->
            for (i in 0 until 81) {
                val pos = Position(i / 9, i % 9)
                val ball = board.getBallAt(pos)
                val drawableId = ball?.color?.toDrawable() ?: R.drawable.ic_cell_background
                cells[i].setImageResource(drawableId)
            }
        }

        viewModel.selectedBallPosition.observe(this) { position ->
            for (i in 0 until 81) {
                cells[i].background = null // Clear all backgrounds
            }
            if (position != null) {
                val index = position.row * 9 + position.col
                cells[index].setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
            }
        }

        viewModel.score.observe(this) { score ->
            scoreTextView.text = "Score: $score"
        }

        viewModel.elapsedTime.observe(this) { time ->
            timeTextView.text = "Time: ${formatTime(time)}"
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

        scoreText.text = "Your Score: ${viewModel.score.value}"
        timeText.text = "Time: ${formatTime(viewModel.elapsedTime.value ?: 0)}"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
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
}

fun BallColor.toDrawable(): Int {
    return when (this) {
        BallColor.RED -> R.drawable.ball_red
        BallColor.GREEN -> R.drawable.ball_green
        BallColor.BLUE -> R.drawable.ball_blue
        BallColor.YELLOW -> R.drawable.ball_yellow
        BallColor.PURPLE -> R.drawable.ball_purple
        BallColor.ORANGE -> R.drawable.ball_orange
    }
}
