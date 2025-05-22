package com.example.smarthome.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthome.fragments.HomeFragment
import com.example.smarthome.utils.AppState
import com.example.smarthome.services.GasSensorMonitor
import com.google.firebase.auth.FirebaseAuth
import android.app.Activity
import android.content.Context
import android.view.KeyEvent

class MainActivity : AppCompatActivity() {
    private lateinit var gasSensorMonitor: GasSensorMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // If not logged in, redirect to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        AppState.roomRepository.fetchRooms()

        // Initialize gas sensor monitoring
        gasSensorMonitor = GasSensorMonitor(this)
        gasSensorMonitor.startMonitoring()

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, HomeFragment())
            .commit()
    }

    override fun onBackPressed() {
        // Instead of finishing the activity, move it to the background
        moveTaskToBack(true)
    }
}
