package com.example.smarthome.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthome.R
import com.example.smarthome.auth.AuthManager

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val btnReset = findViewById<Button>(R.id.btnReset)
        val etEmail = findViewById<EditText>(R.id.etEmailReset)

        btnReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AuthManager.sendPasswordResetEmail(email,
                onSuccess = {
                    Toast.makeText(this, "Reset link sent to: $email 💌", Toast.LENGTH_LONG).show()
                },
                onFailure = { errorMsg ->
                    Toast.makeText(this, "Failed to send reset link: $errorMsg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
