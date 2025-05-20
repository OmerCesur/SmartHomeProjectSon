package com.example.smarthome.viewmodels

import androidx.lifecycle.LiveData
import com.example.smarthome.models.Room
import com.example.smarthome.models.RoomName
import com.example.smarthome.repository.RoomRepository

class DeviceControlViewModel : RoomViewModel(RoomName.SALON) {
    // LiveData'lar
    val salonLiveData: LiveData<Room> = repository.salonLiveData
    val bedroomLiveData: LiveData<Room> = repository.bedroomLiveData
    val bathroomLiveData: LiveData<Room> = repository.banyoLiveData
    val garageLiveData: LiveData<Room> = repository.garajLiveData
    val entranceLiveData: LiveData<Room> = repository.girisLiveData

    // Light Controls
    fun setLightOn(roomName: RoomName, checked: Boolean) {
        repository.setLightOn(roomName, checked)
    }

    // Curtain Control
    fun setCurtainsOpen(checked: Boolean) {
        repository.setCurtainsOpen(checked)
    }

    // Garage Door Control
    fun setGarageDoorOpen(checked: Boolean) {
        repository.setGarageDoorOpen(checked)
    }
    fun setTemperature(value: Float) {
        repository.setTemperature(value)
    }
} 