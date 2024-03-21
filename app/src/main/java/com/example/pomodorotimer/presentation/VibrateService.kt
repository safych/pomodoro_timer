package com.example.pomodorotimer.presentation

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.pomodorotimer.R

class VibrateService : Service() {
    private var alarmManager: AlarmManager? = null
    private var notificationManager: NotificationManager? = null
    private val channelId = "channel_vibrate"
    private val notificationId = 1
    private val notificationUpdateInterval = 540000L
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val handler = Handler()
    private var isServiceRunning = false
    private var remainingTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        startForegroundService()
        startNotificationUpdates()
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager!!.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        notificationBuilder = createNotification()
        startForeground(notificationId, notificationBuilder.build())
    }

    private fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.baseline_warning_amber_24)
    }

    private fun updateNotification() {
        notificationBuilder.setContentText("Service is still active and working...")
        val updatedNotification = notificationBuilder.build()
        startForeground(notificationId, updatedNotification)
    }

    private fun startNotificationUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isServiceRunning) {
                    updateNotification()
                    handler.postDelayed(this, notificationUpdateInterval)
                }
            }
        }, notificationUpdateInterval)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val millisecondsRemaining = intent?.getLongExtra("millisecondsRemaining", 0)
        remainingTime = millisecondsRemaining ?: 0
        scheduleAlarm(remainingTime)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
        cancelAlarm()
        stopForeground(true)
        notificationManager!!.deleteNotificationChannel(channelId)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleAlarm(millisecondsRemaining: Long) {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + millisecondsRemaining,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }
}
