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
import com.example.smarthome.viewmodels.BathroomViewModel
import com.example.smarthome.api.RetrofitInstance
import com.example.smarthome.api.SensorUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class BathroomFragment : Fragment() {

    private val viewModel: BathroomViewModel by viewModels()
    private lateinit var switchBathroomLight: SwitchCompat
    private var isUserAction = true
    private val TAG = "BathroomFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_bath_room, container, false)

        switchBathroomLight = view.findViewById(R.id.switchBathroomLight)

        viewModel.bathroomLiveData.observe(viewLifecycleOwner) {
            onRoomData(it)
        }

        switchBathroomLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) {
                viewModel.setLightOn(isChecked)
                // Send light command
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitInstance.smartHomeApiService.updateSensorData(
                            "banyo",
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
        handleLightStatus(room.lightIsOn)
    }

    private fun handleLightStatus(lightIsOn: Boolean) {
        isUserAction = false
        switchBathroomLight.isChecked = lightIsOn
        isUserAction = true
    }
}

