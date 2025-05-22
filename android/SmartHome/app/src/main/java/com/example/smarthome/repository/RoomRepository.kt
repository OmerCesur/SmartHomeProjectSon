package com.example.smarthome.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smarthome.api.SensorUpdateRequest
import com.example.smarthome.api.SmartHomeApiService
import com.example.smarthome.models.FaceData
import com.example.smarthome.models.Room
import com.example.smarthome.models.RoomName
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RoomRepository(val api: SmartHomeApiService) {

    fun fetchRooms() {
        coroutineScope.launch {
            _bedroomLiveData.postValue(fetchBedroom())
            _salonLiveData.postValue(getSalon())
            _garajLiveData.postValue(getGaraj())
            _banyoLiveData.postValue(getBanyo())
            _girisLiveData.postValue(getGiris())
        }
    }

    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e("RoomRepository", "Error: ${throwable.message}", throwable)
        }
    )

    private val _bedroomLiveData: MutableLiveData<Room> = MutableLiveData(
        Room("yatak_odasi", false)
    )

    private val _salonLiveData: MutableLiveData<Room> = MutableLiveData(
        Room("salon", false)
    )

    private val _garajLiveData: MutableLiveData<Room> = MutableLiveData(
        Room("garaj", false)
    )

    private val _banyoLiveData: MutableLiveData<Room> = MutableLiveData(
        Room("banyo", false)
    )

    private val _girisLiveData: MutableLiveData<Room> = MutableLiveData(
        Room("giris", false)
    )

    val bedroomLiveData: LiveData<Room> = _bedroomLiveData
    val salonLiveData: LiveData<Room> = _salonLiveData
    val garajLiveData: LiveData<Room> = _garajLiveData
    val banyoLiveData: LiveData<Room> = _banyoLiveData
    val girisLiveData: LiveData<Room> = _girisLiveData



    private suspend fun fetchBedroom(): Room {
        val curtainData = api.getSensorData("yatak_odasi", "curtain")
        val lightData = api.getSensorData("yatak_odasi", "light")

        return Room(
            "yatak_odasi",
            lightData.status.value == "on",
            curtain = curtainData.status.value == "open"
        )
    }

    private suspend fun getSalon(): Room {
        val temperatureData = api.getSensorData("salon", "temperature")
        val gasData = api.getSensorData("salon", "gas")
        val lightData = api.getSensorData("salon", "light")

        return Room(
            name = "salon",
            lightIsOn = lightData.status.value == "on",
            temperatureCelcius = temperatureData.status.value as? Float,
            gasSeverity = gasData.status.severity  // ✅ artık doğrudan severity'yi alıyoruz
        )
    }

    private suspend fun getGaraj(): Room {
        val doorData = api.getSensorData("garaj", "door")
        val lightData = api.getSensorData("garaj", "light")

        return Room(
            "garaj",
            lightData.status.value == "on",
            garageDoor = doorData.status.value == "on"
        )
    }

    private suspend fun getGiris(): Room {
        val faceIdData = api.getSensorData("giris", "face_id")
        val lightData = api.getSensorData("giris", "light")

        return Room(
            "giris",
            lightIsOn = lightData.status.value == "on",
            faceData = faceIdData.status.value as? FaceData
        )
    }

    private suspend fun getBanyo(): Room {
        val lightData = api.getSensorData("banyo", "light")
        return Room(
            "banyo",
            lightData.status.value == "on",
        )
    }

    fun setCurtainsOpen(checked: Boolean) {
        coroutineScope.launch {
            api.updateSensorData("yatak_odasi", "curtain", SensorUpdateRequest(
                if (checked) "open" else "close"
            ))

            _bedroomLiveData.postValue(fetchBedroom())
        }
    }

    fun setGarageDoorOpen(checked: Boolean) {
        coroutineScope.launch {
                api.updateSensorData(
                    "garaj",
                    "door",
                    SensorUpdateRequest(if (checked) "on" else "off")
                )
                _garajLiveData.postValue(getGaraj())
        }
    }

    fun setLightOn(roomName: RoomName, checked: Boolean) {
        coroutineScope.launch {

            val roomNameString = when (roomName) {
                RoomName.BEDROOM -> "yatak_odasi"
                RoomName.SALON -> "salon"
                RoomName.GARAGE -> "garaj"
                RoomName.BATHROOM -> "banyo"
                RoomName.ENTERANCE -> "giris"
            }

            api.updateSensorData(roomNameString, "light", SensorUpdateRequest(
                if(checked) "on" else "off"
            ))

            when (roomName) {
                RoomName.BEDROOM -> _bedroomLiveData.postValue(fetchBedroom())
                RoomName.SALON -> _salonLiveData.postValue(getSalon())
                RoomName.GARAGE -> _garajLiveData.postValue(getGaraj())
                RoomName.BATHROOM -> _banyoLiveData.postValue(getBanyo())
                RoomName.ENTERANCE -> _girisLiveData.postValue(getGiris())
            }
        }
    }

    fun setTemperature(value: Float) {
        coroutineScope.launch {
            try {
                // Convert float to string for API compatibility
                api.updateSensorData("salon", "temperature", SensorUpdateRequest(value.toString()))
                _salonLiveData.postValue(getSalon()) // Get updated temperature
            } catch (e: Exception) {
                Log.e("RoomRepository", "Error setting temperature: ${e.message}")
            }
        }
    }
}
