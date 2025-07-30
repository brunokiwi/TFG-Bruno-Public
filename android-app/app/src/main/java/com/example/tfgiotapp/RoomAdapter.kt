package com.example.tfgiotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.model.Room

class RoomAdapter(
    private var rooms: List<Room>,
    private val onRoomClick: (Room) -> Unit,
    private val onDeleteRoom: (Room) -> Unit,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    private var vacationModeActive: Boolean = false

    fun setVacationModeActive(active: Boolean) {
        vacationModeActive = active
        notifyDataSetChanged()
    }

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomName: TextView = view.findViewById(R.id.roomName)
        val roomStatus: TextView = view.findViewById(R.id.roomStatus)
        val deleteButton: Button = view.findViewById(R.id.deleteRoomButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.roomName.text = room.name
        val lightStatus = if (room.lightOn) "Luz: ON" else "Luz: OFF"
        val sensorStatus = if (room.detectOn) "Sensor: ON" else "Sensor: OFF"
        holder.roomStatus.text = "$lightStatus | $sensorStatus"

        val context = holder.itemView.context
        val cardView = holder.itemView as androidx.cardview.widget.CardView
        if (vacationModeActive) {
            cardView.setCardBackgroundColor(context.getColor(R.color.card_gray_bg))
        } else {
            cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
        }

        holder.deleteButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
        holder.deleteButton.setOnClickListener {
            onDeleteRoom(room)
        }

        holder.itemView.setOnClickListener {
            onRoomClick(room)
        }
    }

    override fun getItemCount() = rooms.size

    fun updateRooms(newRooms: List<Room>) {
        rooms = newRooms
        notifyDataSetChanged()
    }
}