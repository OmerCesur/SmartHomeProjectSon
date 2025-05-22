package com.example.smarthome.services

import android.content.Context
import android.util.Log
import com.example.smarthome.api.RetrofitInstance
import com.example.smarthome.api.SensorDataResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class TemperatureSensorMonitor(private val context: Context) {
    private val TAG = "TemperatureSensorMonitor"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var lastTemperature: Float? = null
    private var isMonitoring = false
    private var retryCount = 0
    private val maxRetries = 3
    
    private val _temperatureFlow = MutableStateFlow<Float?>(null)
    val temperatureFlow: StateFlow<Float?> = _temperatureFlow.asStateFlow()

    fun startMonitoring() {
        Log.d(TAG, "startMonitoring called, isMonitoring: $isMonitoring")
        if (isMonitoring) {
            Log.d(TAG, "Already monitoring, returning")
            return
        }
        isMonitoring = true
        retryCount = 0
        Log.d(TAG, "Starting temperature monitoring")
        
        scope.launch {
            try {
                Log.d(TAG, "Making initial API call")
                // Initial check with retry logic
                var success = false
                while (!success && retryCount < maxRetries) {
                    try {
                        val initialResponse = withContext(Dispatchers.IO) {
                            RetrofitInstance.smartHomeApiService.getSensorData("salon", "temperature")
                        }
                        Log.d(TAG, "Initial response received: $initialResponse")
                        processTemperatureData(initialResponse)
                        success = true
                        retryCount = 0
                    } catch (e: HttpException) {
                        retryCount++
                        Log.e(TAG, "HTTP Error (attempt $retryCount): ${e.message}")
                        if (retryCount < maxRetries) {
                            delay(2000) // Wait 2 seconds before retry
                        } else {
                            Log.e(TAG, "Max retries reached, stopping monitoring")
                            isMonitoring = false
                            throw e
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in initial API call: ${e.message}")
                        isMonitoring = false
                        throw e
                    }
                }

                // Start real-time monitoring
                while (isMonitoring) {
                    try {
                        Log.d(TAG, "Fetching temperature data...")
                        val response = withContext(Dispatchers.IO) {
                            RetrofitInstance.smartHomeApiService.getSensorData("salon", "temperature")
                        }
                        Log.d(TAG, "Response received: $response")
                        processTemperatureData(response)
                        retryCount = 0 // Reset retry count on successful call
                    } catch (e: HttpException) {
                        retryCount++
                        Log.e(TAG, "HTTP Error (attempt $retryCount): ${e.message}")
                        if (retryCount >= maxRetries) {
                            Log.e(TAG, "Max retries reached, stopping monitoring")
                            isMonitoring = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching temperature sensor data: ${e.message}", e)
                    }
                    Log.d(TAG, "Waiting 5 seconds before next update")
                    delay(5000) // Check every 5 seconds for updates
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring: ${e.message}", e)
                isMonitoring = false
            }
        }
    }

    private fun processTemperatureData(response: SensorDataResponse) {
        try {
            Log.d(TAG, "Processing temperature data: $response")
            // The value is stored in response.status.value
            val value = response.status.value
            Log.d(TAG, "Raw temperature value: $value")
            
            val temperature = when (value) {
                is Number -> {
                    Log.d(TAG, "Value is a number: ${value.toFloat()}")
                    value.toFloat()
                }
                is String -> {
                    Log.d(TAG, "Value is a string: $value")
                    value.toFloatOrNull()
                }
                else -> {
                    Log.d(TAG, "Value is of unknown type: ${value?.javaClass?.name}")
                    null
                }
            }
            
            Log.d(TAG, "Processed temperature: $temperature, Last temperature: $lastTemperature")
            if (temperature != null && temperature != lastTemperature) {
                lastTemperature = temperature
                Log.d(TAG, "Emitting new temperature: $temperature")
                _temperatureFlow.value = temperature
            } else {
                Log.d(TAG, "Temperature unchanged or null, not emitting")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing temperature data: ${e.message}", e)
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping temperature monitoring")
        isMonitoring = false
    }
} 