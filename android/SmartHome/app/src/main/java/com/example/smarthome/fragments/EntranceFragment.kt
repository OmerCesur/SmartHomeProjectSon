package com.example.smarthome.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smarthome.R
import com.example.smarthome.models.FaceData
import com.example.smarthome.models.Room
import com.example.smarthome.viewmodels.EnteranceViewModel


class EntranceFragment : Fragment() {
    private val viewModel : EnteranceViewModel by viewModels()

    private lateinit var switchEntranceLight: SwitchCompat
    private lateinit var ivFaceStatusIcon: ImageView
    private lateinit var tvFaceStatusText: TextView


    // Flag to prevent listener from triggering during programmatic changes
    private var isUserAction = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entrance, container, false)

        // View tanımlamaları
        switchEntranceLight = view.findViewById(R.id.switchEntranceLight)
        ivFaceStatusIcon = view.findViewById(R.id.ivFaceStatusIcon)
        tvFaceStatusText = view.findViewById(R.id.tvFaceStatusText)


        viewModel.girisLiveData.observe(viewLifecycleOwner) {
            onRoomData(it)
        }

        // Işık değişikliği dinle
        switchEntranceLight.setOnCheckedChangeListener { _, isChecked ->
            // Only process the event if it's from user interaction
            if (isUserAction) {
                viewModel.setLightOn(isChecked)
            }
        }

        return view
    }

    private fun onRoomData(room: Room) {
        handleFaceData(room.faceData)
        handleLightStatus(room.lightIsOn)
    }

    private fun handleLightStatus(lightIsOn: Boolean) {
        // Temporarily disable the listener
        isUserAction = false
        switchEntranceLight.isChecked = lightIsOn
        isUserAction = true
    }

    private fun handleFaceData(faceData: FaceData?) {
        if (faceData?.value == "detected") {
            val name = faceData.name ?: "Unknown"
            ivFaceStatusIcon.setImageResource(R.drawable.ic_face_success)
            tvFaceStatusText.text = "✅ Face recognized: $name"
        } else {
            ivFaceStatusIcon.setImageResource(R.drawable.ic_face_recognition)
            tvFaceStatusText.text = "❌ No face detected"
        }
    }
}
