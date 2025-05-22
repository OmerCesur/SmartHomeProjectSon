package com.example.smarthome.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.example.smarthome.R
import com.example.smarthome.auth.AuthManager
import com.example.smarthome.services.TemperatureSensorMonitor
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private lateinit var tvTempValue: TextView
    private lateinit var temperatureSensorMonitor: TemperatureSensorMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        temperatureSensorMonitor = TemperatureSensorMonitor(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "onViewCreated: Initializing temperature monitoring")
        // Start temperature monitoring
        temperatureSensorMonitor.startMonitoring()
        
        // Observe temperature updates
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Starting temperature flow collection")
            try {
                temperatureSensorMonitor.temperatureFlow.collect { temperature ->
                    Log.d(TAG, "Temperature flow emitted value: $temperature")
                    temperature?.let { temp ->
                        Log.d(TAG, "Processing temperature update: $temp")
                        tvTempValue.text = "${temp.toInt()}°C"
                    } ?: Log.d(TAG, "Received null temperature value")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting temperature updates", e)
            }
        }

        setupNavigation(view, R.id.cardEntrance, ::EntranceFragment)
        setupNavigation(view, R.id.cardMainRoom, ::MainRoomFragment)
        setupNavigation(view, R.id.cardBathRoom, ::BathroomFragment)
        setupNavigation(view, R.id.cardGarage, ::GarageFragment)
        setupNavigation(view, R.id.cardDevices, ::DeviceControlFragment)
        setupNavigation(view, R.id.imgViewNotification, ::NotificationFragment)
        setupNavigation(view, R.id.imgViewMembers, ::MembersFragment)

        // Check user role and restrict Bedroom access for guests
        AuthManager.getCurrentUserRole(
            onSuccess = { role ->
                if (role == "guest") {
                    val cardBedroom = view.findViewById<View>(R.id.cardBedroom)
                    cardBedroom.alpha = 0.5f
                    cardBedroom.setOnClickListener {
                        Toast.makeText(context, "Access Denied", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    setupNavigation(view, R.id.cardBedroom, ::BedRoomFragment)
                }
            },
            onFailure = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        tvTempValue = view.findViewById(R.id.tvTempValue)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        temperatureSensorMonitor.stopMonitoring()
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