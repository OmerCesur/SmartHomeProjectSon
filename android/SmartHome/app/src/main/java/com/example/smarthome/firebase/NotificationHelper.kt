package com.example.smarthome.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smarthome.R

class NotificationHelper(private val context: Context) {

    fun showGasSensorNotification(title: String, message: String, severity: String) {
        val channelId = "gas_sensor_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gas Sensor Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for gas sensor alerts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Set different colors based on severity
        when (severity.lowercase()) {
            "high" -> notificationBuilder.setColor(0xFF0000.toInt()) // Red
            "medium" -> notificationBuilder.setColor(0xFFFFA500.toInt()) // Orange
            "low" -> notificationBuilder.setColor(0xFFFFFF00.toInt()) // Yellow
            else -> notificationBuilder.setColor(0xFF00FF00.toInt()) // Green
        }

        notificationManager.notify(1, notificationBuilder.build())
    }
} 