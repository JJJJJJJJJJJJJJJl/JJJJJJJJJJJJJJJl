package com.instagrammonitor

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class InstagramMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var instagramStartTime: Long = 0
    private var isInstagramActive = false
    private var hasAlerted = false
    private val checkInterval = 2000L // Check every 2 seconds
    private val timeLimit = 30 * 1000L // 30 seconds for testing (change to 5 * 60 * 1000L for 5 minutes)

    private val INSTAGRAM_PACKAGE = "com.instagram.android"
    private val CHANNEL_ID = "instagram_monitor_channel"
    private val NOTIFICATION_ID = 1
    private val ALERT_NOTIFICATION_ID = 2

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkInstagramUsage()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(monitorRunnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }

    private fun checkInstagramUsage() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        // Get the current foreground app
        val foregroundApp = getCurrentForegroundApp(usageStatsManager, currentTime)
        val isInstagramNowInForeground = foregroundApp == INSTAGRAM_PACKAGE

        // Save debug info
        saveDebugInfo(foregroundApp, isInstagramNowInForeground)

        if (isInstagramNowInForeground) {
            // Instagram is currently in foreground
            if (!isInstagramActive) {
                // Instagram just became active
                instagramStartTime = currentTime
                isInstagramActive = true
                hasAlerted = false
                updateForegroundNotification(0) // Show "5m 0s remaining"
            } else {
                // Instagram is still active, check time
                val usageTime = currentTime - instagramStartTime

                if (usageTime >= timeLimit && !hasAlerted) {
                    showAlert(usageTime)
                    hasAlerted = true
                } else if (!hasAlerted) {
                    // Update foreground notification with remaining time
                    updateForegroundNotification(usageTime)
                }
            }
        } else {
            // Instagram is not in foreground
            if (isInstagramActive) {
                // Instagram was active but now closed
                isInstagramActive = false
                hasAlerted = false
                // Reset notification
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, createForegroundNotification())
            }
        }
    }

    private fun getCurrentForegroundApp(usageStatsManager: UsageStatsManager, currentTime: Long): String? {
        val queryTime = currentTime - 5000 // Look back 5 seconds
        val usageEvents = usageStatsManager.queryEvents(queryTime, currentTime)
        val event = UsageEvents.Event()

        var lastForegroundApp: String? = null
        var lastEventTime = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > lastEventTime) {
                    lastForegroundApp = event.packageName
                    lastEventTime = event.timeStamp
                }
            }
        }

        return lastForegroundApp
    }

    private fun saveDebugInfo(foregroundApp: String?, isInstagram: Boolean) {
        val prefs = getSharedPreferences("InstagramMonitor", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_foreground_app", foregroundApp ?: "none")
            putBoolean("is_instagram_detected", isInstagram)
            putLong("last_check_time", System.currentTimeMillis())
            putBoolean("is_tracking", isInstagramActive)
            putLong("time_elapsed", if (isInstagramActive) System.currentTimeMillis() - instagramStartTime else 0)
            apply()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Instagram Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Monitors Instagram usage time"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Instagram Monitor Active")
            .setContentText("Monitoring Instagram usage...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(usageTime: Long) {
        val remainingTime = timeLimit - usageTime
        val remainingMinutes = remainingTime / 60000
        val remainingSeconds = (remainingTime % 60000) / 1000

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Instagram Monitor Active")
            .setContentText("Time remaining: ${remainingMinutes}m ${remainingSeconds}s")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showAlert(usageTime: Long) {
        val minutes = usageTime / 60000

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Instagram Time Limit Reached!")
            .setContentText("You've been on Instagram for $minutes minutes. Time to take a break!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}
