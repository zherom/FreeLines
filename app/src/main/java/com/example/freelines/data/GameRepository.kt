package com.example.freelines.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.freelines.viewmodel.GameState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

data class HighScore(val playerName: String, val score: Int, val time: Long)

class GameRepository(private val context: Context) {

    private val gson = Gson()

    private object PreferencesKeys {
        val GAME_STATE_HISTORY = stringPreferencesKey("game_state_history")
        val GAME_STATE_INDEX = stringPreferencesKey("game_state_index")
        val HIGH_SCORES = stringPreferencesKey("high_scores")
    }

    val gameStateHistory: Flow<List<GameState>> = context.dataStore.data.map {
        val json = it[PreferencesKeys.GAME_STATE_HISTORY]
        if (json != null) {
            try {
                val type = object : TypeToken<List<GameState>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val gameStateIndex: Flow<Int> = context.dataStore.data.map {
        it[PreferencesKeys.GAME_STATE_INDEX]?.toInt() ?: -1
    }

    val highScores: Flow<List<HighScore>> = context.dataStore.data.map {
        val json = it[PreferencesKeys.HIGH_SCORES]
        if (json != null) {
            val type = object : TypeToken<List<HighScore>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    suspend fun saveGame(history: List<GameState>, index: Int) {
        val json = gson.toJson(history)
        context.dataStore.edit {
            it[PreferencesKeys.GAME_STATE_HISTORY] = json
            it[PreferencesKeys.GAME_STATE_INDEX] = index.toString()
        }
    }

    suspend fun addHighScore(highScore: HighScore) {
        val currentScores = highScores.first().toMutableList()
        currentScores.add(highScore)
        currentScores.sortByDescending { it.score }
        val json = gson.toJson(currentScores)
        context.dataStore.edit {
            it[PreferencesKeys.HIGH_SCORES] = json
        }
    }

    suspend fun hasSavedGame(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences.contains(PreferencesKeys.GAME_STATE_HISTORY)
    }

    suspend fun clearSavedGame() {
        context.dataStore.edit {
            it.remove(PreferencesKeys.GAME_STATE_HISTORY)
            it.remove(PreferencesKeys.GAME_STATE_INDEX)
        }
    }
}
