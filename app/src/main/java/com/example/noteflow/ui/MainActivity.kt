package com.example.noteflow.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.noteflow.databinding.ActivityMainBinding

/**
 * Single-activity host. Navigation owns all screen-to-screen transitions.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
