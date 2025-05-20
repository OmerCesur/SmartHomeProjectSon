package com.example.smarthome.services

import android.content.Context
import android.util.Log
import com.example.smarthome.api.RetrofitInstance
import com.example.smarthome.firebase.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect

class GasSensorMonitor(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationHelper = NotificationHelper(context)
    private var lastGasValue: String? = null
    private var lastGasSeverity: String? = null
    private var lastNotificationSeverity: String? = null
    private var isMonitoring = false

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        scope.launch {
            try {
                // Initial check
                val initialResponse = RetrofitInstance.smartHomeApiService.getSensorData("salon", "gas")
                processGasData(initialResponse)

                // Start real-time monitoring
                while (isMonitoring) {
                    try {
                        val response = RetrofitInstance.smartHomeApiService.getSensorData("salon", "gas")
                        processGasData(response)
                    } catch (e: Exception) {
                        Log.e("GasSensorMonitor", "Error fetching gas sensor data: ${e.message}")
                    }
                    delay(1000) // Check every second for real-time updates
                }
            } catch (e: Exception) {
                Log.e("GasSensorMonitor", "Error in monitoring: ${e.message}")
                isMonitoring = false
            }
        }
    }

    private fun processGasData(response: com.example.smarthome.api.SensorDataResponse) {
        // Check if gas level or severity has changed
        if (response.status.value.toString() != lastGasValue || response.status.severity != lastGasSeverity) {
            // Update last values
            lastGasValue = response.status.value.toString()
            lastGasSeverity = response.status.severity

            // Show notification if severity is not normal and different from last notification
            if (response.status.severity != "normal" && response.status.severity != lastNotificationSeverity) {
                scope.launch(Dispatchers.Main) {
                    notificationHelper.showGasSensorNotification(
                        "Gas Sensor Alert",
                        "Gas level: ${response.status.value} (Severity: ${response.status.severity})",
                        response.status.severity ?: "normal"
                    )
                }
                lastNotificationSeverity = response.status.severity
            } else if (response.status.severity == "normal") {
                // Reset last notification severity when gas level returns to normal
                lastNotificationSeverity = null
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
    }
} 