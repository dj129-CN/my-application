package com.example.noteflow.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.noteflow.R
import com.example.noteflow.databinding.ActivityMainBinding
import com.example.noteflow.util.NoteDeepLinks

/**
 * Single-activity host and the single entry point for URI deep links.
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
        if (!NoteDeepLinks.SCHEME.equals(data.scheme, ignoreCase = true)) return

        // Let the NavHost create its graph before applying an external deep link.
        binding.root.post {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                ?: return@post
            navHost.navController.handleDeepLink(intent)
        }
    }
}
