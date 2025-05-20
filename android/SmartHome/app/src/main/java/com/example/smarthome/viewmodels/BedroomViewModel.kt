package com.example.smarthome.viewmodels

import androidx.lifecycle.LiveData
import com.example.smarthome.models.Room
import com.example.smarthome.utils.AppState
import com.example.smarthome.models.RoomName

class BedroomViewModel: RoomViewModel(RoomName.BEDROOM) {
    val appState = AppState
    private val roomRepository = appState.roomRepository

    val bedroomLiveData: LiveData<Room> = roomRepository.bedroomLiveData

    fun setCurtainsOpen(checked: Boolean) {
        roomRepository.setCurtainsOpen(checked)
    }
}