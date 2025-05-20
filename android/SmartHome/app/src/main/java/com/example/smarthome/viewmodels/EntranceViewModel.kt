package com.example.smarthome.viewmodels

class EnteranceViewModel() : RoomViewModel(com.example.smarthome.models.RoomName.ENTERANCE) {
    val girisLiveData = repository.girisLiveData
}
