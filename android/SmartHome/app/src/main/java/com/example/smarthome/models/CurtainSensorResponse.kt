package com.example.smarthome.models

data class CurtainSensorResponse(
    val message: String,
    val room: String,
    val sensorType: String,
    val data: CurtainSensorData
)

data class CurtainSensorData(
    val value: Boolean, // "on" veya "off"
    val timestamp: String
)
