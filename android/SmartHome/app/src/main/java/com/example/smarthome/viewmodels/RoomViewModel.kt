package com.example.smarthome.viewmodels

import androidx.lifecycle.ViewModel
import com.example.smarthome.utils.AppState

abstract class RoomViewModel(val roomName: com.example.smarthome.models.RoomName) : ViewModel() {
    protected val repository = AppState.roomRepository

    fun setLightOn(checked: Boolean) {
        repository.setLightOn(roomName, checked)
    }
}