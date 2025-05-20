package com.example.smarthome.firebase

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.example.smarthome.model.SensorData

object FirebaseDatabaseManager {
    private val db = FirebaseDatabase.getInstance().reference

    fun writeSensorData(deviceId: String, type: String, value: Double) {
        val data = SensorData(type, value, System.currentTimeMillis())
        db.child("sensors").child(deviceId).setValue(data)
            .addOnSuccessListener {
                Log.d("DB", "Data saved: $data")
            }
            .addOnFailureListener {
                Log.e("DB", "Save failed: ${it.message}")
            }
    }

    fun readSensorData(deviceId: String) {
        db.child("sensors").child(deviceId).get()
            .addOnSuccessListener {
                val type = it.child("type").value
                val value = it.child("value").value
                Log.d("DB", "Sensor $deviceId: $type - $value")
            }
    }
} 