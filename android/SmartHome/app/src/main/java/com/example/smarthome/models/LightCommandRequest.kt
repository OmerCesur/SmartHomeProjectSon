package com.example.smarthome.models

data class LightCommandRequest(
    val room: String,
    val device: String = "light",
    val command: String
)