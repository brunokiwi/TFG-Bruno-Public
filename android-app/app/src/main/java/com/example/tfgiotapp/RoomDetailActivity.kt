package com.example.tfgiotapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomDetailActivity : ComponentActivity() {
    private lateinit var roomNameTextView: TextView
    private lateinit var lightStatusTextView: TextView
    private lateinit var detectStatusTextView: TextView
    private val apiService = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        roomNameTextView = findViewById(R.id.roomNameDetail)
        lightStatusTextView = findViewById(R.id.lightStatus)
        detectStatusTextView = findViewById(R.id.detectStatus)

        val roomName = intent.getStringExtra("roomName") ?: ""
        loadRoomDetail(roomName)
    }

    private fun loadRoomDetail(roomName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val room = apiService.getRoomByName(roomName)
                withContext(Dispatchers.Main) {
                    if (room != null) {
                        displayRoomInfo(room)
                    } else {
                        Toast.makeText(this@RoomDetailActivity, "Habitación no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RoomDetailActivity, "Error al cargar detalle", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayRoomInfo(room: Room) {
        roomNameTextView.text = room.name
        lightStatusTextView.text = "Luz: ${if (room.lightOn) "ON" else "OFF"}"
        detectStatusTextView.text = "Detección: ${if (room.detectOn) "ON" else "OFF"}"
    }
}