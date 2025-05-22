package com.example.smarthome.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smarthome.model.User
import com.google.firebase.database.FirebaseDatabase

class MembersViewModel : ViewModel() {
    val database = FirebaseDatabase.getInstance().reference
    private val _host = MutableLiveData<User>()
    private val _guests = MutableLiveData<List<User>>()

    val host: LiveData<User> = _host
    val guests: LiveData<List<User>> = _guests

    fun loadMembers() {
        database.child("users").get()
            .addOnSuccessListener { snapshot ->
                val users = mutableListOf<User>()
                snapshot.children.forEach { child ->
                    child.getValue(User::class.java)?.let { user ->
                        users.add(user)
                    }
                }
                
                // Separate hosts and guests
                val hostUsers = users.filter { it.role == "host" }
                val guestUsers = users.filter { it.role == "guest" }
                
                _host.value = hostUsers.firstOrNull() // Keep first host for backward compatibility
                _guests.value = guestUsers
            }
    }
} 