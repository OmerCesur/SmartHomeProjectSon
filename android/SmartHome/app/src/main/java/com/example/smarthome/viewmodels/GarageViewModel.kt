package com.example.smarthome.viewmodels

import androidx.lifecycle.LiveData
import com.example.smarthome.models.Room
import com.example.smarthome.models.RoomName
import com.example.smarthome.utils.AppState

class GarageViewModel : RoomViewModel(RoomName.GARAGE) {
    val appState = AppState
    private val roomRepository = appState.roomRepository

    // Garaj verilerini tutan LiveData
    val garageLiveData: LiveData<Room> = repository.garajLiveData

    // Garaj kapısını aç/kapat
    fun setGarageDoorOpen(isOpen: Boolean) {
        repository.setGarageDoorOpen(isOpen)
    }
} 