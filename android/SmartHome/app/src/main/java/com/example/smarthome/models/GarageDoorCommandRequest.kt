package com.example.smarthome.models

data class GarageDoorCommandRequest(
    val room: String,
    val device: String = "door",
    val command: String // "on" veya "off"
)
