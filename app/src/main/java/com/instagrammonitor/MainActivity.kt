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
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        permissionButton = findViewById(R.id.permissionButton)

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
    }
}
