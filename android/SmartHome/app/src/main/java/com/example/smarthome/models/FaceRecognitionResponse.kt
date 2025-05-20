package com.example.smarthome.models

data class FaceRecognitionResponse(
    val message: String,
    val room: String,
    val sensorType: String,
    val data: FaceData
)

data class FaceData(
    val value: String,  // "detected" ya da "not_detected"
    val timestamp: String,
    val name: String? = null  // olabilir de olmayabilir de
)
