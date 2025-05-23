package com.example.smarthome.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smarthome.R
import com.example.smarthome.models.Room
import com.example.smarthome.viewmodels.GarageViewModel
import com.example.smarthome.api.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.example.smarthome.api.SensorUpdateRequest

class GarageFragment : Fragment() {
    private val viewModel: GarageViewModel by viewModels()
    private lateinit var switchGarageDoor: SwitchCompat
    private lateinit var switchGarageLight: SwitchCompat
    private val TAG = "GarageFragment"

    // Flag to prevent listener from triggering during programmatic changes
    private var isUserAction = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_garage, container, false)

        // View tanımlamaları
        switchGarageDoor = view.findViewById(R.id.switchGarageDoor)
        switchGarageLight = view.findViewById(R.id.switchGarageLight)

        // LiveData'yı gözlemle
        viewModel.garageLiveData.observe(viewLifecycleOwner) {
            onRoomData(it)
        }

        // Kapı değişikliği dinle
        switchGarageDoor.setOnCheckedChangeListener { _, isChecked ->
            // Only process the event if it's from user interaction
            if (isUserAction) {
                viewModel.setGarageDoorOpen(isChecked)
                // Send appropriate garage door command
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (isChecked) {
                            RetrofitInstance.smartHomeApiService.sendGarajOpenCommand()
                            Log.d(TAG, "Garage door open command sent successfully")
                        } else {
                            RetrofitInstance.smartHomeApiService.sendGarajCloseCommand()
                            Log.d(TAG, "Garage door close command sent successfully")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending garage door command: ${e.message}")
                    }
                }
            }
        }

        // Işık değişikliği dinle
        switchGarageLight.setOnCheckedChangeListener { _, isChecked ->
            // Only process the event if it's from user interaction
            if (isUserAction) {
                viewModel.setLightOn(isChecked)
                // Send light command
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitInstance.smartHomeApiService.updateSensorData(
                            "garaj",
                            "light",
                            SensorUpdateRequest(if (isChecked) "on" else "off")
                        )
                        Log.d(TAG, "Light command sent successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending light command: ${e.message}")
                    }
                }
            }
        }

        return view
    }

    private fun onRoomData(room: Room) {
        handleDoorStatus(room.garageDoor)
        handleLightStatus(room.lightIsOn)
    }

    private fun handleDoorStatus(isOpen: Boolean?) {
        // Temporarily disable the listener
        isUserAction = false
        switchGarageDoor.isChecked = isOpen ?: false
        isUserAction = true
    }

    private fun handleLightStatus(lightIsOn: Boolean) {
        // Temporarily disable the listener
        isUserAction = false
        switchGarageLight.isChecked = lightIsOn
        isUserAction = true
    }
}
