package com.example.smarthome.utils

import com.example.smarthome.api.RetrofitInstance
import com.example.smarthome.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth

object AppState {
    val authState = FirebaseAuth.getInstance().addAuthStateListener { state ->
        state.currentUser
    }

    val api = RetrofitInstance.smartHomeApiService

    val roomRepository = RoomRepository(api)
}