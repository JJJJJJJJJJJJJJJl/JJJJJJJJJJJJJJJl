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
    private val timeLimit = 5 * 60 * 1000L // 5 minutes in milliseconds

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
        val queryTime = currentTime - 10000 // Look back 10 seconds

        val usageEvents = usageStatsManager.queryEvents(queryTime, currentTime)
        val event = UsageEvents.Event()

        var instagramInForeground = false

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            if (event.packageName == INSTAGRAM_PACKAGE) {
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        instagramInForeground = true
                        if (!isInstagramActive) {
                            // Instagram just became active
                            instagramStartTime = currentTime
                            isInstagramActive = true
                            hasAlerted = false
                        }
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        instagramInForeground = false
                    }
                }
            }
        }

        // If no recent foreground event found, check if we need to reset
        if (!instagramInForeground && isInstagramActive) {
            isInstagramActive = false
            hasAlerted = false
        }

        // Check time limit
        if (isInstagramActive && !hasAlerted) {
            val usageTime = currentTime - instagramStartTime

            if (usageTime >= timeLimit) {
                showAlert(usageTime)
                hasAlerted = true
            } else {
                // Update foreground notification with remaining time
                updateForegroundNotification(usageTime)
            }
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
