package com.example.smarthome.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthome.R
import com.example.smarthome.auth.AuthManager

class TestRolesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_roles)

        val btnUpdateRoles = findViewById<Button>(R.id.btnUpdateRoles)
        val btnSetHost = findViewById<Button>(R.id.btnSetHost)

        btnUpdateRoles.setOnClickListener {
            AuthManager.updateExistingUsersRoles(
                onSuccess = {
                    Toast.makeText(this, "Current user updated to guest role", Toast.LENGTH_LONG).show()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        }

        btnSetHost.setOnClickListener {
            AuthManager.setUserAsHost(
                onSuccess = {
                    Toast.makeText(this, "Current user set as host", Toast.LENGTH_LONG).show()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
} 