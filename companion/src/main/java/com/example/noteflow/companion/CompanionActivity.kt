package com.example.noteflow.companion

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.noteflow.companion.data.CompanionProviderClient
import com.example.noteflow.companion.data.CompanionSnapshot
import com.example.noteflow.companion.databinding.ActivityCompanionBinding
import com.example.noteflow.companion.widget.CompanionWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standalone companion launcher: connection status, optional main-app entry and
 * a local widget refresh action.
 */
class CompanionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openMainButton.setOnClickListener {
            val opened = CompanionProviderClient.openMainApp(this)
            if (!opened) {
                Toast.makeText(
                    this,
                    R.string.companion_main_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        binding.refreshWidgetButton.setOnClickListener {
            CompanionWidgetUpdater.requestUpdate(this)
            Toast.makeText(
                this,
                R.string.companion_widget_refreshed,
                Toast.LENGTH_SHORT,
            ).show()
            loadSnapshot()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSnapshot()
    }

    private fun loadSnapshot() {
        binding.connectionTitle.setText(R.string.companion_status_checking)
        binding.connectionDetail.setText(R.string.companion_status_checking_detail)

        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                CompanionProviderClient.loadSnapshot(this@CompanionActivity)
            }
            render(snapshot)
        }
    }

    private fun render(snapshot: CompanionSnapshot) {
        val latest = snapshot.latestNote
        if (snapshot.connected) {
            binding.connectionTitle.setText(R.string.companion_status_connected)
            binding.connectionDetail.setText(R.string.companion_status_connected_detail)
            binding.latestLabel.setText(R.string.companion_latest_label)

            if (latest == null) {
                binding.latestTitle.setText(R.string.companion_no_notes_title)
                binding.latestContent.setText(R.string.companion_no_notes_detail)
            } else {
                binding.latestTitle.text = latest.title
                binding.latestContent.text = latest.content.ifBlank {
                    getString(R.string.companion_no_content)
                }
            }
        } else {
            binding.connectionTitle.setText(R.string.companion_status_local)
            binding.connectionDetail.setText(R.string.companion_status_local_detail)
            binding.latestLabel.setText(R.string.companion_local_tip_label)
            binding.latestTitle.setText(R.string.companion_local_tip_title)
            binding.latestContent.setText(R.string.companion_local_tip_detail)
        }
    }
}
