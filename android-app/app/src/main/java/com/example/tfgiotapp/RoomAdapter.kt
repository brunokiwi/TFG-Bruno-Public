package com.example.tfgiotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.R
import com.example.tfgiotapp.model.Room

class RoomAdapter(
    private var rooms: List<Room>,
    private val onRoomClick: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomName: TextView = view.findViewById(R.id.roomName)
        val roomStatus: TextView = view.findViewById(R.id.roomStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.roomName.text = room.name
        holder.roomStatus.text = if (room.lightOn) "Luz: ON" else "Luz: OFF"

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