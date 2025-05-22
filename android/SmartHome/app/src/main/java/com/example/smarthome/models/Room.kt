package com.example.smarthome.models

data class Room(
    val name: String,
    val lightIsOn: Boolean = false,
    val temperatureCelcius: Float? = null,
    val gasSeverity: String? = null,
    val garageDoor: Boolean = false,
    val curtain: Boolean = false,
    val faceData: FaceData? = null
)

data class SensorData(
    val value: Any,
    val timestamp: String
)

data class TemperatureData(
    val value: Float,
    val timestamp: String
)

