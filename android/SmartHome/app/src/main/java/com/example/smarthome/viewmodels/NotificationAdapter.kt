package com.example.smarthome.viewmodels

import android.graphics.Color
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.models.Notification

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.notificationTitle)
        val message: TextView = itemView.findViewById(R.id.notificationMessage)
        val timestamp: TextView = itemView.findViewById(R.id.notificationTimestamp)
        val severityBar: View = itemView.findViewById(R.id.severityBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.timestamp.text = formatTimestamp(notification.timestamp)

        val severityColor = when (notification.severity) {
            "high" -> Color.parseColor("#FF5252") // Red
            "low" -> Color.parseColor("#FFD600") // Yellow
            else -> Color.parseColor("#CCCCCC") // Gray
        }
        holder.severityBar.setBackgroundColor(severityColor)

        if (notification.read == false) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFDFD"))
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
    }

    override fun getItemCount() = notifications.size

    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val date = sdf.parse(timestamp)
            if (date != null) {
                DateUtils.getRelativeTimeSpanString(
                    date.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}
