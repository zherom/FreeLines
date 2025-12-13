package com.example.freelines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.freelines.data.GameRepository
import com.example.freelines.data.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var repository: GameRepository
    private lateinit var languageSpinner: Spinner
    private lateinit var widthEditText: EditText
    private lateinit var heightEditText: EditText
    private lateinit var colorsEditText: EditText
    private lateinit var lineSizeEditText: EditText
    private lateinit var spawnCountEditText: EditText
    private lateinit var stretchCheckBox: CheckBox
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button

    private var currentSettings: Settings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        repository = GameRepository(application)
        languageSpinner = findViewById(R.id.spinner_language)
        widthEditText = findViewById(R.id.et_board_width)
        heightEditText = findViewById(R.id.et_board_height)
        colorsEditText = findViewById(R.id.et_color_count)
        lineSizeEditText = findViewById(R.id.et_line_size)
        spawnCountEditText = findViewById(R.id.et_spawn_count)
        stretchCheckBox = findViewById(R.id.cb_unproportional_stretch)
        saveButton = findViewById(R.id.btn_save_settings)
        resetButton = findViewById(R.id.btn_reset_settings)

        setupSpinner()

        lifecycleScope.launch {
            loadAndApplySettings()
        }

        saveButton.setOnClickListener { saveSettings() }
        resetButton.setOnClickListener { resetSettings() }
    }

    private fun setupSpinner() {
        val languages = arrayOf("English", "Deutsch", "Русский")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        languageSpinner.adapter = adapter
    }

    private suspend fun loadAndApplySettings() {
        currentSettings = repository.settings.first()
        runOnUiThread {
            widthEditText.setText(currentSettings?.boardWidth.toString())
            heightEditText.setText(currentSettings?.boardHeight.toString())
            colorsEditText.setText(currentSettings?.colorCount.toString())
            lineSizeEditText.setText(currentSettings?.lineSize.toString())
            spawnCountEditText.setText(currentSettings?.spawnCount.toString())
            stretchCheckBox.isChecked = currentSettings?.unproportionalStretch ?: false

            val langIndex = when (currentSettings?.language) {
                "de" -> 1
                "ru" -> 2
                else -> 0
            }
            languageSpinner.setSelection(langIndex)
        }
    }

    private fun saveSettings() {
        val width = widthEditText.text.toString().toIntOrNull() ?: 1
        val height = heightEditText.text.toString().toIntOrNull() ?: 1
        val colors = colorsEditText.text.toString().toIntOrNull() ?: 1
        val lineSize = lineSizeEditText.text.toString().toIntOrNull() ?: 1
        val spawnCount = spawnCountEditText.text.toString().toIntOrNull() ?: 1

        if (width <= 1 || height <= 1 || colors <= 1 || lineSize <= 1 || spawnCount < 1) {
            Toast.makeText(this, "Values must be greater than 1 (or 1 for spawn count)", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedLangCode = when (languageSpinner.selectedItemPosition) {
            1 -> "de"
            2 -> "ru"
            else -> "en"
        }

        val newSettings = Settings(selectedLangCode, width, height, colors, lineSize, spawnCount, stretchCheckBox.isChecked)
        repository.saveSettings(newSettings)

        if (currentSettings?.language != selectedLangCode) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(selectedLangCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        } else {
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun resetSettings() {
        val defaultSettings = Settings("en", 9, 9, 6, 5, 3, false)
        repository.saveSettings(defaultSettings)

        widthEditText.setText(defaultSettings.boardWidth.toString())
        heightEditText.setText(defaultSettings.boardHeight.toString())
        colorsEditText.setText(defaultSettings.colorCount.toString())
        lineSizeEditText.setText(defaultSettings.lineSize.toString())
        spawnCountEditText.setText(defaultSettings.spawnCount.toString())
        stretchCheckBox.isChecked = defaultSettings.unproportionalStretch
        languageSpinner.setSelection(0)
        
        Toast.makeText(this, "Settings reset to default!", Toast.LENGTH_SHORT).show()

        if (currentSettings?.language != defaultSettings.language) {
             val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(defaultSettings.language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}
