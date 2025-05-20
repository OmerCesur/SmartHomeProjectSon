package com.example.smarthome.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import com.example.smarthome.R

class HomeFragment : Fragment() {



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNavigation(view, R.id.cardEntrance, ::EntranceFragment)
        setupNavigation(view, R.id.cardMainRoom, ::MainRoomFragment)
        setupNavigation(view, R.id.cardBathRoom, ::BathroomFragment)
        setupNavigation(view, R.id.cardBedroom, ::BedRoomFragment)
        setupNavigation(view, R.id.cardGarage, ::GarageFragment)
        setupNavigation(view, R.id.cardDevices, ::DeviceControlFragment)
        setupNavigation(view, R.id.imgViewNotification, ::NotificationFragment)
        setupNavigation(view, R.id.imgViewMembers, ::MembersFragment)
    }

    private fun setupNavigation(view: View, cardId: Int, fragmentSupplier: () -> Fragment) {
        view.findViewById<View>(cardId)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragmentSupplier())
                .addToBackStack(null)
                .commit()
        }
    }
}