package com.example.smarthome.viewmodels

import androidx.lifecycle.LiveData
import com.example.smarthome.api.RetrofitInstance
import com.example.smarthome.models.Room
import com.example.smarthome.models.RoomName
import com.example.smarthome.network.SensorUpdateRequest

class MainRoomViewModel : RoomViewModel(RoomName.SALON) {
    val salonLiveData: LiveData<Room> = repository.salonLiveData

    suspend fun getGasLevel() = RetrofitInstance.smartHomeApiService.getSensorData("salon", "gas")

    fun setTemperature(value: Float) {
        repository.setTemperature(value)
    }

    suspend fun getTemperature() = RetrofitInstance.smartHomeApiService.getSensorData("salon", "temperature")
}