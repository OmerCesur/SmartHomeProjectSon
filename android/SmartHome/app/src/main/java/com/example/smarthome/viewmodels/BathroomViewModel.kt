package com.example.smarthome.viewmodels

import androidx.lifecycle.LiveData
import com.example.smarthome.models.Room
import com.example.smarthome.models.RoomName

class BathroomViewModel : RoomViewModel(RoomName.BATHROOM) {
    val bathroomLiveData: LiveData<Room> = repository.banyoLiveData
}