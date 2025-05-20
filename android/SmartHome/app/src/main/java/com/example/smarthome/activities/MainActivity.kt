package com.example.smarthome.activities

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.example.smarthome.fragments.HomeFragment
import com.example.smarthome.utils.AppState
import com.example.smarthome.services.GasSensorMonitor

class MainActivity : AppCompatActivity() {
    private lateinit var gasSensorMonitor: GasSensorMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.roomRepository.fetchRooms()

        // Initialize gas sensor monitoring
        gasSensorMonitor = GasSensorMonitor(this)
        gasSensorMonitor.startMonitoring()

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, HomeFragment())
            .commit()
    }
}
