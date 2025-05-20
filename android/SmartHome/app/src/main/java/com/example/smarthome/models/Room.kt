package com.example.smarthome.models

data class Room(
    val name: String,
    val lightIsOn: Boolean,
    val temperatureCelcius: Float? = null,
    val curtain: Boolean? = null,
    val gasSeverity: String? = null,
    val garageDoor: Boolean? = null,
    val faceData: FaceData? = null
)

