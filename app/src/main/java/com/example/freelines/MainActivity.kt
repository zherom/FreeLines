package com.example.freelines

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.freelines.data.GameRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var repository: GameRepository
    private lateinit var resumeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = GameRepository(application)

        val newGameButton: Button = findViewById(R.id.btn_new_game)
        resumeButton = findViewById(R.id.btn_resume)
        val settingsButton: Button = findViewById(R.id.btn_settings)
        val scoreButton: Button = findViewById(R.id.btn_score)
        val exitButton: Button = findViewById(R.id.btn_exit)

        newGameButton.setOnClickListener {
            lifecycleScope.launch {
                repository.clearSavedGame() 
                startActivity(Intent(this@MainActivity, GameActivity::class.java))
            }
        }

        resumeButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        settingsButton.setOnClickListener {
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        scoreButton.setOnClickListener {
            Toast.makeText(this, "Score coming soon!", Toast.LENGTH_SHORT).show()
        }

        exitButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            resumeButton.isEnabled = repository.hasSavedGame()
        }
    }
}
