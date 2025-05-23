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
import retrofit2.HttpException
import java.io.IOException

class RoomRepository(private val api: SmartHomeApiService) {
    private val TAG = "RoomRepository"

    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Error: ${throwable.message}", throwable)
            when (throwable) {
                is HttpException -> handleHttpError(throwable)
                is IOException -> handleNetworkError(throwable)
                else -> handleGenericError(throwable)
            }
        }
    )

    private val _bedroomLiveData = MutableLiveData<Room>(Room("yatak_odasi", false))
    private val _salonLiveData = MutableLiveData<Room>(Room("salon", false))
    private val _garajLiveData = MutableLiveData<Room>(Room("garaj", false))
    private val _banyoLiveData = MutableLiveData<Room>(Room("banyo", false))
    private val _girisLiveData = MutableLiveData<Room>(Room("giris", false))

    val bedroomLiveData: LiveData<Room> = _bedroomLiveData
    val salonLiveData: LiveData<Room> = _salonLiveData
    val garajLiveData: LiveData<Room> = _garajLiveData
    val banyoLiveData: LiveData<Room> = _banyoLiveData
    val girisLiveData: LiveData<Room> = _girisLiveData

    fun fetchRooms() {
        coroutineScope.launch {
            try {
                _bedroomLiveData.postValue(fetchBedroom())
                _salonLiveData.postValue(getSalon())
                _garajLiveData.postValue(getGaraj())
                _banyoLiveData.postValue(getBanyo())
                _girisLiveData.postValue(getGiris())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching rooms: ${e.message}")
                handleError(e)
            }
        }
    }

    private suspend fun fetchBedroom(): Room {
        return try {
            val curtainData = api.getSensorData("yatak_odasi", "curtain")
            val lightData = api.getSensorData("yatak_odasi", "light")

            Room(
                "yatak_odasi",
                lightData.status.value == "on",
                curtain = curtainData.status.value == "open"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bedroom data: ${e.message}")
            Room("yatak_odasi", false)
        }
    }

    private suspend fun getSalon(): Room {
        return try {
            val temperatureData = api.getSensorData("salon", "temperature")
            val gasData = api.getSensorData("salon", "gas")
            val lightData = api.getSensorData("salon", "light")

            Room(
                name = "salon",
                lightIsOn = lightData.status.value == "on",
                temperatureCelcius = temperatureData.status.value as? Float,
                gasSeverity = gasData.status.severity
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching salon data: ${e.message}")
            Room("salon", false)
        }
    }

    private suspend fun getGaraj(): Room {
        return try {
            val doorData = api.getSensorData("garaj", "door")
            val lightData = api.getSensorData("garaj", "light")

            Room(
                "garaj",
                lightData.status.value == "on",
                garageDoor = doorData.status.value == "on"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching garage data: ${e.message}")
            Room("garaj", false)
        }
    }

    private suspend fun getGiris(): Room {
        return try {
            val faceIdData = api.getSensorData("giris", "face_id")
            val lightData = api.getSensorData("giris", "light")

            Room(
                "giris",
                lightIsOn = lightData.status.value == "on",
                faceData = faceIdData.status.value as? FaceData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching entrance data: ${e.message}")
            Room("giris", false)
        }
    }

    private suspend fun getBanyo(): Room {
        return try {
            val lightData = api.getSensorData("banyo", "light")
            Room(
                "banyo",
                lightData.status.value == "on"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bathroom data: ${e.message}")
            Room("banyo", false)
        }
    }

    fun setCurtainsOpen(checked: Boolean) {
        coroutineScope.launch {
            try {
                api.updateSensorData("yatak_odasi", "curtain", SensorUpdateRequest(
                    if (checked) "open" else "close"
                ))
                _bedroomLiveData.postValue(fetchBedroom())
            } catch (e: Exception) {
                Log.e(TAG, "Error setting curtains: ${e.message}")
                handleError(e)
            }
        }
    }

    fun setGarageDoorOpen(checked: Boolean) {
        coroutineScope.launch {
            try {
                api.updateSensorData(
                    "garaj",
                    "door",
                    SensorUpdateRequest(if (checked) "on" else "off")
                )
                _garajLiveData.postValue(getGaraj())
            } catch (e: Exception) {
                Log.e(TAG, "Error setting garage door: ${e.message}")
                handleError(e)
            }
        }
    }

    fun setLightOn(roomName: RoomName, checked: Boolean) {
        coroutineScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error setting light: ${e.message}")
                handleError(e)
            }
        }
    }

    fun setTemperature(value: Float) {
        coroutineScope.launch {
            try {
                api.updateSensorData("salon", "temperature", SensorUpdateRequest(value.toString()))
                _salonLiveData.postValue(getSalon())
            } catch (e: Exception) {
                Log.e(TAG, "Error setting temperature: ${e.message}")
                handleError(e)
            }
        }
    }

    private fun handleHttpError(error: HttpException) {
        Log.e(TAG, "HTTP Error: ${error.code()} - ${error.message()}")
        // Handle specific HTTP error codes
        when (error.code()) {
            401 -> handleUnauthorized()
            403 -> handleForbidden()
            404 -> handleNotFound()
            500 -> handleServerError()
            else -> handleGenericError(error)
        }
    }

    private fun handleNetworkError(error: IOException) {
        Log.e(TAG, "Network Error: ${error.message}")
        // Handle network connectivity issues
    }

    private fun handleGenericError(error: Throwable) {
        Log.e(TAG, "Generic Error: ${error.message}")
        // Handle generic errors
    }

    private fun handleUnauthorized() {
        // Handle unauthorized access
    }

    private fun handleForbidden() {
        // Handle forbidden access
    }

    private fun handleNotFound() {
        // Handle not found errors
    }

    private fun handleServerError() {
        // Handle server errors
    }

    private fun handleError(error: Throwable) {
        when (error) {
            is HttpException -> handleHttpError(error)
            is IOException -> handleNetworkError(error)
            else -> handleGenericError(error)
        }
    }
}
