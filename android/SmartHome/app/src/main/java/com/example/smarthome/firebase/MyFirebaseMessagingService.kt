package com.example.smarthome.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smarthome.R
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.example.smarthome.api.SensorDataResponse
import com.example.smarthome.api.SensorInfo
import com.example.smarthome.api.SensorStatus

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle notification payload
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        showNotification(title, body)

        // Handle data payload for gas sensor
        remoteMessage.data["sensor_data"]?.let { sensorDataJson ->
            try {
                val gson = Gson()
                val sensorData = gson.fromJson(sensorDataJson, SensorDataResponse::class.java)
                
                // Check if it's gas sensor data
                if (sensorData.sensorType == "gas") {
                    val severity = sensorData.status.severity ?: "normal"
                    val value = sensorData.status.value
                    
                    // Show gas sensor notification with severity
                    showGasSensorNotification(
                        "Gas Sensor Alert",
                        "Gas level: $value (Severity: $severity)",
                        severity
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "default_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun showGasSensorNotification(title: String, message: String, severity: String) {
        val channelId = "gas_sensor_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
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
