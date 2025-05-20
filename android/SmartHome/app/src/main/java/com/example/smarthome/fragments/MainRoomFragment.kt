package com.example.smarthome.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smarthome.R
import com.example.smarthome.models.Room
import com.example.smarthome.viewmodels.MainRoomViewModel
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainRoomFragment : Fragment() {

    private val TAG = "MainRoomFragment"
    private val viewModel: MainRoomViewModel by viewModels()
    private lateinit var switchMainRoomLight: SwitchCompat
    private var isUserAction = true
    private lateinit var tvGasStatus: TextView
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvTargetTemp: TextView
    private lateinit var seekBarTemp: SeekBar
    private lateinit var btnSetTemp: Button
    private var selectedTemp: Float? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_room, container, false)

        switchMainRoomLight = view.findViewById(R.id.switchMainRoomLight)
        tvGasStatus = view.findViewById(R.id.tvGasStatus)
        tvCurrentTemp = view.findViewById(R.id.tvCurrentTemp)
        tvTargetTemp = view.findViewById(R.id.tvTargetTemp)
        seekBarTemp = view.findViewById(R.id.seekBarTemp)
        btnSetTemp = view.findViewById(R.id.btnSetTemp)

        // Set initial values
        tvTargetTemp.text = "Target Temperature: --°C"
        tvCurrentTemp.text = "Current Temperature: --°C"
        updateGasStatus("normal", "0") // Initial gas status

        // Check gas level initially
        checkGasLevel()

        viewModel.salonLiveData.observe(viewLifecycleOwner) {
            onRoomData(it)
        }

        switchMainRoomLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) {
                viewModel.setLightOn(isChecked)
            }
        }

        seekBarTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedTemp = progress.toFloat()
                tvTargetTemp.text = "Target Temperature: $progress°C"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSetTemp.setOnClickListener {
            selectedTemp?.let { temp ->
                viewModel.setTemperature(temp)
            }
        }

        return view
    }

    private fun checkGasLevel() {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    viewModel.getGasLevel()
                }
                
                val value = response.status.value.toString()
                val severity = response.status.severity ?: "normal"
                Log.d(TAG, "Gas data updated - Value: $value, Severity: $severity")
                updateGasStatus(severity, value)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking gas level: ${e.message}", e)
                updateGasStatus("unknown", "0")
            }
        }
    }

    private fun updateGasStatus(severity: String, value: String) {
        val (statusText, backgroundColor) = when (severity.lowercase()) {
            "high" -> Pair("🔴 Gas Level: ALARM", R.color.gasHigh)
            "medium" -> Pair("🟠 Gas Level: MEDIUM", R.color.gasMedium)
            "low" -> Pair("🟢 Gas Level: NORMAL", R.color.gasLow)
            else -> Pair("⚪ Gas Level: UNKNOWN", R.color.gasUnknown)
        }

        tvGasStatus.apply {
            text = "$statusText ($value)"
            setBackgroundResource(backgroundColor)
            setTextColor(Color.BLACK)
        }
    }

    private fun onRoomData(room: Room) {
        handleLightStatus(room.lightIsOn)
        room.temperatureCelcius?.let {
            tvCurrentTemp.text = "Current Temperature: ${it.toInt()}°C"
            seekBarTemp.progress = it.toInt()
            selectedTemp = it
        } ?: run {
            tvCurrentTemp.text = "Current Temperature: --°C"
        }
    }

    private fun handleLightStatus(lightIsOn: Boolean) {
        isUserAction = false
        switchMainRoomLight.isChecked = lightIsOn
        isUserAction = true
    }
}
