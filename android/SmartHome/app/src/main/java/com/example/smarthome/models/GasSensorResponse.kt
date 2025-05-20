package com.example.smarthome.models

data class GasSensorResponse(
    val message: String,
    val room: String,
    val sensorType: String,
    val data: GasSensorData
)

data class GasSensorData(
    val value: Int,
    val timestamp: String,
)
