package com.example.smarthome.models

data class GarageDoorResponse(
    val message: String,
    val room: String,
    val sensorType: String,
    val data: GarageDoorData
)

data class GarageDoorData(
    val value: String,
    val timestamp: String
)
