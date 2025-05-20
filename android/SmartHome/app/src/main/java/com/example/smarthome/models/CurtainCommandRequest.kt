package com.example.smarthome.models

data class CurtainCommandRequest(
    val room: String,
    val command: String // "on" veya "off"
)
