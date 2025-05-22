package com.example.smarthome.fragments

import android.os.Bundle
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
import com.example.smarthome.models.RoomName
import com.example.smarthome.viewmodels.DeviceControlViewModel

class DeviceControlFragment : Fragment() {

    private val viewModel: DeviceControlViewModel by viewModels()
    private var isUserAction = true

    // Light Switches
    private lateinit var switchMainRoomLight: SwitchCompat
    private lateinit var switchBedroomLight: SwitchCompat
    private lateinit var switchBathroomLight: SwitchCompat
    private lateinit var switchGarageLight: SwitchCompat
    private lateinit var switchEntranceLight: SwitchCompat


    private lateinit var switchCurtain: SwitchCompat
    private lateinit var switchGarageDoor: SwitchCompat
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvTargetTemp: TextView
    private lateinit var seekBarTemp: SeekBar
    private lateinit var btnSetTemp: Button
    private var selectedTemp: Float? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_device_control, container, false)

        // Initialize views
        initializeViews(view)
        setupListeners()
        observeData()

        return view
    }

    private fun initializeViews(view: View) {
        // Light Switches
        switchMainRoomLight = view.findViewById(R.id.switchMainRoomLight)
        switchBedroomLight = view.findViewById(R.id.switchBedroomLight)
        switchBathroomLight = view.findViewById(R.id.switchBathroomLight)
        switchGarageLight = view.findViewById(R.id.switchGarageLight)
        switchEntranceLight = view.findViewById(R.id.switchEntranceLight)

        // Other Controls
        switchCurtain = view.findViewById(R.id.switchCurtain)
        switchGarageDoor = view.findViewById(R.id.switchGarageDoor)
        tvCurrentTemp = view.findViewById(R.id.tvCurrentTemp)
        tvTargetTemp = view.findViewById(R.id.tvTargetTemp)
        seekBarTemp = view.findViewById(R.id.seekBarTemp)
        btnSetTemp = view.findViewById(R.id.btnSetTemp)

        tvTargetTemp.text = "Target Temperature: --°C"
        tvCurrentTemp.text = "Current Temperature: --°C"
    }

    private fun setupListeners() {
        // Light Switches
        switchMainRoomLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setLightOn(RoomName.SALON, isChecked)
        }
        switchBedroomLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setLightOn(RoomName.BEDROOM, isChecked)
        }
        switchBathroomLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setLightOn(RoomName.BATHROOM, isChecked)
        }
        switchGarageLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setLightOn(RoomName.GARAGE, isChecked)
        }
        switchEntranceLight.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setLightOn(RoomName.ENTERANCE, isChecked)
        }

        // Other Controls
        switchCurtain.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setCurtainsOpen(isChecked)
        }
        switchGarageDoor.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) viewModel.setGarageDoorOpen(isChecked)
        }

        // Sıcaklık seçimi
        seekBarTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedTemp = progress.toFloat()
                tvTargetTemp.text = "Target Temperature: $progress°C"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Buton ile sıcaklık gönderme
        btnSetTemp.setOnClickListener {
            selectedTemp?.let {
                viewModel.setTemperature(it)
            }
        }
    }

    private fun observeData() {
        // Observe Main Room
        viewModel.salonLiveData.observe(viewLifecycleOwner) { room ->
            handleLightStatus(switchMainRoomLight, room.lightIsOn)
        }

        // Observe Bedroom
        viewModel.bedroomLiveData.observe(viewLifecycleOwner) { room ->
            handleLightStatus(switchBedroomLight, room.lightIsOn)
            room.curtain?.let { isOpen ->
                handleCurtainStatus(isOpen)
            }
        }

        // Observe Bathroom
        viewModel.bathroomLiveData.observe(viewLifecycleOwner) { room ->
            handleLightStatus(switchBathroomLight, room.lightIsOn)
        }

        // Observe Garage
        viewModel.garageLiveData.observe(viewLifecycleOwner) { room ->
            handleLightStatus(switchGarageLight, room.lightIsOn)
            room.garageDoor?.let { isOpen ->
                handleGarageDoorStatus(isOpen)
            }
        }
        // Observe Entrance
        viewModel.entranceLiveData.observe(viewLifecycleOwner) { room ->
            handleLightStatus(switchEntranceLight, room.lightIsOn)
        }
    }

    private fun handleLightStatus(switch: SwitchCompat, isOn: Boolean) {
        isUserAction = false
        switch.isChecked = isOn
        isUserAction = true
    }

    private fun handleCurtainStatus(isOpen: Boolean) {
        isUserAction = false
        switchCurtain.isChecked = isOpen
        isUserAction = true
    }

    private fun handleGarageDoorStatus(isOpen: Boolean) {
        isUserAction = false
        switchGarageDoor.isChecked = isOpen
        isUserAction = true
    }
}
