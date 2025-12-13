package com.example.freelines

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.freelines.data.GameRepository
import com.example.freelines.data.HighScore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScoreActivity : AppCompatActivity() {

    private lateinit var repository: GameRepository
    private lateinit var highscoreListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        repository = GameRepository(application)
        highscoreListView = findViewById(R.id.lv_highscores)
        val backButton: Button = findViewById(R.id.btn_back_from_score)

        backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            val scores = repository.highScores.first()
            val adapter = HighScoreAdapter(this@ScoreActivity, scores)
            highscoreListView.adapter = adapter
        }
    }

    private class HighScoreAdapter(context: Context, scores: List<HighScore>) :
        ArrayAdapter<HighScore>(context, 0, scores) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(context, R.layout.list_item_highscore, null)
            val item = getItem(position)!!

            val nameTextView = view.findViewById<TextView>(R.id.tv_player_name)
            val scoreTextView = view.findViewById<TextView>(R.id.tv_player_score)
            val timeTextView = view.findViewById<TextView>(R.id.tv_player_time)

            nameTextView.text = item.playerName
            scoreTextView.text = item.score.toString()
            timeTextView.text = formatTime(item.time)

            return view
        }

        private fun formatTime(seconds: Long): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return "%02d:%02d".format(minutes, remainingSeconds)
        }
    }
}
