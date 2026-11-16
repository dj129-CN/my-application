package com.example.noteflow.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.noteflow.R
import com.example.noteflow.databinding.ActivityTodoWidgetConfigBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Configuration screen shown when the user adds the 4x1 todo widget. Lets them
 * pick which category to display (all / completed / incomplete) and applies the
 * choice immediately before returning to the launcher.
 */
class TodoWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodoWidgetConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val configScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        val currentCategory = TodoWidgetPrefs.getCategory(this, appWidgetId)
        when (currentCategory) {
            TodoWidgetPrefs.CATEGORY_COMPLETED -> binding.radioCompleted.isChecked = true
            TodoWidgetPrefs.CATEGORY_INCOMPLETE -> binding.radioIncomplete.isChecked = true
            else -> binding.radioAll.isChecked = true
        }

        binding.buttonSave.setOnClickListener {
            val selectedCategory = when (binding.radioGroup.checkedRadioButtonId) {
                R.id.radioCompleted -> TodoWidgetPrefs.CATEGORY_COMPLETED
                R.id.radioIncomplete -> TodoWidgetPrefs.CATEGORY_INCOMPLETE
                else -> TodoWidgetPrefs.CATEGORY_ALL
            }
            saveAndFinish(selectedCategory)
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveAndFinish(category: String) {
        TodoWidgetPrefs.saveCategory(this, appWidgetId, category)
        configScope.launch {
            TodoWidgetUpdater.updateWidget(applicationContext, appWidgetId)
        }
        val resultIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
