package com.example.noteflow.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.noteflow.R
import com.example.noteflow.databinding.ActivityMainBinding
import com.example.noteflow.util.NoteDeepLinks
import com.example.noteflow.util.TodoDeepLinks

/**
 * Single-activity host and the single entry point for URI deep links.
 * Supports both the legacy noteflow://open scheme and the new todo://app scheme.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            handleDeepLink(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return

        val scheme = data.scheme ?: return
        val isNoteLink = NoteDeepLinks.SCHEME.equals(scheme, ignoreCase = true)
        val isTodoLink = TodoDeepLinks.SCHEME.equals(scheme, ignoreCase = true)
        if (!isNoteLink && !isTodoLink) return

        // Rewrite external ?id= to internal ?todoId= for the detail route.
        val resolvedIntent = if (isTodoLink && data.path == "/detail" &&
            data.getQueryParameter("todoId") == null &&
            data.getQueryParameter("id") != null
        ) {
            val rewritten = data.buildUpon()
                .clearQuery()
                .appendQueryParameter("todoId", data.getQueryParameter("id")!!)
                .build()
            Intent(intent).apply { this.data = rewritten }
        } else {
            intent
        }

        // Let the NavHost create its graph before applying an external deep link.
        binding.root.post {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                ?: return@post
            navHost.navController.handleDeepLink(resolvedIntent)
        }
    }
}
