package com.example.smarthome.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.adapters.MembersAdapter
import com.example.smarthome.viewmodels.MembersViewModel
import com.example.smarthome.auth.AuthManager
import com.example.smarthome.model.User
import com.example.smarthome.activities.LoginActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.Toast

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MembersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MembersFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var viewModel: MembersViewModel
    private lateinit var hostAdapter: MembersAdapter
    private lateinit var guestsAdapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[MembersViewModel::class.java]
        
        // Initialize RecyclerViews
        val rvHost = view.findViewById<RecyclerView>(R.id.rvHost)
        val rvGuests = view.findViewById<RecyclerView>(R.id.rvGuests)

        // Setup logout button
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AuthManager.logout(
                onSuccess = {
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    // Navigate to login screen
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                },
                onFailure = { errorMsg ->
                    Toast.makeText(context, "Logout failed: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Check if current user is host
        var isCurrentUserHost = false
        AuthManager.getCurrentUserRole(
            onSuccess = { role ->
                isCurrentUserHost = role == "host"
                setupAdapters(isCurrentUserHost)
            },
            onFailure = { /* Handle error */ }
        )
    }

    private fun setupAdapters(isHost: Boolean) {
        val rvHost = requireView().findViewById<RecyclerView>(R.id.rvHost)
        val rvGuests = requireView().findViewById<RecyclerView>(R.id.rvGuests)

        hostAdapter = MembersAdapter(isHost) {
            viewModel.loadMembers()
        }
        guestsAdapter = MembersAdapter(isHost) {
            viewModel.loadMembers()
        }

        rvHost.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hostAdapter
        }

        rvGuests.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = guestsAdapter
        }

        // Observe host data
        viewModel.host.observe(viewLifecycleOwner) { host ->
            // Get all hosts from the database
            viewModel.database.child("users").get()
                .addOnSuccessListener { snapshot ->
                    val hosts = mutableListOf<User>()
                    snapshot.children.forEach { child ->
                        child.getValue(User::class.java)?.let { user ->
                            if (user.role == "host") {
                                hosts.add(user)
                            }
                        }
                    }
                    hostAdapter.updateMembers(hosts)
                }
        }

        // Observe guests data
        viewModel.guests.observe(viewLifecycleOwner) { guests ->
            guestsAdapter.updateMembers(guests)
        }

        // Load members data
        viewModel.loadMembers()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MembersFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MembersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}