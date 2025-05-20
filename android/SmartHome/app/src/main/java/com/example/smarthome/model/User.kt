package com.example.smarthome.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "guest" // Default role is guest
) 