package com.example.smarthome.models

data class LightSensorResponse(
    val message: String,
    val room: String,
    val sensorType: String,
    val data: LightSensorData
)

data class LightSensorData(
    val value: String,
    val timestamp: String
)