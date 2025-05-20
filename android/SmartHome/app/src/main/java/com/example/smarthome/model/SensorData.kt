package com.example.smarthome.model

data class SensorData(
    val type: String = "",
    val value: Double = 0.0,
    val timestamp: Long = 0
) 