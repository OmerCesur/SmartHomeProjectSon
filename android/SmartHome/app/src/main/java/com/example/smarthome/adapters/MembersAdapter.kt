package com.example.smarthome.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.model.User
import com.example.smarthome.auth.AuthManager

class MembersAdapter(
    private val isHost: Boolean = false,
    private val onUserDeleted: () -> Unit = {}
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {
    private var members: List<User> = emptyList()

    fun updateMembers(newMembers: List<User>) {
        members = newMembers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount() = members.size

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvMemberRole: TextView = itemView.findViewById(R.id.tvMemberRole)
        private val btnPromote: ImageButton = itemView.findViewById(R.id.btnPromote)
        private val btnDemote: ImageButton = itemView.findViewById(R.id.btnDemote)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(user: User) {
            tvMemberName.text = user.username
            if (user.role == "host") {
                tvMemberRole.text = "Full Access"
                tvMemberRole.setTextColor(itemView.context.getColor(R.color.colorPrimary))
            } else {
                tvMemberRole.text = "Limited Access"
                tvMemberRole.setTextColor(itemView.context.getColor(R.color.colorSoftBlue))
            }

            // Show/hide management buttons based on host status and user role
            if (isHost) {
                btnPromote.visibility = if (user.role == "guest") View.VISIBLE else View.GONE
                btnDemote.visibility = if (user.role == "host") View.VISIBLE else View.GONE
                btnDelete.visibility = View.VISIBLE

                btnPromote.setOnClickListener {
                    AuthManager.promoteToHost(
                        user.uid,
                        onSuccess = {
                            Toast.makeText(itemView.context, "User promoted to host", Toast.LENGTH_SHORT).show()
                            onUserDeleted()
                        },
                        onFailure = { error ->
                            Toast.makeText(itemView.context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                btnDemote.setOnClickListener {
                    AuthManager.demoteToGuest(
                        user.uid,
                        onSuccess = {
                            Toast.makeText(itemView.context, "User demoted to guest", Toast.LENGTH_SHORT).show()
                            onUserDeleted()
                        },
                        onFailure = { error ->
                            Toast.makeText(itemView.context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                btnDelete.setOnClickListener {
                    AuthManager.deleteUser(
                        user.uid,
                        onSuccess = {
                            Toast.makeText(itemView.context, "User deleted", Toast.LENGTH_SHORT).show()
                            onUserDeleted()
                        },
                        onFailure = { error ->
                            Toast.makeText(itemView.context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                btnPromote.visibility = View.GONE
                btnDemote.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }
        }
    }
} 