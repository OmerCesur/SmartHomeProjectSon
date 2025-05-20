package com.example.smarthome.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.models.Notification
import com.example.smarthome.viewmodels.NotificationAdapter
import com.google.firebase.database.*
import android.widget.Toast
import android.util.Log
import android.widget.TextView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NotificationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()

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
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(requireContext(), "NotificationFragment loaded", Toast.LENGTH_SHORT).show()
        recyclerView = view.findViewById(R.id.notificationRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(notifications)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("notifications")
        val placeholderText = view.findViewById<TextView>(R.id.placeholderText)
        placeholderText.visibility = View.GONE // Hide by default
        fetchNotifications()

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("NotificationFragment", "SingleValueEvent: ${snapshot.value}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationFragment", "SingleValueEvent error: ${error.message}")
            }
        })
    }

    private fun fetchNotifications() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                for (notifSnapshot in snapshot.children) {
                    Log.d("NotificationFragment", "Snapshot: ${notifSnapshot.value}")
                    val notif = notifSnapshot.getValue(Notification::class.java)
                    notif?.let {
                        notifications.add(it.copy(id = notifSnapshot.key))
                    }
                }
                notifications.sortByDescending { it.timestamp }
                Log.d("NotificationFragment", "Loaded ${notifications.size} notifications")
                adapter.notifyDataSetChanged()

                // Show/hide placeholder
                view?.findViewById<TextView>(R.id.placeholderText)?.visibility =
                    if (notifications.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationFragment", "Database error: ${error.message}")
            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NotificationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NotificationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}