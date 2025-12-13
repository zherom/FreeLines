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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

data class HighScore(val playerName: String, val score: Int, val time: Long)
data class Settings(
    val language: String,
    val boardWidth: Int,
    val boardHeight: Int,
    val colorCount: Int,
    val lineSize: Int,
    val spawnCount: Int,
    val unproportionalStretch: Boolean
)

class GameRepository(private val context: Context) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private object PreferencesKeys {
        val GAME_STATE_HISTORY = stringPreferencesKey("game_state_history")
        val GAME_STATE_INDEX = stringPreferencesKey("game_state_index")
        val HIGH_SCORES = stringPreferencesKey("high_scores")
        val SETTINGS = stringPreferencesKey("settings")
    }

    val settings: Flow<Settings> = context.dataStore.data.map {
        val json = it[PreferencesKeys.SETTINGS]
        if (json != null) {
            gson.fromJson(json, Settings::class.java)
        } else {
            // Default settings
            Settings("en", 9, 9, 6, 5, 3, false)
        }
    }

    // ... (rest of the file is correct)
    val gameStateHistory: Flow<List<GameState>> = context.dataStore.data.map { val json = it[PreferencesKeys.GAME_STATE_HISTORY]; if (json != null) { try { val type = object : TypeToken<List<GameState>>() {}.type; gson.fromJson(json, type) } catch (e: Exception) { emptyList() } } else { emptyList() } }
    val gameStateIndex: Flow<Int> = context.dataStore.data.map { it[PreferencesKeys.GAME_STATE_INDEX]?.toInt() ?: -1 }
    val highScores: Flow<List<HighScore>> = context.dataStore.data.map { val json = it[PreferencesKeys.HIGH_SCORES]; if (json != null) { val type = object : TypeToken<List<HighScore>>() {}.type; gson.fromJson(json, type) } else { emptyList() } }
    fun saveGame(history: List<GameState>, index: Int) { repositoryScope.launch { val json = gson.toJson(history); context.dataStore.edit { pref -> pref[PreferencesKeys.GAME_STATE_HISTORY] = json; pref[PreferencesKeys.GAME_STATE_INDEX] = index.toString() } } }
    fun addHighScore(highScore: HighScore) { repositoryScope.launch { val currentScores = highScores.first().toMutableList(); currentScores.add(highScore); currentScores.sortByDescending { s -> s.score }; val json = gson.toJson(currentScores); context.dataStore.edit { pref -> pref[PreferencesKeys.HIGH_SCORES] = json } } }
    fun saveSettings(settings: Settings) { repositoryScope.launch { val json = gson.toJson(settings); context.dataStore.edit { pref -> pref[PreferencesKeys.SETTINGS] = json } } }
    suspend fun hasSavedGame(): Boolean { val preferences = context.dataStore.data.first(); return preferences.contains(PreferencesKeys.GAME_STATE_HISTORY) }
    fun clearSavedGame() { repositoryScope.launch { context.dataStore.edit { pref -> pref.remove(PreferencesKeys.GAME_STATE_HISTORY); pref.remove(PreferencesKeys.GAME_STATE_INDEX) } } }
}
