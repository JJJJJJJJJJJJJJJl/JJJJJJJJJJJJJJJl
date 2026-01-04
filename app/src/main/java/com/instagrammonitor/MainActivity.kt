package com.instagrammonitor

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var debugText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var permissionButton: Button
    private lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        debugText = findViewById(R.id.debugText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        permissionButton = findViewById(R.id.permissionButton)
        refreshButton = findViewById(R.id.refreshButton)

        updateUI()

        permissionButton.setOnClickListener {
            // Open usage access settings
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        startButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startMonitoringService()
                updateUI()
            } else {
                statusText.text = "Please grant Usage Access permission first!"
            }
        }

        stopButton.setOnClickListener {
            stopMonitoringService()
            updateUI()
        }

        refreshButton.setOnClickListener {
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isServiceRunning(): Boolean {
        val prefs = getSharedPreferences("InstagramMonitor", Context.MODE_PRIVATE)
        return prefs.getBoolean("service_running", false)
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, InstagramMonitorService::class.java)
        startForegroundService(serviceIntent)

        val prefs = getSharedPreferences("InstagramMonitor", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_running", true).apply()
    }

    private fun stopMonitoringService() {
        val serviceIntent = Intent(this, InstagramMonitorService::class.java)
        stopService(serviceIntent)

        val prefs = getSharedPreferences("InstagramMonitor", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_running", false).apply()
    }

    private fun updateUI() {
        val hasPermission = hasUsageStatsPermission()
        val serviceRunning = isServiceRunning()

        permissionButton.isEnabled = !hasPermission
        startButton.isEnabled = hasPermission && !serviceRunning
        stopButton.isEnabled = serviceRunning

        statusText.text = when {
            !hasPermission -> "❌ Usage Access permission required"
            serviceRunning -> "✓ Monitoring Instagram (5 min limit)"
            else -> "Ready to monitor"
        }

        // Show debug info
        val prefs = getSharedPreferences("InstagramMonitor", Context.MODE_PRIVATE)
        val lastForegroundApp = prefs.getString("last_foreground_app", "none")
        val isInstagramDetected = prefs.getBoolean("is_instagram_detected", false)
        val lastCheckTime = prefs.getLong("last_check_time", 0)
        val isTracking = prefs.getBoolean("is_tracking", false)
        val timeElapsed = prefs.getLong("time_elapsed", 0)

        val timeSinceCheck = if (lastCheckTime > 0) {
            (System.currentTimeMillis() - lastCheckTime) / 1000
        } else {
            0
        }

        val debugInfo = buildString {
            appendLine("DEBUG INFO:")
            appendLine("Last checked: ${timeSinceCheck}s ago")
            appendLine("Foreground app: $lastForegroundApp")
            appendLine("Instagram detected: $isInstagramDetected")
            appendLine("Currently tracking: $isTracking")
            if (isTracking && timeElapsed > 0) {
                val minutes = timeElapsed / 60000
                val seconds = (timeElapsed % 60000) / 1000
                appendLine("Time on Instagram: ${minutes}m ${seconds}s")
            }
        }

        debugText.text = debugInfo
    }
}
