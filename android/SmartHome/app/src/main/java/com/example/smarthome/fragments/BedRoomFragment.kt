package com.example.smarthome.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smarthome.R
import com.example.smarthome.viewmodels.BedroomViewModel
import com.example.smarthome.api.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.example.smarthome.api.SensorUpdateRequest

class BedRoomFragment : Fragment() {
    private val viewModel: BedroomViewModel by viewModels()
    private lateinit var switchCurtain: SwitchCompat
    private lateinit var switchBedroomLight: SwitchCompat
    private val TAG = "BedRoomFragment"

    // Flag to prevent listener from triggering during programmatic changes
    private var isUserAction = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_bed_room, container, false)

        switchCurtain = view.findViewById(R.id.switchCurtain)
        switchBedroomLight = view.findViewById(R.id.switchBedroomLight)

        // lifecyclew owner: fragment bir lifecycle'a sahip. yani
        // onresume, onpause, ondestroy gibi durumlar olabilir
        // viewLifecycleOwner da fragmentin VIEWINININ lifecycle'ı
        viewModel.bedroomLiveData.observe(viewLifecycleOwner) {
            it.curtain?.let {
                // Temporarily disable the listener
                isUserAction = false
                switchCurtain.isChecked = it
                isUserAction = true
            }

            it.lightIsOn.let {
                switchBedroomLight.isChecked = it
            }
        }

        switchCurtain.setOnCheckedChangeListener { _, isChecked ->
            // Only process the event if it's from user interaction
            if (isUserAction) {
                viewModel.setCurtainsOpen(isChecked)
                // Send appropriate curtain command
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (isChecked) {
                            RetrofitInstance.smartHomeApiService.sendPerdeOpenCommand()
                            Log.d(TAG, "Curtain open command sent successfully")
                        } else {
                            RetrofitInstance.smartHomeApiService.sendPerdeCloseCommand()
                            Log.d(TAG, "Curtain close command sent successfully")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending curtain command: ${e.message}")
                    }
                }
            }
        }

        switchBedroomLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) {
                viewModel.setLightOn(isChecked)
                // Send light command
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitInstance.smartHomeApiService.updateSensorData(
                            "yatak_odasi",
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
}
