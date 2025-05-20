package com.example.smarthome.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.smarthome.model.User

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun registerUser(email: String, password: String, username: String, isHost: Boolean, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        // Create user data in Realtime Database
                        val userData = User(
                            uid = firebaseUser.uid,
                            email = email,
                            username = username,
                            role = if (isHost) "host" else "guest"
                        )
                        
                        database.child("users").child(firebaseUser.uid).setValue(userData)
                            .addOnSuccessListener {
                                Log.d("AUTH", "User data saved: ${firebaseUser.uid}")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e("AUTH", "Failed to save user data: ${e.message}")
                                onFailure(e.message ?: "Failed to save user data")
                            }
                    }
                } else {
                    Log.e("AUTH", "Register failed: ${task.exception?.message}")
                    onFailure(task.exception?.message ?: "Unknown error")
                }
            }
    }

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d("AUTH", "Login successful")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("AUTH", "Login failed: ${it.message}")
                onFailure(it.message ?: "Unknown error")
            }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "Password reset email sent to $email")
                    onSuccess()
                } else {
                    Log.e("AUTH", "Password reset failed: ${task.exception?.message}")
                    onFailure(task.exception?.message ?: "Unknown error")
                }
            }
    }

    fun getCurrentUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid).child("role")
                .get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.value as? String ?: "guest"
                    onSuccess(role)
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Failed to get user role")
                }
        } else {
            onFailure("No user logged in")
        }
    }

    // Function to update existing users' roles
    fun updateExistingUsersRoles(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("No user is currently logged in")
            return
        }

        // Create user data for current user
        val userData = User(
            uid = currentUser.uid,
            email = currentUser.email ?: "",
            username = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "User",
            role = "guest"  // Set default role as guest
        )

        // Save to database
        database.child("users").child(currentUser.uid).setValue(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update user role")
            }
    }

    // Function to set current user as host
    fun setUserAsHost(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("No user is currently logged in")
            return
        }

        database.child("users").child(currentUser.uid).child("role")
            .setValue("host")
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update user role")
            }
    }
}