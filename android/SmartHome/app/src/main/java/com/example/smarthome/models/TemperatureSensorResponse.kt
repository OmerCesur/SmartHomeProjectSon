package com.example.smarthome.models

data class TemperatureSensorResponse(
    val message: String,
    val room: String,
    val sensorType: String,
    val data: TemperatureSensorData
)

data class TemperatureSensorData(
    val value: Float,
    val timestamp: String
)
