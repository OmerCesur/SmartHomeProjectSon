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

class BedRoomFragment : Fragment() {
    private val viewModel: BedroomViewModel by viewModels()
    private lateinit var switchCurtain: SwitchCompat
    private lateinit var switchBedroomLight: SwitchCompat

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
                switchCurtain.isChecked = it
            }

            it.lightIsOn.let {
                switchBedroomLight.isChecked = it
            }
        }

        switchCurtain.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCurtainsOpen(isChecked)
        }

        switchBedroomLight.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLightOn(isChecked)
        }

        return view
    }
}
