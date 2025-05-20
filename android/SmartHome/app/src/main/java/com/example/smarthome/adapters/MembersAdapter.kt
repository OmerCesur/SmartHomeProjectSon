package com.example.smarthome.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.model.User

class MembersAdapter : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {
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

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvMemberRole: TextView = itemView.findViewById(R.id.tvMemberRole)

        fun bind(user: User) {
            tvMemberName.text = user.username
            if (user.role == "host") {
                tvMemberRole.text = "Full Access"
                tvMemberRole.setTextColor(itemView.context.getColor(R.color.colorPrimary))
            } else {
                tvMemberRole.text = "Limited Access"
                tvMemberRole.setTextColor(itemView.context.getColor(R.color.colorSoftBlue))
            }
        }
    }
} 